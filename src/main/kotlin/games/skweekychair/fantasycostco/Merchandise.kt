package games.skweekychair.fantasycostco

import java.util.Random
import org.bukkit.Material

data class Merchandise(
        var material: Material,
        var mass: Double,
        var hiddenPrice: Double,
        var shownPrice: Double
) {
    // TODO: Make sure these to work properly
    /** Calculates the magnitude of the change of price given a transaction */
    fun pushAmount(): Double {
        val dist: Double = Math.abs(this.shownPrice - this.hiddenPrice) + 1.0
        val sqrtPrice: Double = Math.sqrt(this.shownPrice)
        return Math.sqrt(1.0 / this.mass * dist * sqrtPrice)
    }

    /**
     * Increases the 'mass' of the commodity, making it harder to move in the future. Only increases
     * the mass if it's not already at `maximumMass`
     */
    fun addMass() {
        if (this.mass < CostcoGlobals.massPerTransaction) {
            this.mass += CostcoGlobals.massPerTransaction
            this.mass = Math.min(CostcoGlobals.maximumMass, this.mass)
        }
    }

    /**
     * Prevents the shown price from moving more than `maxPctChange` than its previous value. For
     * example, if the hidden price moves from 1 to 2 but `maxPctChange` is 0.1, the new shown price
     * is 1.10.
     */
    fun smoothPrice() {
        val upperLimit: Double = (1.0 + CostcoGlobals.maxPctChange) * this.shownPrice
        val lowerLimit: Double = (1.0 - CostcoGlobals.maxPctChange) * this.shownPrice
        if (this.hiddenPrice < upperLimit && this.hiddenPrice > lowerLimit) {
            this.shownPrice = this.hiddenPrice
        } else if (this.hiddenPrice > upperLimit) {
            this.shownPrice = upperLimit
        } else {
            this.shownPrice = lowerLimit
        }
    }

    /** Gets the buy price of `amount` of this item */
    fun itemBuyPrice(amount: Int): Double {
        return CostcoUtils.buyPrice(this.shownPrice, amount, this.material.getMaxStackSize())
    }

    /** Gets the sell price of `amount` of this item */
    fun itemSellPrice(amount: Int): Double {
        return CostcoUtils.sellPrice(this.shownPrice, amount, this.material.getMaxStackSize())
    }

    fun perturbPrice() {
        // hopefully this is either unnecessary or doesn't happen often
        // but just in case
        this.hiddenPrice = Math.abs(this.hiddenPrice)
        val massVar: Double =
                CostcoUtils.lerp(
                        this.mass,
                        0.0,
                        CostcoGlobals.massVarMin,
                        CostcoGlobals.maximumMass,
                        CostcoGlobals.massVarMax
                )
        val variation: Double = this.hiddenPrice * CostcoGlobals.varMultiplier * massVar
        val gaussian: Double = Random().nextGaussian() * variation
        this.hiddenPrice += gaussian
        val dist = Math.abs(this.hiddenPrice - this.shownPrice)
        val correctionGain =
                CostcoUtils.lerpClamp(
                        dist,
                        this.hiddenPrice,
                        this.hiddenPrice / 2.0,
                        CostcoGlobals.clampMultiplier
                )
        this.shownPrice =
                Math.abs(
                        (1.0 - correctionGain) * this.shownPrice + correctionGain * this.hiddenPrice
                )
    }

    fun hold() {
        smoothPrice()
        perturbPrice()
    }

    fun buy() {
        this.hiddenPrice += pushAmount()
        smoothPrice()
        addMass()
        perturbPrice()
    }

    fun sell() {
        this.hiddenPrice -= pushAmount()
        smoothPrice()
        addMass()
        perturbPrice()
    }
}
