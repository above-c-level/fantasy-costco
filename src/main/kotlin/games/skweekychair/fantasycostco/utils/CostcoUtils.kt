package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Bukkit

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
