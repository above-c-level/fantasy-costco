package games.skweekychair.fantasycostco

import java.text.DecimalFormat
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import org.bukkit.Location
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
    // The wallet is guaranteed not to be null so we can safely access it
    Cereal.wallets[player.uniqueId]!!.balance = getWallet(player) + amount
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
 * Gets the player's wallet or adds it if it does not exist
 * @param player The player to get the wallet of
 * @return The player's wallet
 */
fun getWallet(player: Player): Double {
    ensureWallet(player)
    return Cereal.wallets[player.uniqueId]!!.balance
}

fun getWalletRounded(player: Player): Double {
    return roundDouble(getWallet(player))
}
/**
 * Gets the player's wallet or adds it if it does not exist
 * @param player The player to get the wallet of
 * @return The player's wallet
 */
fun getWalletString(player: Player): String {
    return roundDoubleString(getWallet(player))
}

/**
 * Sets the player's wallet to the given amount
 * @param player The player to set the wallet of
 * @param amount The amount to set the wallet to
 */
fun setWallet(player: Player, amount: Double): Double {
    ensureWallet(player)
    Cereal.wallets[player.uniqueId]!!.balance = amount
    return amount
}

/**
 * Ensures the player's wallet exists
 * @param player The player to ensure the wallet of
 */
fun ensureWallet(player: Player) {
    if (!Cereal.wallets.containsKey(player.uniqueId)) {
        Cereal.wallets[player.uniqueId] = MembershipCard(CostcoGlobals.defaultWallet)
    }
}

/**
 * Gets the player's buy goal or adds it if it does not exist
 * @param player The player to get the buy goal of
 * @return The player's buy goal
 */
fun getBuyGoal(player: Player): Int {
    ensureWallet(player)
    return Cereal.wallets[player.uniqueId]!!.buyGoal
}

/**
 * Sets the player's buy goal to the given amount
 * @param player The player to set the buy goal of
 * @param amount The amount to set the buy goal to
 */
fun setBuyGoal(player: Player, amount: Int) {
    ensureWallet(player)
    Cereal.wallets[player.uniqueId]!!.buyGoal = amount
}

/**
 * Gets whether the play wants to buy as much as they can afford
 * @param player The player to get the buy goal of
 * @return Whether the player wants to buy as much as they can afford
 */
fun getBuyMaxItems(player: Player): Boolean {
    ensureWallet(player)
    return Cereal.wallets[player.uniqueId]!!.buyMaxItems
}

/**
 * Sets whether the player wants to buy as much as they can afford
 * @param player The player to set the buy goal of
 * @param buyMax Whether the player wants to buy as much as they can afford
 */
fun setBuyMaxItems(player: Player, buyMax: Boolean) {
    ensureWallet(player)
    Cereal.wallets[player.uniqueId]!!.buyMaxItems = buyMax
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
    return (CostcoGlobals.buyMult * inputPrice * (1 + hyperbola) - priceOffset) * amount
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

/**
 * Gets merchandise from the set of Merchandise. If it does not exist, it is added automatically.
 * @param name The name of the merchandise.
 * @return The merchandise.
 */
fun getMerchandise(baseMerch: BaseMerchandise): Merchandise {

    if (baseMerch !in Cereal.merch) {
        logIfDebug("${baseMerch.material.name} not in merch")
        val material = baseMerch.material
        if (CostcoGlobals.hasFixedPrice(material)) {
            logIfDebug("    ${material.name} has fixed price")
            val bestGuessPrice = CostcoGlobals.fixedPrice(baseMerch.material)
            Cereal.merch[baseMerch] =
                    Merchandise(
                            baseMerch.material,
                            CostcoGlobals.startingMass,
                            bestGuessPrice,
                            hasFixedPrice = true
                    )
        } else {
            logIfDebug("    ${material.name} has varying price")
            val bestGuessPrice = CostcoGlobals.startingMerchPrice(baseMerch.material)
            Cereal.merch[baseMerch] =
                    Merchandise(baseMerch.material, CostcoGlobals.startingMass, bestGuessPrice)
        }
    } else {
        logIfDebug("${baseMerch.material.name} already in merch")
    }

    for (i in baseMerch.enchantments) {
        logIfDebug("This ${baseMerch.material.name} has enchantments ${i}")
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

/**
 * Broadcasts a message to the minecraft server if debug mode is enabled.
 * @param message The message to broadcast.
 */
fun logIfDebug(message: String) {
    if (CostcoGlobals.debugMessages) {
        LogInfo(message)
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

/**
 * Removes a merchandise-sign association
 * @param location The location of the sign.
 * @return True if there was an association (which was then removed), false otherwise.
 */
fun RemoveSignFromMerch(location: Location): Boolean {
    if (location in Cereal.purchasePoints) {
        // logIfDebug("Removing ${location.blockX} ${location.blockY} ${location.blockZ}")
        // Grab the base merch associated with the location.
        val baseMerchAtLocation = Cereal.purchasePoints[location]
        // Get the old merch associated with the location.
        val oldMerch = getMerchandise(baseMerchAtLocation!!)
        // Remove the location from the list of purchase points
        Cereal.purchasePoints.remove(location)
        // Lastly, remove the location from the old merch
        oldMerch.listOfSigns.remove(location)
        Cereal.signs.remove(location)
        return true
    }
    return false
}

/**
 * Adds a sign to the merchandise at the given location. If a sign already exists, it is
 * overwritten. Also removes the sign from the merchandise it was previously associated with.
 * @param baseMerch The BaseMerchandise to associate the sign with.
 * @param location The location of the sign.
 * @param signType The type of the sign.
 */
fun AddSignToMerch(baseMerch: BaseMerchandise, location: Location, signType: SignType) {
    // TODO: Check for null signs?
    // First, check if the location already is catalogued, and if so, remove it.
    RemoveSignFromMerch(location)
    // Add the location to the purchase points and the merch.
    Cereal.purchasePoints[location] = baseMerch
    val merch = getMerchandise(baseMerch)
    merch.listOfSigns.add(location)
    Cereal.signs[location] = SignData(signType)
    merch.updateSign(location)
}

/** Logs an info message to the console. */
fun LogInfo(message: String) {
    return Bukkit.getServer().getLogger().info("[FantasyCostco] $message")
}

/** Logs a warning message to the console. */
fun LogWarning(message: String) {
    return Bukkit.getServer().getLogger().warning("[FantasyCostco] $message")
}

/**
 * Gets the data associated with a player. Makes sure the player has an entry in the map before
 * returning it.
 * @param player The player to get the data of.
 * @return The data associated with the player.
 */
fun getMembershipCard(player: Player): MembershipCard {
    ensureWallet(player)
    return Cereal.wallets[player.uniqueId]!!
}

/**
 * Round a double to a specified number of decimal places with trailing zeros.
 * @param value The value to round.
 * @param places The number of decimal places to round to.
 * @return The rounded value.
 */
fun roundDoubleString(value: Double, significantDigits: Int = 2): String {
    if (significantDigits < 0) throw IllegalArgumentException()
    var format: StringBuilder = StringBuilder("#.")
    for (i in 0 until significantDigits) {
        format.append("0")
    }
    return "₿${DecimalFormat(format.toString()).format(value)}"
}

/**
 * Round a double to a specified number of significant digits.
 * @param value The value to round.
 * @param significantDigits The number of significant digits to round to.
 * @return The rounded value.
 */
fun roundDouble(value: Double, significantDigits: Int = 2): Double {
    if (value.isNaN()) {
        return Double.NaN
    }

    val scale = Math.pow(10.0, significantDigits.toDouble())
    return Math.round(value * scale) / scale
}

/**
 * Worst case scenario is the player wants a full inventory, so even if we increase the amount a
 * player can buy up to 36 stacks, this is guaranteed to take <= 11 iterations
 */
fun binarySearchPrice(
        amountStart: Int,
        merchandise: Merchandise,
        playerFunds: Double
): Pair<Int, Double> {
    var amount: Int = amountStart
    var low: Int = 1
    var high: Int = amount
    while (low < high) {
        val mid = (low + high) / 2
        val midBuyPrice = merchandise.itemBuyPrice(mid)
        if (midBuyPrice > playerFunds) {
            high = mid - 1
        } else {
            low = mid + 1
        }
    }
    amount = high - 1
    return Pair(amount, merchandise.itemBuyPrice(amount))
}
