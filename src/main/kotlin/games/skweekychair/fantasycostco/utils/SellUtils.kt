package games.skweekychair.fantasycostco

import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

object SellUtils {
    /**
     * Handles the transaction of a player selling to a sign
     * @param player The player selling the items.
     * @param location The location of the sign.
     */
    fun handleSellToSign(player: Player, location: Location) {
        // TODO: Check to make sure the item (if not air) is not a shulker box containing items
        val signType = Cereal.signs[location]!!.signType
        when (signType) {
            SignType.SELL_ONE -> handleSellSingle(player)
            SignType.SELL_STACK -> handleSellStack(player)
            SignType.SELL_TYPE -> handleSellType(player)
            SignType.SELL_ALL -> handleSellAll(player)
            else -> throw IllegalArgumentException("handleBuyFromSign called incorrectly")
        }
    }

    /**
     * Handles the transaction of a player selling a single item.
     * @param player The player selling the item.
     */
    fun handleSellSingle(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val item = itemInHand.type
        val merchandise = MerchUtils.getMerchandise(item)
        val amount = 1
        if (!isAccepted(player, itemInHand)) {
            return
        }
        val price = merchandise.itemSellPrice(amount)
        if (price.isNaN()) {
            player.sendMessage("Don't sell air man!")
        }
    }

    fun handleSellStack(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val item = itemInHand.type
        val merchandise = MerchUtils.getMerchandise(item)
        val amount = itemInHand.amount
        if (!isAccepted(player, itemInHand)) {
            return
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
        // TODO: When enchantment support is added, update this to handle them
        val enchantments = itemInHand.enchantments
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmount = 0
        if (!isAccepted(player, itemInHand)) {
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

            if (!isAccepted(player, item, false)) {
                continue
            }

            val baseMerch = MerchUtils.getMerchandise(item).baseMerch()
            if (!sellAmounts.containsKey(baseMerch)) {
                sellAmounts[baseMerch] = item.amount
            } else {
                sellAmounts[baseMerch] = sellAmounts[baseMerch]!! + item.amount
            }
            inventory.setItem(i, null)
        }
        var totalPrice = 0.0
        for ((baseMerch, amount) in sellAmounts) {
            val merchandise = MerchUtils.getMerchandise(baseMerch)
            val price = merchandise.itemSellPrice(amount)
            MemberUtils.walletAdd(player, price)
            val playerFunds = MemberUtils.getWalletRounded(player)
            player.sendMessage("${playerFunds}")
            player.inventory.setItemInMainHand(null)
            merchandise.sell(amount.toDouble())
            totalPrice += price
        }
        player.sendMessage(
                "${GREEN}You received ${WHITE}${MemberUtils.roundDoubleString(totalPrice)}${GREEN} in the sale"
        )
        player.sendMessage(
                "${GREEN}You now have ${WHITE}${MemberUtils.getWalletString(player)}${GREEN} in your wallet"
        )
    }

    /**
     * Handle the situation where the player just wants to see their price.
     * @param player The player who is selling the items.
     */
    fun handleJustLookingAtSign(player: Player, location: Location) {
        // TODO: Check to make sure the item (if not air) is not a shulker box containing items
        val signType = Cereal.signs[location]!!.signType
        when (signType) {
            SignType.SELL_ONE -> handleJustLookingSingle(player)
            SignType.SELL_STACK -> handleJustLookingStack(player)
            SignType.SELL_TYPE -> handleJustLookingType(player)
            SignType.SELL_ALL -> handleJustLookingAll(player)
            else -> throw IllegalArgumentException("handleBuyFromSign called incorrectly")
        }
    }

    fun handleJustLookingSingle(player: Player) {
        handleJustLookingAmount(player, 1)
    }

    fun handleJustLookingStack(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val amount = itemInHand.amount
        handleJustLookingAmount(player, amount)
    }

    fun handleJustLookingType(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val merchandise = MerchUtils.getMerchandise(itemInHand)
        // TODO: When enchantment support is added, update this to handle them
        val enchantments = itemInHand.enchantments
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmount = 0
        if (!isAccepted(player, itemInHand)) {
            return
        }
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            if (item.type == itemInHand.type && item.enchantments == enchantments) {
                sellAmount += item.amount
            }
        }
        handleJustLookingAmount(player, sellAmount)
    }

    fun handleJustLookingAll(player: Player) {
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmounts = HashMap<BaseMerchandise, Int>()
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            if (!isAccepted(player, item, false)) {
                continue
            }
            val baseMerch = MerchUtils.getMerchandise(item).baseMerch()
            if (!sellAmounts.containsKey(baseMerch)) {
                sellAmounts[baseMerch] = item.amount
            } else {
                sellAmounts[baseMerch] = sellAmounts[baseMerch]!! + item.amount
            }
        }
        var totalValue = 0.0
        for ((baseMerch, amount) in sellAmounts) {
            val merchandise = MerchUtils.getMerchandise(baseMerch)
            val value = merchandise.itemSellPrice(amount)
            totalValue += value
        }
        var newWallet = MemberUtils.getWalletRounded(player) + totalValue

        player.sendMessage(
                "${GREEN}You would receive ${WHITE}${MemberUtils.roundDoubleString(totalValue)}${GREEN} in the sale, resulting in ${WHITE}${MemberUtils.roundDoubleString(newWallet)}${GREEN} in your wallet"
        )
    }

    fun handleJustLookingAmount(player: Player, amount: Int) {
        val item = player.inventory.itemInMainHand
        val merchandise = MerchUtils.getMerchandise(item)
        val price = merchandise.itemSellPrice(amount)
        val newWallet = MemberUtils.roundDoubleString(price + MemberUtils.getWalletRounded(player))
        val roundedPrice = MemberUtils.roundDoubleString(price)
        player.sendMessage(
                "${GREEN}You would receive ${WHITE}${roundedPrice}${GREEN} in the sale and have ${WHITE}${newWallet}${GREEN} in your wallet"
        )
    }

    private fun isAccepted(player: Player, item: ItemStack, sendMessages: Boolean = true): Boolean {
        // TODO: If we ever accept enchanted items, deal with that
        if (item.enchantments.isNotEmpty()) {
            if (sendMessages) {
                player.sendMessage("Enchantments are not yet supported")
            }
            return false
        }
        if (item.type == Material.AIR) {
            if (sendMessages) {
                player.sendMessage("Don't sell air man!")
            }
            return false
        }
        // TODO: If we ever accept damaged items, deal with that
        val damageable = item.getItemMeta() as Damageable
        if (damageable.hasDamage()) {
            if (sendMessages) {
                player.sendMessage("Sorry, but we can't accept damaged goods :/")
            }
            return false
        } else if (CostcoGlobals.isNotAccepted(item.type)) {
            if (sendMessages) {
                player.sendMessage("Sorry, but we don't accept that item :/")
            }
            return false
        }
        return true
    }

    private fun performSale(player: Player, price: Double, amount: Int, merchandise: Merchandise) {
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
