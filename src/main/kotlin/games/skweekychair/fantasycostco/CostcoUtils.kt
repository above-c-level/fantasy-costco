package games.skweekychair.fantasycostco

import org.bukkit.plugin.java.JavaPlugin

/** Holds useful methods for the fantasy costco stock simulator. */
class CostcoUtils : JavaPlugin() {
    /**
     * Returns the buy price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     */
    fun buyPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
        val hyperbola: Double = CostcoGlobals.getSurchargeCurveEpsilon() * stackSize / amount
        val priceOffset: Double = CostcoGlobals.getSurchargeCurveEpsilon() * inputPrice
        return CostcoGlobals.getBuyMult() * inputPrice * (1 + hyperbola) - priceOffset
    }

    /**
     * Returns the sell price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     */
    fun sellPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
        val hyperbola: Double = -CostcoGlobals.getSurchargeCurveEpsilon() * stackSize / amount
        val priceOffset: Double = CostcoGlobals.getSurchargeCurveEpsilon() * inputPrice
        // TODO: Check this--the python file uses buy_mult
        return CostcoGlobals.getSellMult() * inputPrice * (1 + hyperbola) + priceOffset
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
