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

    /**
     * Returns the sell price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     */
    fun sellPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
        val hyperbola: Double = -CostcoUtils.getSurchargeCurveEpsilon() * stackSize / amount
        val priceOffset: Double = CostcoUtils.getSurchargeCurveEpsilon() * inputPrice
        // TODO: Check this--the python file uses buy_mult
        return CostcoUtils.getSellMult() * inputPrice * (1 + hyperbola) + priceOffset
    }

    /** Linearly interpolates `x` between the points (x0, y0) and (x1, y1) */
    fun lerp(x: Double, x0: Double, y0: Double, x1: Double, y1: Double): Double {
        // Literally just the formula for linear interpolation
        // if you have a function `func` that goes between 0 and 1, you can also
        // interpolate with that function by replacing it with
        // (y1 - y0) * func((x - x0) / (x1 - x0)) + y0
        // For more, see here: https://www.desmos.com/calculator/6wh1xdmhc5
        return (y1 - y0) * ((x - x0) / (x1 - x0)) + y0
    }

    /** https://en.wikipedia.org/wiki/Smoothstep */
    fun smoothstep(x: Double): Double {
        if (x <= 0) {
            return 0.0
        } else if (x >= 1) {
            return 1.0
        } else {
            return 3.0 * Math.pow(x, 2.0) - 2.0 * Math.pow(x, 3.0)
        }
    }

    /**
     * Combination of lerp and clamp. Linearly interpolates between 0 and some `multiplier` between
     * `lowerBound` and `upperBound`, but it also clamps the values between 0 and `multiplier`.
     * https://www.desmos.com/calculator/dzqgq1bkyw
     */
    fun lerpClamp(
            x: Double,
            upperBound: Double,
            lowerBound: Double,
            multiplier: Double = 0.25
    ): Double {
        if (x <= lowerBound) {
            return 0.0
        } else if (x >= upperBound) {
            return multiplier
        } else {
            val diff: Double = upperBound - lowerBound
            return multiplier * x / diff - (lowerBound * multiplier) / diff
        }
    }
}
