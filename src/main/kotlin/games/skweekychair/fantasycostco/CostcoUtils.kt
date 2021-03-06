package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

/** Holds useful methods for the fantasy costco stock simulator. */
fun walletAdd(player: Player, amount: Double) {
    Cereal.wallets[player.uniqueId] =
            Cereal.wallets.getOrDefault(player.uniqueId, CostcoGlobals.defaultWallet) + amount
}

fun walletSubtract(player: Player, amount: Double) {
    walletAdd(player, -amount)
}

/**
 * Returns the buy price given given an ideal price, the number of items being purchased, and the
 * maximum amount that can be held in one stack.
 */
fun buyPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
    val hyperbola: Double = CostcoGlobals.surchargeCurveEpsilon * stackSize / amount
    val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
    return CostcoGlobals.buyMult * inputPrice * (1 + hyperbola) - priceOffset
}

/**
 * Returns the sell price given given an ideal price, the number of items being purchased, and the
 * maximum amount that can be held in one stack.
 */
fun sellPrice(inputPrice: Double, amount: Int, stackSize: Int = 64): Double {
    val hyperbola: Double = -CostcoGlobals.surchargeCurveEpsilon * stackSize / amount
    val priceOffset: Double = CostcoGlobals.surchargeCurveEpsilon * inputPrice
    // TODO: Check this--the python file uses buy_mult
    return CostcoGlobals.sellMult * inputPrice * (1 + hyperbola) + priceOffset
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

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
 */
fun getMerchandise(baseMerch: BaseMerchandise): Merchandise {

    if (baseMerch !in Cereal.merch) {
        // Bukkit.getServer().broadcastMessage("${baseMerch.material.name} not in merch")
        // Bukkit.getServer().broadcastMessage("baseMerch has hash code${baseMerch.hashCode()}")
        Cereal.merch[baseMerch] = Merchandise(baseMerch.material, CostcoGlobals.startingMass, 10.0)
    } else {
        // Bukkit.getServer().broadcastMessage("${baseMerch.material.name} already in merch")
    }
    for (i in baseMerch.enchantments) {
        // Bukkit.getServer().broadcastMessage("${baseMerch.material.name} has enchantments ${i}")
    }
    return Cereal.merch.getOrDefault(
            baseMerch,
            Merchandise(baseMerch.material, CostcoGlobals.startingMass, 10.0)
    )
}

fun tryDiscordBroadcast(message: String) {
    tryOnlyDiscord(message)
    tryOnlyBroadcast(message)
}

fun tryOnlyDiscord(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv:discord bcast $message")
}

fun tryOnlyBroadcast(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "bcast $message")
}

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
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
 */
fun getMerchandise(item: ItemStack): Merchandise {
    val baseMerch = BaseMerchandise(item.type, getItemEnchants(item))
    return getMerchandise(baseMerch)
}

/**
 * Enchanted books store their information differently. This method gets the map of enchantments an
 * item has, regardless of whether it's an enchanted book. If there are no enchantments, returns an
 * empty Map<Enchantment, Int>
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
