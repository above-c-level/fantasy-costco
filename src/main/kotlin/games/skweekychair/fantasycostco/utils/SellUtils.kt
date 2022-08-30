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
        if (isShulkerInv(itemInHand)) {
            sellShulkerInv(player, itemInHand)
            return
        }
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

    /**
     * Handle the situation where a player is selling a stack in their hand
     * @param player The player selling the item(s)
     */
    fun handleSellStack(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        if (isShulkerInv(itemInHand)) {
            sellShulkerInv(player, itemInHand)
            return
        }
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
        if (isShulkerInv(itemInHand)) {
            sellShulkerInv(player, itemInHand)
            return
        }
        val merchandise = MerchUtils.getMerchandise(itemInHand)
        val amount = getSellAmountFromInventory(player, itemInHand)
        if (amount < 0) {
            return
        }
        val priceToAdd = merchandise.itemSellPrice(amount)
        MemberUtils.walletAdd(player, priceToAdd)
    }

    /**
     * Get the amount of items that can be sold from a player's inventory.
     * @param player The player who is selling the items.
     * @param item The item being sold.
     * @param setItemToNull If true, each item in the inventory will be set to null
     * @return The amount of items that can be sold.
     */
    private fun getSellAmountFromInventory(
            player: Player,
            itemInHand: ItemStack,
            setItemToNull: Boolean
    ): Int {
        // TODO: When enchantment support is added, update this to handle them
        val enchantments = itemInHand.enchantments
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmount = 0
        if (!isAccepted(player, itemInHand)) {
            return -1
        }
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            if (item.type == itemInHand.type && item.enchantments == enchantments) {
                sellAmount += item.amount
                if (setItemToNull) {
                    inventory.setItem(i, null)
                }
            }
        }
        return sellAmount
    }

    /**
     * Handle the situation where a player is selling their entire inventory. Currently unused to
     * prevent players from selling all of their items and then complaining lmao
     * @param player The player selling the items.
     */
    fun handleSellAll(player: Player) {
        val inventory = player.inventory
        // Inventory has 36 items excluding armor slots and offhand
        var sellAmounts = HashMap<BaseMerchandise, Int>()
        for (i in 0 until 36) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }
            if (isShulkerInv(item)) {
                sellShulkerInv(player, item, false)
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

    /**
     * Handle the situation where the player just wants to see their price for a single item.
     * @param player The player who is (looking at) selling their items.
     */
    private fun handleJustLookingSingle(player: Player) {
        handleJustLookingAmount(player, 1)
    }

    /**
     * Handle the situation where the player just wants to see their price for a stack of items.
     * @param player The player who is (looking at) selling their items.
     */
    fun handleJustLookingStack(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val amount = itemInHand.amount
        handleJustLookingAmount(player, amount)
    }

    /**
     * Handle the situation where the player just wants to see their price for all of one type of
     * item.
     * @param player The player who is (looking at) selling their items.
     */
    private fun handleJustLookingType(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        val sellAmount = getSellAmountFromInventory(player, itemInHand, false)
        if (sellAmount < 0) {
            return
        }
        handleJustLookingAmount(player, sellAmount)
    }

    /**
     * Handle the situation where the player just wants to see their price for all of their
     * inventory. Currently unused as it is complementary to handleSellAll.
     * @param player The player who is (looking at) selling their items.
     */
    private fun handleJustLookingAll(player: Player) {
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

    /**
     * Tell a player how much they would hypothetically receive if they sold `amount` of the item in
     * their hand.
     * @param player The player who is (looking at) selling their items.
     * @param amount The amount of items the player is selling.
     */
    private fun handleJustLookingAmount(player: Player, amount: Int) {
        val item = player.inventory.itemInMainHand
        val merchandise = MerchUtils.getMerchandise(item)
        val price = merchandise.itemSellPrice(amount)
        val newWallet = MemberUtils.roundDoubleString(price + MemberUtils.getWalletRounded(player))
        val roundedPrice = MemberUtils.roundDoubleString(price)
        player.sendMessage(
                "${GREEN}You would receive ${WHITE}${roundedPrice}${GREEN} in the sale and have ${WHITE}${newWallet}${GREEN} in your wallet"
        )
    }

    /**
     * Whether the player is allowed to sell `item`.
     * @param player The player who is selling their items.
     * @param item The item the player is selling.
     * @param sendMessages Whether to send messages to the player regarding the lack of sellability.
     * @return True if the player is allowed to sell `item` and false otherwise.
     */
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
        val merchandise = MerchUtils.getMerchandise(item)
        if (merchandise.listOfSigns.isEmpty()) {
            if (sendMessages) {
                player.sendMessage(
                        "${RED}Sorry, we don't currently sell or accept ${merchandise.getName()}. Try bugging us if you think we should."
                )
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

    /**
     * Handle the situation where the player wants to sell all of their inventory
     * @param player The player who is selling their items
     * @param price The price of the items the player is selling
     * @param amount The amount of items the player is selling
     * @param merchandise The merchandise the plyer is selling
     */
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

    /**
     * Check whether an item is a shulker box
     * @param item The item in question
     * @return True if the item is a shulker box and false otherwise
     */
    private fun isShulkerInv(item: ItemStack): Boolean {
        if (item.getItemMeta() !is BlockStateMeta) {
            return false
        }
        val itemMeta = item.getItemMeta() as BlockStateMeta

        if (itemMeta.getBlockState() !is ShulkerBox) {
            return false
        }
        return true
    }

    /**
     * Sell the items of a shulker box.
     * @param player The player who is selling their items.
     * @param shulkerItem The shulker box item
     * @param sendMessages Whether to send messages to the player about how much they received in
     * the sale
     */
    private fun sellShulkerInv(
            player: Player,
            shulkerItem: ItemStack,
            sendMessages: Boolean = true
    ) {
        val shulkerMeta = shulkerItem.getItemMeta() as BlockStateMeta
        val shulkerBox = shulkerMeta.getBlockState() as ShulkerBox
        val inventory = shulkerBox.inventory

        var sellAmounts = HashMap<Merchandise, Int>()
        for (i in 0 until inventory.size) {
            val item = inventory.getItem(i)
            if (item == null) {
                continue
            }

            if (!isAccepted(player, item, false)) {
                continue
            }

            val merchandise = MerchUtils.getMerchandise(item)
            if (!sellAmounts.containsKey(merchandise)) {
                sellAmounts[merchandise] = item.amount
            } else {
                sellAmounts[merchandise] = sellAmounts[merchandise]!! + item.amount
            }
            inventory.setItem(i, null)
        }
        var totalPrice = 0.0
        for ((merchandise, amount) in sellAmounts) {
            val price = merchandise.itemSellPrice(amount)
            MemberUtils.walletAdd(player, price)
            merchandise.sell(amount.toDouble())
            totalPrice += price
        }
        shulkerMeta.setBlockState(shulkerBox)
        shulkerItem.setItemMeta(shulkerMeta)
        if (sendMessages) {
            player.sendMessage(
                    "${GREEN}You received ${WHITE}${MemberUtils.roundDoubleString(totalPrice)}${GREEN} in the sale"
            )
            player.sendMessage(
                    "${GREEN}You now have ${WHITE}${MemberUtils.getWalletString(player)}${GREEN} in your wallet"
            )
        }
    }
}
