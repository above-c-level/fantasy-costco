package games.skweekychair.fantasycostco

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

/** Handles configuration for the plugin. */
object CostcoGlobals {
    var spigotConfig: FileConfiguration = YamlConfiguration()

    // Starting amount in wallet
    val defaultWallet
        get() = spigotConfig.getDouble("default-wallet", 100.0)

    // Mass to add to each transaction
    val massPerTransaction
        get() = spigotConfig.getDouble("mass-per-transaction", 1.0)

    // A multiplier for how much larger transactions affect the economy
    val purchaseSizeMultiplier
        get() = spigotConfig.getDouble("purchase-size-multiplier", 1.0)

    // This limits how high the mass of a commodity can go. This means that
    // selling/buying can always modify price a little bit
    val maximumMass
        get() = spigotConfig.getDouble("maximum-mass", 1000.0)

    // This prevents the shown price from changing *too* quickly
    val maxPctChange
        get() = spigotConfig.getDouble("max-pct-change", 0.05)

    // This affects how much variation the random noise has based on price
    // 1/50 seems to be a solid value
    val varMultiplier
        get() = spigotConfig.getDouble("var-multiplier", 1.0/50.0)

    // This affects the variation based on mass, so that when `mass` approaches 0,
    // variation is multiplied by `massVarMin`, and when `mass` approaches
    // `maximumMass`, the variation is multiplied by `massVarMax`
    val massVarMin
        get() = spigotConfig.getDouble("mass-var-min", 1.0)
    val massVarMax
        get() = spigotConfig.getDouble("mass-var-max", 0.1)

    // 0.05 is a spread of 5% at the most ideal price
    val priceSpread
        get() = spigotConfig.getDouble("price-spread", 0.05)

    // 0.001 results in a price difference between 1 item and a stack of 64 of:
    // 9.37 and 10 for sell prices (10 is the idealized price in this example)
    // 10.63 and 10 for buy prices (10 is the idealized price in this example)
    val surchargeCurveEpsilon
        get() = spigotConfig.getDouble("surcharge-curve-epsilon", 0.001)

    // How hard to clamp down on the distance a commodity can travel from its "ideal" price
    // This seems to work best with values between 0.25 and 0.5 based on preliminary testing.
    // Hypothetically, a market running randomly would run with a value of 0, but we don't want
    // values to wander *too* much
    val clampMultiplier
        get() = spigotConfig.getDouble("clamp-multiplier", 0.5)

    val startingMass
        get() = spigotConfig.getDouble("starting-mass", 5.0)

    val secondsBetweenPriceMotion
        // It's important to multiply by 20 because minecraft has 20 ticks per second
        get() = spigotConfig.getLong("seconds-between-price-motion", 60L) * 20

    // Not stored in config because they're just helper values
    val buyMult
        get() = 1.0 + priceSpread / 2.0
    val sellMult
        get() = 1.0 - priceSpread / 2.0

    val debugMessages
        get() = spigotConfig.getBoolean("debug-messages", false)
}
