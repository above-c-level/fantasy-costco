package games.skweekychair.fantasycostco

import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta

object TransactionUtils {

    /**
     * Handles the transaction of a player buying some number of items.
     * @param player The player buying the items.
     * @param item The item being bought.
     * @param amount The amount of items being bought.
     */
    fun handleBuyAmount(player: Player, merchandise: Merchandise, initialAmount: Int) {
        var amount = initialAmount
        val material = merchandise.material
        if (!withinBounds(player, material, amount)) {
            return
        }
        val membershipCard = MemberUtils.getMembershipCard(player)
        // Deal with cases where the player just wants to see prices
        if (membershipCard.justLooking) {
            handleJustLooking(player, merchandise, amount)
            return
        }

        val playerFunds = MemberUtils.getWalletRounded(player)
        var price = MemberUtils.roundDouble(merchandise.itemBuyPrice(amount))
        // Make sure the player has enough money to buy the items
        if (price > playerFunds) {
            if (!MemberUtils.getBuyMaxItems(player)) {
                player.sendMessage("${RED}Honey, you ain't got the money fo' that.")
                player.sendMessage(
                        "${RED}You only have ${WHITE}${MemberUtils.getWalletString(player)}${RED}, and you need ${WHITE}${MemberUtils.roundDoubleString(price)}."
                )
                return
            }
            // Since the price is nonlinear, we can do binary search to find the largest number
            // of items purchaseable.
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            price = result.second
            if (amount <= 0) {
                val singleItemPrice = MemberUtils.roundDoubleString(merchandise.itemBuyPrice(1))
                player.sendMessage("${RED}You can't buy any more of ${material.name}.")
                player.sendMessage(
                        "${RED}You only have ${WHITE}${MemberUtils.getWalletString(player)}${RED}, and you need ${WHITE}${singleItemPrice}${RED} for 1."
                )
                return
            }
        }

        val itemStack = ItemStack(material, amount)

        MemberUtils.walletSubtract(player, price)
        merchandise.buy(amount.toDouble())
        val remaining = player.inventory.addItem(itemStack)
        if (remaining.isNotEmpty()) {
            placeRemainingItems(remaining, player)
        }
        player.sendMessage(
                "${GREEN}You bought ${WHITE}${amount} ${material.name}${GREEN} for ${WHITE}${MemberUtils.roundDoubleString(price)}"
        )
        player.sendMessage(
                "${GREEN}Your wallet now contains ${WHITE}${MemberUtils.getWalletString(player)}"
        )
    }

    fun handleBuyFromSign(player: Player, location: Location) {
        val merchandise = MerchUtils.getMerchandiseAtLocation(location)
        val material = merchandise.material
        val signType = Cereal.signs[location]!!.signType
        val membershipCard = MemberUtils.getMembershipCard(player)

        if (membershipCard.useAmount) {
            handleBuyAmount(player, merchandise, membershipCard.buyGoal)
            return
        }

        var amount: Int
        when (signType) {
            SignType.BUY_ONE -> amount = 1
            SignType.BUY_STACK -> amount = material.maxStackSize
            SignType.BUY_SHULKER_BOX -> {
                handleBuyShulker(player, merchandise)
                return
            }
            SignType.TRUE_PRICE -> {
                if (membershipCard.justLooking) {
                    handleJustLooking(
                            player,
                            MerchUtils.getMerchandiseAtLocation(location),
                            membershipCard.buyGoal // Since this is true price value
                    )
                } else {
                    player.sendMessage(
                            "${RED}You don't have ${WHITE}/useamount${RED} set to true to buy from this sign (maybe try toggling the sign)"
                    )
                }
                return
            }
            else -> throw IllegalArgumentException("handleBuyFromSign called incorrectly")
        }

        if (membershipCard.justLooking) {
            handleJustLooking(player, merchandise, amount)
            return
        }
        handleBuyAmount(player, merchandise, amount)
    }

    /**
     * Handle the situation where the player just wants to see their price.
     * @param player The player who is buying.
     * @param merchandise The merchandise being bought.
     * @param amount The amount of items being bought.
     */
    fun handleJustLooking(player: Player, merchandise: Merchandise, initialAmount: Int) {
        var amount = initialAmount
        var price = MemberUtils.roundDouble(merchandise.itemBuyPrice(amount))
        val material = merchandise.material
        val playerFunds = MemberUtils.getWalletRounded(player)
        var newWallet = MemberUtils.roundDouble(playerFunds - price)
        var newWalletStr: String
        var roundedPrice: String
        if (newWallet < 0.0) {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "You wouldn't have enough money to buy that many ${material.name}s, since you have $newWalletStr and it costs $roundedPrice, but for now you're just looking"
            )
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            roundedPrice = MemberUtils.roundDoubleString(result.second)
            newWalletStr = MemberUtils.roundDoubleString(playerFunds - result.second)
            player.sendMessage(
                    "You could buy up to ${amount} instead for ${roundedPrice} leaving you with $newWalletStr, but for now you're just looking"
            )
        } else {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "It would cost you ${roundedPrice} and you would have ${newWalletStr} remaining in your wallet, but for now you're just looking"
            )
        }
    }

    private fun handleBuyShulker(player: Player, merchandise: Merchandise) {
        val material = merchandise.material
        val shulkerMat = Material.getMaterial("SHULKER_BOX")!!
        // Ideal price of shulker box because they're already buying upwards of 1728 items
        val shulkerMerch = MerchUtils.getMerchandise(shulkerMat)

        val filledPrice =
                merchandise.itemBuyPrice(merchandise.material.maxStackSize * 27) +
                        shulkerMerch.shownPrice
        if (MemberUtils.getWalletRounded(player) < filledPrice) {
            player.sendMessage(
                    "${RED}You don't have enough money to buy a shulker box full of ${material.name}."
            )
            return
        }
        MemberUtils.walletSubtract(player, filledPrice)
        merchandise.buy(merchandise.material.maxStackSize * 27.0)
        shulkerMerch.buy(1.0)
        // Make the shulker box
        var item = ItemStack(shulkerMat)

        val im = item.getItemMeta() as BlockStateMeta
        val shulkerbox = im.getBlockState() as ShulkerBox

        shulkerbox.inventory.addItem(ItemStack(material, merchandise.material.maxStackSize * 27))
        placeRemainingItems(player.inventory.addItem(item), player)
        // TODO: Make sure this works

    }

    private fun withinBounds(player: Player, material: Material, amount: Int): Boolean {
        // Make sure amount requested is valid
        if (amount < 0) {
            player.sendMessage("${RED}I can't give you negative items dude :/")
            return false
        } else if (amount == 0) {
            player.sendMessage("${GREEN}Aight here's your 0 ${material.name}")
            return false
        } else if (amount > CostcoGlobals.maxStacksPurchase * material.maxStackSize) {
            player.sendMessage("${RED}You can't buy that many items.")
            return false
        }
        return true
    }

    /**
     * Place any remaining items the player could not fit in their inventory on the ground.
     * @param remaining The remaining items in a hashmap of int:itemstack.
     * @param player The player who is buying.
     */
    private fun placeRemainingItems(remaining: HashMap<Int, ItemStack>, player: Player) {
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
