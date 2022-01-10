package games.skweekychair.fantasycostco

import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

data class Merchandise(
        var material: Material,
        var mass: Double,
        var hiddenPrice: Double,
        var shownPrice: Double
)

class CostcoPlugin : JavaPlugin() {

    // TODO: Modify these to work properly, or move to CostcoUtils and convert them to be static
    // /**
    //  * Calculates the magnitude of the change of price given a transaction
    //  */
    // fun pushAmount(): Double {
    //     val dist: Double = abs(shownPrice - hiddenPrice) + 1
    //     val sqrtPrice: Double = sqrt(shownPrice)
    //     return sqrt(1 / mass * dist * sqrtPrice)
    // }

    // /**
    //  * Increases the 'mass' of the commodity, making it harder to move in the future
    //  */
    // fun addMass() {
    //     if (mass < massPerTransaction) {
    //         mass += massPerTransaction
    //         mass = min(maximumMass, mass)
    //     }
    // }

    // /**
    //  * Prevents the shown price from moving more than `maxPctChange` than its previous value.
    //  * For example, if the hidden price moves from 1 to 2 but `maxPctChange` is 0.1,
    //  * the new shown price is 1.10.
    //  */
    // fun smoothPrice() {
    //     val upperLimit: Double = (1 + maxPctChange) * shownPrice
    //     val lowerLimit: Double = (1 - maxPctChange) * shownPrice
    //     if (hiddenPrice < upperLimit) && (hiddenPrice > lowerLimit) {
    //         shownPrice = hiddenPrice
    //     } else if (hiddenPrice > upperLimit) {
    //         shownPrice = upperLimit
    //     } else {
    //         shownPrice = lowerLimit
    //     }
    // }

}
