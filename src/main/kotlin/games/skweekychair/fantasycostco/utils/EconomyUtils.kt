package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/** Holds methods used to get things directly related to the economy, including helper methods. */
object EconomyUtils {
    /**
     * Returns the buy price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     *
     * @param inputPrice The ideal price.
     * @param amount The number of items being purchased.
     * @param maxStack The maximum amount that can be held in one stack.
     * @return The buy price.
     */
    fun buyPrice(inputPrice: Double, amount: Int, maxStack: Int = 64): Double {
        val hyperbola: Double = CostcoGlobals.surchargeCurveEpsilon * maxStack / amount
        val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
        return (CostcoGlobals.buyMult * inputPrice * (1 + hyperbola) - priceOffset) * amount
    }

    /**
     * Returns the sell price given given an ideal price, the number of items being purchased, and
     * the maximum amount that can be held in one stack.
     * @param inputPrice The ideal price.
     * @param amount The number of items being sold.
     * @param maxStack The maximum amount that can be held in one stack.
     * @return The sell price.
     */
    fun sellPrice(inputPrice: Double, amount: Int, maxStack: Int = 64): Double {
        val hyperbola: Double = -CostcoGlobals.surchargeCurveEpsilon * maxStack / amount
        val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
        // TODO: Check this--the python file uses buy_mult
        return (CostcoGlobals.sellMult * inputPrice * (1 + hyperbola) + priceOffset) * amount
    }

    /**
     * Linearly interpolates `x` between the points (x0, y0) and (x1, y1)
     * @param x The x value to interpolate.
     * @param x0 The x value of the first point.
     * @param y0 The y value of the first point.
     * @param x1 The x value of the second point.
     * @param y1 The y value of the second point.
     * @return The y value of the interpolated point.
     */
    fun lerp(x: Double, x0: Double, y0: Double, x1: Double, y1: Double): Double {
        // Literally just the formula for linear interpolation
        // if you have a function `func` that goes between 0 and 1, you can also
        // interpolate with that function by replacing it with
        // (y1 - y0) * func((x - x0) / (x1 - x0)) + y0
        // For more, see here: https://www.desmos.com/calculator/6wh1xdmhc5
        return (y1 - y0) * ((x - x0) / (x1 - x0)) + y0
    }

    /**
     * Smoothly interpolates `x` between the points (0, 0) and (1, 1)
     * https://en.wikipedia.org/wiki/Smoothstep
     * @param x The x value to interpolate.
     * @return The y value of the interpolated point.
     */
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
     * @param x The x value to interpolate.
     * @param lowerBound The lower x value of the interpolation.
     * @param upperBound The upper x value of the interpolation.
     * @param multiplier The multiplier to use for the interpolation.
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