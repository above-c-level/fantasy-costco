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
}
