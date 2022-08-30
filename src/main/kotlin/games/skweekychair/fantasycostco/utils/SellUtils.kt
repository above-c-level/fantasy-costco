package games.skweekychair.fantasycostco

import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.Damageable

object SellUtils {
    fun handleSellAmount(player: Player, amount: Int): Boolean {
        if (MemberUtils.getMembershipCard(player).justLooking) {
            val newWallet =
                    MemberUtils.roundDoubleString(price + MemberUtils.getWalletRounded(player))
            val roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "${GREEN}You would receive ${WHITE}${roundedPrice}${GREEN} in the sale and have ${WHITE}${newWallet}${GREEN} in your wallet, but for now you're just looking"
            )
            return true
        }
    }

    /**
     * Handles the transaction of a player selling to a sign
     * @param player The player selling the items.
     * @param location The location of the sign.
     */
    fun handleSellToSign(player: Player, location: Location) {
        val itemInHand = player.inventory.itemInMainHand
        // TODO: Check to make sure the item (if not air) is not a shulker box containing items
        val signType = Cereal.signs[location]!!.signType
        val membershipCard = MemberUtils.getMembershipCard(player)
        var amount: Int
        when (signType) {
            SignType.SELL_ONE -> handleSellSingle(player)
            SignType.SELL_STACK -> handleSellStack(player)
            SignType.SELL_TYPE -> handleSellType(player)
            SignType.SELL_ALL -> handleSellAll(player)
            else -> throw IllegalArgumentException("handleBuyFromSign called incorrectly")
        }
    }


    fun handleSellSingle(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val item = itemInHand.type
        val merchandise = MerchUtils.getMerchandise(item)
        val amount = 1
        if (!isAccepted(player, itemInHand, merchandise)) {
            return
        }
        val price = merchandise.itemSellPrice(amount)
        if (price.isNaN()) {
            player.sendMessage("Don't sell air man!")
        }
    }

    fun handleSellStack(player: Player): Boolean {
        val itemInHand = player.inventory.itemInMainHand
        val item = itemInHand.type
        val merchandise = MerchUtils.getMerchandise(item)
        val amount = itemInHand.amount
        if (!isAccepted(player, itemInHand, merchandise)) {
            return true
        }
        val price = merchandise.itemSellPrice(amount)
        if (price.isNaN()) {
            player.sendMessage("Don't sell air man!")
        }
        performSale(player, price, amount, merchandise)
    }

    /**
     * Handle the situation where a player is selling all of one type of item.
     * @param player The player who is selling the items
     * @param merchandise The merchandise being sold.
     */
    private fun handleSellType(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val merchandise = MerchUtils.getMerchandise(itemInHand)
        // TODO: If enchantment support is added, update this to handle them
        val enchantments = itemInHand.enchantments
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmount = 0
        if (!isAccepted(player, itemInHand, merchandise)) {
            return
        }
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            if (item.type == itemInHand.type && item.enchantments == enchantments) {
                sellAmount += item.amount
                inventory.setItem(i, null)
            }
        }
        val priceToAdd = merchandise.itemSellPrice(sellAmount)
        MemberUtils.walletAdd(player, priceToAdd)
    }

    fun handleSellAll(player: Player) {
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmounts = HashMap<BaseMerchandise, Int>()
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            val merchandise = MerchUtils.getMerchandise(item)
            if (item.type == itemInHand.type) {
                sellAmount += item.amount
                inventory.setItem(i, null)
            }
        }
        val priceToAdd = merchandise.itemSellPrice(sellAmount)
        MemberUtils.walletAdd(player, priceToAdd)
    }

    /**
     * Handles the situation where a player is just looking or sneaking.
     * @param player The player buying items
     * @param location The location of the sign
     */
    fun handleJustLookingAtSign(player: Player, location: Location) {
        val merchandise = MerchUtils.getMerchandiseAtLocation(location)
        val material = merchandise.material
        val signType = Cereal.signs[location]!!.signType
        val membershipCard = MemberUtils.getMembershipCard(player)
        var amount: Int
        when (signType) {
            SignType.BUY_ONE -> amount = 1
            SignType.BUY_STACK -> amount = material.maxStackSize
            SignType.BUY_SHULKER_BOX -> {
                handleJustLookingShulker(player, merchandise)
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
        handleJustLooking(player, merchandise, amount)
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
        val playerFunds = MemberUtils.getWalletRounded(player)
        var newWallet = MemberUtils.roundDouble(playerFunds - price)
        var newWalletStr: String
        var roundedPrice: String
        if (!withinBounds(player, merchandise)) {
            return
        }
        if (newWallet < 0.0) {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "You wouldn't have enough money to buy that many of ${merchandise.getName()}, since you have $newWalletStr and it costs $roundedPrice"
            )
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            roundedPrice = MemberUtils.roundDoubleString(result.second)
            newWalletStr = MemberUtils.roundDoubleString(playerFunds - result.second)
            player.sendMessage(
                    "You could buy up to ${amount} instead for ${roundedPrice} leaving you with $newWalletStr"
            )
        } else {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "It would cost you ${roundedPrice} and you would have ${newWalletStr} remaining in your wallet"
            )
        }
    }

    /**
     * Handle the situation where a player is just looking and right clicks on a sign selling
     * shulker boxes containing items.
     * @param player The player who is buying.
     * @param merchandise The merchandise being bought.
     */
    private fun handleJustLookingShulker(player: Player, merchandise: Merchandise) {
        val material = merchandise.material
        var amount = 27 * material.maxStackSize
        val shulkerMaterial = Material.SHULKER_BOX
        val shulkerMerch = MerchUtils.getMerchandise(shulkerMaterial)
        var price =
                MemberUtils.roundDouble(merchandise.itemBuyPrice(amount)) +
                        MemberUtils.roundDouble(shulkerMerch.itemBuyPrice(1))
        val playerFunds = MemberUtils.getWalletRounded(player)
        var newWallet = MemberUtils.roundDouble(playerFunds - price)
        var newWalletStr: String
        var roundedPrice: String
        if (newWallet < 0.0) {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "You wouldn't have enough money to buy a shulker full of ${merchandise.getName()}, since you have $newWalletStr and it costs $roundedPrice"
            )
        } else {
            newWalletStr = MemberUtils.roundDoubleString(newWallet)
            roundedPrice = MemberUtils.roundDoubleString(price)
            player.sendMessage(
                    "It would cost you ${roundedPrice} and you would have ${newWalletStr} remaining in your wallet"
            )
        }
    }

    private fun isAccepted(player: Player, item: ItemStack, merchandise: Merchandise): Boolean {
        // TODO: If we ever accept enchanted items, deal with that
        if (merchandise.enchantments.isNotEmpty()) {
            player.sendMessage("Enchantments are not yet supported")
            return false
        }
        if (item.type == Material.AIR) {
            player.sendMessage("Don't sell air man!")
            return false
        }
        // TODO: If we ever accept damaged items, deal with that
        val damageable = item.getItemMeta() as Damageable
        if (damageable.hasDamage()) {
            player.sendMessage("Sorry, but we can't accept damaged goods :/")
            return false
        } else if (CostcoGlobals.isNotAccepted(item.type)) {
            player.sendMessage("Sorry, but we don't accept that item :/")
            return false
        }
        return true
    }

    private fun performSale (player: Player, price: Double, amount: Int, merchandise: Merchandise) {
        MemberUtils.walletAdd(player, price)
        val playerFunds = MemberUtils.getWalletRounded(player)
        player.sendMessage("${playerFunds}")
        player.inventory.setItemInMainHand(null)
        merchandise.sell(amount.toDouble())
        player.sendMessage(
                "${GREEN}You received ${WHITE}${MemberUtils.roundDoubleString(price)}${GREEN} in the sale"
        )
        player.sendMessage(
                "${GREEN}You now have ${WHITE}${MemberUtils.getWalletString(player)}${GREEN} in your wallet"
        )
    }
}
