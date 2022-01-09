package games.skweekychair.fantasycostco

import org.bukkit.plugin.java.JavaPlugin

/** Holds useful methods for the fantasy costco stock simulator. */
class CostcoUtils : JavaPlugin() {
    companion object Globals {
        // Mass to add to each transaction
        fun getMassPerTransaction(): Double {
            return 1.0
        }
        // This limits how high the mass of a commodity can go. This means that
        // selling/buying can always modify price a little bit
        fun getMaximumMass(): Double {
            return 1000.0
        }
        // This prevents the shown price from changing *too* quickly
        fun getMaxPctChange(): Double {
            return 0.05
        }
        // This affects how much variation the random noise has based on price
        // 1/50 seems to be a solid value
        fun getVarMultiplier(): Double {
            return 1.0 / 50.0
        }
        // This affects the variation based on mass, so that when `mass` approaches 0,
        // variation is multiplied by `massVarMin`, and when `mass` approaches
        // `maximumMass`, the variation is multiplied by `massVarMax`
        fun getMassVarMin(): Double {
            return 1.0
        }

        fun getMassVarMax(): Double {
            return 1.0 / 10.0
        }
        // 0.05 is a spread of 5% at the most ideal price
        fun getPriceSpread(): Double {
            return 0.05
        }
        // 0.001 results in a price difference between 1 item and a stack of 64 of:
        // 9.37 and 10 for sell prices (10 is the idealized price)
        // 10.63 and 10 for buy prices (10 is the idealized price)
        fun getSurchargeCurveEpsilon(): Double {
            return 0.001
        }

        fun getBuyMult(): Double {
            return (1.0 + getPriceSpread() / 2.0)
        }

        fun getSellMult(): Double {
            return (1.0 - getPriceSpread() / 2.0)
        }
    }
    /**
     * Returns the buy price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     */
    fun buyPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
        val hyperbola: Double = CostcoUtils.getSurchargeCurveEpsilon() * stackSize / amount
        val priceOffset: Double = CostcoUtils.getSurchargeCurveEpsilon() * inputPrice
        return CostcoUtils.getBuyMult() * inputPrice * (1 + hyperbola) - priceOffset
    }
}
