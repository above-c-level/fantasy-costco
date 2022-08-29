package games.skweekychair.fantasycostco

import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object TransactionUtils {

    /**
     * Handles the transaction of a player buying some number of items.
     * @param player The player buying the items.
     * @param item The item being bought.
     * @param amount The amount of items being bought.
     */
    fun handleBuyAmount(player: Player, material: Material, initialAmount: Int) {
        var amount = initialAmount
        val merchandise = getMerchandise(material)
        val membershipCard = getMembershipCard(player)
        // Deal with cases where the player just wants to see prices
        if (membershipCard.justLooking) {
            handleJustLooking(player, merchandise, amount)
            return
        }

        val playerFunds = getWalletRounded(player)
        var price = roundDouble(merchandise.itemBuyPrice(amount))
        // Make sure the player has enough money to buy the items
        if (price > playerFunds) {
            if (!getBuyMaxItems(player)) {
                player.sendMessage("${RED}Honey, you ain't got the money fo' that.")
                player.sendMessage(
                        "${RED}You only have ${WHITE}${getWalletString(player)}${RED}, and you need ${WHITE}${roundDoubleString(price)}."
                )
                return
            }
            // Since the price is nonlinear, we can do binary search to find the largest number
            // of items purchaseable.
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            price = result.second
            if (amount <= 0) {
                val singleItemPrice = roundDoubleString(merchandise.itemBuyPrice(1))
                player.sendMessage("${RED}You can't buy any more of ${material.name}.")
                player.sendMessage(
                        "${RED}You only have ${WHITE}${getWalletString(player)}${RED}, and you need ${WHITE}${singleItemPrice}${RED} for 1."
                )
                return
            }
        }

        val itemStack = ItemStack(material, amount)

        walletSubtract(player, price)
        merchandise.buy(amount.toDouble())
        val remaining = player.inventory.addItem(itemStack)
        if (remaining.isNotEmpty()) {
            placeRemainingItems(remaining, player)
        }
        player.sendMessage(
                "${GREEN}You bought ${WHITE}${amount} ${material.name}${GREEN} for ${WHITE}${roundDoubleString(price)}"
        )
        player.sendMessage("${GREEN}Your wallet now contains ${WHITE}${getWalletString(player)}")
    }

    /**
     * Handle the situation where the player just wants to see their price.
     * @param player The player who is buying.
     * @param merchandise The merchandise being bought.
     * @param amount The amount of items being bought.
     */
    fun handleJustLooking(player: Player, merchandise: Merchandise, initialAmount: Int) {
        var amount = initialAmount
        var price = roundDouble(merchandise.itemBuyPrice(amount))
        val material = merchandise.material
        val playerFunds = getWalletRounded(player)
        var newWallet = roundDouble(playerFunds - price)
        var newWalletStr: String
        var roundedPrice: String
        if (newWallet < 0.0) {
            newWalletStr = roundDoubleString(newWallet)
            roundedPrice = roundDoubleString(price)
            player.sendMessage(
                    "You wouldn't have enough money to buy that many ${material.name}s, since you have $newWalletStr and it costs $roundedPrice, but for now you're just looking"
            )
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            roundedPrice = roundDoubleString(result.second)
            newWalletStr = roundDoubleString(playerFunds - result.second)
            player.sendMessage(
                    "You could buy up to ${amount} instead for ${roundedPrice} leaving you with $newWalletStr, but for now you're just looking"
            )
        } else {
            newWalletStr = roundDoubleString(newWallet)
            roundedPrice = roundDoubleString(price)
            player.sendMessage(
                    "It would cost you ${roundedPrice} and you would have ${newWalletStr} remaining in your wallet, but for now you're just looking"
            )
        }
    }

    /**
     * Place any remaining items the player could not fit in their inventory on the ground.
     * @param remaining The remaining items in a hashmap of int:itemstack.
     * @param player The player who is buying.
     */
    private fun placeRemainingItems(remaining: HashMap<Int!, ItemStack!>, player: Player) {
        for (entry in remaining) {
            // val argnum = entry.key
            // figure out how many items fit in one stack
            val stackSize = entry.value.maxStackSize
            // figure out how many full stacks there are in entry.value
            val fullStacks = entry.value.amount / stackSize
            // figure out how many items are left over
            val leftover = entry.value.amount % stackSize
            // drop the full stacks
            val fullItemStack = ItemStack(entry.value.type, stackSize)
            for (i in 0 until fullStacks) {
                player.world.dropItem(player.location, fullItemStack)
            }
            val leftoverItemStack = ItemStack(entry.value.type, leftover)
            // drop the leftover items
            if (leftover > 0) {
                player.world.dropItem(player.location, leftoverItemStack)
            }
        }
    }
}
