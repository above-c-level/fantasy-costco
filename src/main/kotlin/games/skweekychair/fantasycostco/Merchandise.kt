@file:UseSerializers(EnchantmentSerializer::class, LocationSerializer::class, UuidSerializer::class)

package games.skweekychair.fantasycostco

import java.util.Objects
import java.util.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.Sign
import org.bukkit.enchantments.Enchantment

/** Modifies the price of items pseudorandomly without affecting the best-guess price */
class Perturber : Runnable {
    override fun run() {
        for (item in Cereal.merch.values) {
            item.hold()
            val staleLocations: MutableList<Location> = mutableListOf()
            for (signLocation in item.listOfSigns) {
                val price = item.shownPrice
                val blockState: BlockState = signLocation.getBlock().state
                if (blockState !is Sign) {
                    staleLocations.add(signLocation)
                    continue
                }
                UpdateSignLine(signLocation, 2, price.toString())
            }
            for (staleLocation in staleLocations) {
                RemoveSignFromMerch(staleLocation)
            }
        }
        // Bukkit.broadcastMessage("Perturbed prices of ${Cereal.merch.size} items")
    }
}

/**
 * Represents a single item type in the shop.
 * @param material The base material as defined by Bukkit
 * @param enchantments The enchantments on the item
 */
@Serializable
open class BaseMerchandise(
        open val material: Material,
        open val enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BaseMerchandise) {
            return false
        }
        if (this.material == other.material && this.enchantments.equals(other.enchantments)) {
            return true
        }
        return false
    }
    override fun hashCode() = Objects.hash(material, enchantments)
}

/**
 * Represents a single item in the shop with enchantments as well as various variables that our
 * gradient descent algorithm uses to determine the best price.
 * @param material The material of the item.
 * @param mass The `mass` of our point in the gradient descent algorithm. It's used as a way of
 * determining an items "inertia" or resistance to change.
 * @param hiddenPrice The true, raw best guess at the price of the item.
 * @param shownPrice The price as displayed to players, which is smoothed to prevent too much price
 * movement too quickly, and also to add pseudorandom noise to the price even when no sales occur.
 * @param enchantments The enchantments on the item.
 * @param listOfSigns The signs that reference this merchandise
 * @constructor Creates a new merchandise item.
 */
@Serializable
class Merchandise(
        val material: Material,
        var mass: Double,
        var hiddenPrice: Double,
        var shownPrice: Double,
        val enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>(),
        val listOfSigns: MutableList<Location> = mutableListOf()
) {
    /**
     * The constructor for a new merchandise item.
     * @param material The material of the item.
     * @param mass The `mass` of our point in the gradient descent algorithm.
     * @param startingPrice Our best guess as to the ideal starting price of the item.
     */
    constructor(
            material: Material,
            mass: Double,
            startingPrice: Double
    ) : this(material, mass, startingPrice, startingPrice)

    /**
     * Checks whether this merchandise item is equal to another.
     * @param other The other merchandise item to compare to.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BaseMerchandise) {
            return false
        }
        if (this.material == other.material && this.enchantments.equals(other.enchantments)) {
            return true
        }
        return false
    }
    override fun hashCode() = Objects.hash(material, enchantments)

    /**
     * Increases the 'mass' of the commodity, making it harder to move in the future. Only increases
     * the mass if it's not already at `maximumMass`
     */
    fun addMass() {
        if (this.mass < CostcoGlobals.maximumMass) {
            this.mass += CostcoGlobals.massPerTransaction
            // this.mass = Math.min(CostcoGlobals.maximumMass, this.mass)
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

    /**
     * Gets the buy price of `amount` of this item
     * @param amount The amount of the item to buy.
     * @return The price of the item.
     */
    fun itemBuyPrice(amount: Int): Double {
        return buyPrice(this.shownPrice, amount, this.material.getMaxStackSize())
    }

    /**
     * Gets the sell price of `amount` of this item
     * @param amount The amount of the item to sell.
     * @return The price of the item.
     */
    fun itemSellPrice(amount: Int): Double {
        return sellPrice(this.shownPrice, amount, this.material.getMaxStackSize())
    }

    /** Pseudo-randomly perturbs the price of the item without affecting the best-guess price. */
    fun perturbPrice() {
        // hopefully this is either unnecessary or doesn't happen often
        // but just in case
        this.hiddenPrice = Math.abs(this.hiddenPrice)
        val massVar: Double =
                lerp(
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
                lerpClamp(
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

    /**
     * Calculates the magnitude of the change of price given a transaction
     * @param numItems The number of items in the transaction
     * @return The magnitude of the change of price.
     */
    fun pushAmount(numItems: Double): Double {
        val dist: Double = Math.abs(this.shownPrice - this.hiddenPrice) + 1.0
        val sqrtPrice: Double = Math.sqrt(this.shownPrice)
        val multItems: Double = Math.sqrt(numItems) * CostcoGlobals.purchaseSizeMultiplier
        return Math.sqrt(multItems / this.mass * dist * sqrtPrice)
    }

    /** First smooths changes to price, then perturbs the price. */
    fun hold() {
        smoothPrice()
        perturbPrice()
    }

    /**
     * Updates our best guess at the price of the item by buying, smooths the price, adds mass, and
     * perturbs the price.
     */
    fun buy(numItems: Double) {
        this.hiddenPrice += pushAmount(numItems)
        smoothPrice()
        addMass()
        perturbPrice()
        UpdateSignPrices(BaseMerchandise(this.material, this.enchantments))
    }

    /**
     * Updates our best guess at the price of the item by selling, smooths the price, adds mass, and
     * perturbs the price.
     */
    fun sell(numItems: Double) {
        this.hiddenPrice -= pushAmount(numItems)
        smoothPrice()
        addMass()
        perturbPrice()
        UpdateSignPrices(BaseMerchandise(this.material, this.enchantments))
    }
}
