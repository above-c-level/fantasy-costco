package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/**
 * Adds the given amount to the player's wallet.
 * @param player The player we're adding to
 * @param amount The amount to add
 */
fun walletAdd(player: Player, amount: Double) {
    Cereal.wallets[player.uniqueId] =
            Cereal.wallets.getOrDefault(player.uniqueId, CostcoGlobals.defaultWallet) + amount
}

/**
 * Subtracts the given amount from the player's wallet.
 *
 * @param player The player to subtract from.
 * @param amount The amount to subtract.
 */
fun walletSubtract(player: Player, amount: Double) {
    walletAdd(player, -amount)
}

/**
 * Returns the buy price given given an ideal price, the number of items being purchased, and the
 * maximum amount that can be held in one stack.
 *
 * @param inputPrice The ideal price.
 * @param amount The number of items being purchased.
 * @param maxStack The maximum amount that can be held in one stack.
 * @return The buy price.
 */
fun buyPrice(inputPrice: Double, amount: Int, maxStack: Int = 64): Double {
    val hyperbola: Double = CostcoGlobals.surchargeCurveEpsilon * maxStack / amount
    val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
    return CostcoGlobals.buyMult * inputPrice * (1 + hyperbola) - priceOffset
}

/**
 * Returns the sell price given given an ideal price, the number of items being purchased, and the
 * maximum amount that can be held in one stack.
 * @param inputPrice The ideal price.
 * @param amount The number of items being sold.
 * @param maxStack The maximum amount that can be held in one stack.
 * @return The sell price.
 */
fun sellPrice(inputPrice: Double, amount: Int, maxStack: Int = 64): Double {
    val hyperbola: Double = -CostcoGlobals.surchargeCurveEpsilon * maxStack / amount
    val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
    // TODO: Check this--the python file uses buy_mult
    return CostcoGlobals.sellMult * inputPrice * (1 + hyperbola) + priceOffset
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

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
 * @param name The name of the merchandise.
 * @return The merchandise.
 */
fun getMerchandise(baseMerch: BaseMerchandise): Merchandise {

    if (baseMerch !in Cereal.merch) {
        broadcastIfDebug("${baseMerch.material.name} not in merch")
        broadcastIfDebug("baseMerch has hash code${baseMerch.hashCode()}")
        Cereal.merch[baseMerch] = Merchandise(baseMerch.material, CostcoGlobals.startingMass, 10.0)
    } else {
        broadcastIfDebug("${baseMerch.material.name} already in merch")
    }

    for (i in baseMerch.enchantments) {
        broadcastIfDebug("${baseMerch.material.name} has enchantments ${i}")
    }
    return Cereal.merch[baseMerch]!!
}

/**
 * Broadcasts a message to all players on both the minecraft server and to the discord server.
 * @param message The message to broadcast.
 */
fun tryDiscordBroadcast(message: String) {
    tryOnlyDiscord(message)
    tryOnlyBroadcast(message)
}

/**
 * Broadcasts a message to the discord server.
 * @param message The message to broadcast.
 */
fun tryOnlyDiscord(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv:discord bcast $message")
}

/**
 * Broadcasts a message to the minecraft server.
 * @param message The message to broadcast.
 */
fun tryOnlyBroadcast(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bcast $message")
}

/** Broadcasts a message to the minecraft server if debug mode is enabled. */
fun broadcastIfDebug(message: String) {
    if (CostcoGlobals.debugMessages) {
        Bukkit.getServer().broadcastMessage(message)
    }
}

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
 * @param material The item type as defined by bukkit's Material
 * @param enchantments The enchantments and levels of enchantments on the merchandise
 * @return The merchandise.
 */
fun getMerchandise(
        material: Material,
        enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>()
): Merchandise {

    val baseMerch = BaseMerchandise(material, enchantments)
    return getMerchandise(baseMerch)
}

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
 * @param item The item type as defined by bukkit's ItemStack
 * @return The merchandise.
 */
fun getMerchandise(item: ItemStack): Merchandise {
    val baseMerch = BaseMerchandise(item.type, getItemEnchants(item))
    return getMerchandise(baseMerch)
}

/**
 * Enchanted books store their information differently. This method gets the map of enchantments an
 * item has, regardless of whether it's an enchanted book. If there are no enchantments, returns an
 * empty Map<Enchantment, Int>
 * @param item The item to get the enchantments of.
 * @return The map of enchantments and levels.
 */
fun getItemEnchants(item: ItemStack): Map<Enchantment, Int> {
    var enchantments: Map<Enchantment, Int>? = null
    if (item.type == Material.ENCHANTED_BOOK) {
        val meta = item.itemMeta
        if (meta is EnchantmentStorageMeta) {
            enchantments = meta.getStoredEnchants()
        }
    } else {
        enchantments = item.enchantments
    }
    return enchantments ?: HashMap<Enchantment, Int>()
}
