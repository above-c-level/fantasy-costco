package games.skweekychair.fantasycostco

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.configuration.ConfigurationSection

object CostcoGlobals {
    var spigotConfig: FileConfiguration = YamlConfiguration()
    
    // Mass to add to each transaction
    val massPerTransaction get() = spigotConfig.getDouble("mass-per-transaction", 1.0)

    // This limits how high the mass of a commodity can go. This means that
    // selling/buying can always modify price a little bit
    val maximumMass = 1000.0

    // This prevents the shown price from changing *too* quickly
    val maxPctChange = 0.05

    // This affects how much variation the random noise has based on price
    // 1/50 seems to be a solid value
    val varMultiplier = 1.0 / 50.0

    // This affects the variation based on mass, so that when `mass` approaches 0,
    // variation is multiplied by `massVarMin`, and when `mass` approaches
    // `maximumMass`, the variation is multiplied by `massVarMax`
    val massVarMin = 1.0
    val massVarMax = 1.0 / 10.0

    // 0.05 is a spread of 5% at the most ideal price
    val priceSpread = 0.05

    // 0.001 results in a price difference between 1 item and a stack of 64 of:
    // 9.37 and 10 for sell prices (10 is the idealized price in this example)
    // 10.63 and 10 for buy prices (10 is the idealized price in this example)
    val surchargeCurveEpsilon = 0.001

    val buyMult = 1.0 + priceSpread / 2.0

    val sellMult = 1.0 - priceSpread / 2.0

    // How hard to clamp down on the distance a commodity can travel from its "ideal" price
    // This seems to work best with values between 0.25 and 0.5 based on preliminary testing.
    // Hypothetically, a market running randomly would run with a value of 0, but we don't want
    // values to wander *too* much
    val clampMultiplier = 0.5

    val startingMass = 5
}
