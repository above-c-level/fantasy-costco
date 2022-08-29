package games.skweekychair.fantasycostco

import java.util.Locale
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

/**
 * Gets the player's wallet as a double or adds it if it does not exist
 * @param player The player to get the wallet of
 * @return The player's wallet
 */
fun getWalletRounded(player: Player): Double {
    return roundDouble(getWallet(player))
}
/**
 * Gets the player's wallet as a string or adds it if it does not exist
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
 * Broadcasts a message to all players on both the minecraft server and to the discord server.
 * @param message The message to broadcast.
 */
fun broadcastAll(message: String) {
    broadcastDiscord(message)
    broadcastServer(message)
}

/**
 * Broadcasts a message to the discord server.
 * @param message The message to broadcast.
 */
fun broadcastDiscord(message: String) {
    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discordsrv:discord bcast $message")
}

/**
 * Broadcasts a message to the minecraft server.
 * @param message The message to broadcast.
 */
fun broadcastServer(message: String) {
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



/** Logs an info message to the console. */
fun LogInfo(message: String) {
    return Bukkit.getServer().getLogger().info("[FantasyCostco] $message")
}

/** Logs a warning message to the console. */
fun LogWarning(message: String) {
    return Bukkit.getServer().getLogger().warning("[FantasyCostco] $message")
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

class StartupSplashArt : Runnable {
    override fun run() {
        // woooo ansi codes

        val RED: String = "\u001B[31m"
        val BLUE: String = "\u001B[34m"
        val RESET: String = "\u001B[0m"
        // val BLACK: String = "\u001B[30m"
        // val GREEN: String = "\u001B[32m"
        // val YELLOW: String = "\u001B[33m"
        // val PURPLE: String = "\u001B[35m"
        val CYAN: String = "\u001B[36m"
        // val WHITE: String = "\u001B[37m"
        // val BGBLACK: String = "\u001b[40m"
        // val BGRED: String = "\u001b[41m"
        // val BGGREEN: String = "\u001b[42m"
        // val BGYELLOW: String = "\u001b[43m"
        // val BGBLUE: String = "\u001b[44m"
        // val BGMAGENTA: String = "\u001b[45m"
        // val BGCYAN: String = "\u001b[46m"
        // val BGWHITE: String = "\u001b[47m"

        fun log(x: String) = Bukkit.getServer().getLogger().info(x)

        log(
                "\n                      ${BLUE} █████░ ▓█▒  ██░ ▓█ ██████ ▓█▓   █▓█  ██  ██▒\n" +
                        "                      ${BLUE} ▓▓▓▓▓  █▓█  ▓▓▓ █▓ ▓▓▓▓▓▓ ▓▓█  ▓▓█▓▓ █▓ ▒▓█\n" +
                        "                      ${BLUE} ▓▓    ▓▓▓▓  ▓▓▓ ▓▓  ▒▓▒  █▓▓▓  ▓▓     ▓░▓█\n" +
                        "                      ${BLUE} ▓▓▓▓  ▓▒▓▓  ▓▓▓▒▓█  █▓   ▓▒▓▓  ▓▓▓▓░  ▓▓▓\n" +
                        "${RED}             ░▒▒░     ${BLUE}▒▓▒░░ ▓▓░█▓ ░▓░█▓▓▒  ▓▓  ▓▓░▓▓    ░▓▓  █▓\n" +
                        "${RED}         ████████████ ${BLUE}█▓   ▒▓▓▓▓▓ █▓ ░▓▓   ▓▓ ░▓▓▓▓▓ █▓░░▓▓  ▓▓\n" +
                        "${RED}       ██████████████ ${BLUE}▓▓   ▓█  ▒█░██  ██   ▓█ ██  ░█▒ ▓▓▓▓   ██\n" +
                        "${RED}     ███████████████░\n" +
                        "${RED}    ████████████████        ▓█████▒            ░█████▓   ███████████████      ▒█████▓         ▒█████▓\n" +
                        "${RED}   █████████████████     ████████████░       ███████████▒███████████████   ░██████████     ████████████░\n" +
                        "${RED}  ██████████████████   ████████████████    █████████████░██████████████▓  ████████████   ████████████████\n" +
                        "${RED} ██████████░      ██  ██████████████████  █████████████ ███████████████ ░█████████████  ██████████████████\n" +
                        "${RED} █████████         ░ ███████████████████░░███████░░▓███ ███████████████░█████████████▒ ███████████████████░\n" +
                        "${RED}▒████████▒          █████████░ ░████████████████        ▓▒░░███████░░▒█████████▓  ░██ █████████░ ░█████████\n" +
                        "${RED}█████████           ███████░     ██████████████████▓       ███████░   ████████        ███████░     ████████\n" +
                        "${RED}█████████          ███████▒       ████████████████████     ███████    ███████        ████████       ███████\n" +
                        "${RED}██████████        ░███████       ░███████░█████████████    ███████   ░███████        ███████       ░███████\n" +
                        "${RED}███████████     ░█████████       ███████░  ████████████▒  ███████░   ████████        ███████       ███████▒\n" +
                        "${RED}▓█████████████████░████████     ████████      ░████████▒  ███████    ▓███████░     ░░████████     ████████\n" +
                        "${RED} █████████████████ █████████▒░▓████████░ ██     ███████   ███████     █████████▓▓███ █████████▒░▓████████▒\n" +
                        "${RED} ░████████████████ ░███████████████████ ███████████████  ▓██████▒     ██████████████ ░███████████████████\n" +
                        "${RED}  ▒███████████████  ██████████████████  ██████████████   ███████       █████████████  ██████████████████\n" +
                        "${RED}    █████████████░   ███████████████   ██████████████    ███████       ░███████████░   ███████████████\n" +
                        "${RED}      ███████████      ███████████     ░███████████     ▒███████         ██████████      ███████████\n" +
                        "${RED}                          ░▒▒░░            ░░▒▒░                            ░▒▒░░           ░▒▒░░\n" +
                        "${CYAN}                                                Where all your dreams come true\n" +
                        "${CYAN}                                                     (Got a deal for you!)${RESET}\n"
        )
    }
}
