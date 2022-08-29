package games.skweekychair.fantasycostco

import java.util.Locale
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.entity.Player

object MembershipUtils {
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
    fun roundDoubleString(value: Double): String {
        return "₿${String.format(Locale("en", "US"), "%,.2f", value)}"
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
}
