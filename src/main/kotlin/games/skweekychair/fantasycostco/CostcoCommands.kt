package games.skweekychair.fantasycostco

import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.util.StringUtil

/** Allows the player to purchase items in exchange for money */
object BuyCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        // Make sure the sender is a player
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // Check permission
        if (!sender.hasPermission("fantasycostco.buy")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return true
        }
        val player: Player = sender

        // Make sure the command was called with the right amount of arguments
        if (args.size == 0) {
            sender.sendMessage("${RED}You must specify an item to buy.")
            return false
        } else if (args.size > 2) {
            sender.sendMessage("${RED}This command only takes up to two arguments")
            return false
        }

        // Make sure the material submitted is valid
        val material = Material.matchMaterial(args[0])

        if (material == null || !material.isItem) {
            sender.sendMessage("${RED}Not an item.")
            return true
        }

        // Get amount if specified, or fall back to previously set value
        var amount: Int?
        if (args.size == 2) {
            amount = args[1].toIntOrNull()
        } else {
            amount = getMembershipCard(player).buyGoal
        }
        if (amount == null) {
            sender.sendMessage("${RED}Not an amount.")
            return false
        }

        // Make sure amount requested is valid
        if (amount < 0) {
            sender.sendMessage("${RED}I can't give you negative items dude :/")
            return true
        } else if (amount == 0) {
            sender.sendMessage("${GREEN}Aight here's your 0 ${material.name}")
            return true
        } else if (amount > 27 * material.maxStackSize) {
            sender.sendMessage("${RED}You can't buy that many items.")
            return true
        }

        val merchandise = getMerchandise(material)
        var price = roundDouble(merchandise.itemBuyPrice(amount))
        val playerFunds = getWalletRounded(player)
        val membershipCard = getMembershipCard(player)
        // Deal with cases where the player just wants to see prices
        if (membershipCard.justLooking) {
            var newWallet = roundDouble(playerFunds - price)
            var newWalletStr: String
            var roundedPrice: String
            if (newWallet < 0.0) {
                newWalletStr = roundDoubleString(newWallet)
                roundedPrice = roundDoubleString(price)
                sender.sendMessage(
                        "You wouldn't have enough money to buy that many ${material.name}s, since you have $newWalletStr and it costs $roundedPrice, but for now you're just looking"
                )
                val result = binarySearchPrice(amount, merchandise, playerFunds)
                amount = result.first
                roundedPrice = roundDoubleString(result.second)
                newWalletStr = roundDoubleString(playerFunds - result.second)
                sender.sendMessage(
                        "You could buy up to ${amount} instead for ${roundedPrice} leaving you with $newWalletStr, but for now you're just looking"
                )
                return true
            } else {
                newWalletStr = roundDoubleString(newWallet)
                roundedPrice = roundDoubleString(price)
                player.sendMessage(
                        "It would cost you ${roundedPrice} and you would have ${newWalletStr} remaining in your wallet, but for now you're just looking"
                )
            }
            return true
        }

        // Make sure the player has enough money to buy the items
        if (price > playerFunds) {
            val buyMaxItems = getBuyMaxItems(player)
            if (!buyMaxItems) {
                sender.sendMessage("${RED}Honey, you ain't got the money fo' that.")
                sender.sendMessage(
                        "${RED}You only have ${WHITE}${getWalletString(player)}${RED}, and you need ${WHITE}${roundDoubleString(price)}."
                )
                return true
            }
            // Since the price is nonlinear, we can do binary search to find the largest number
            // of items purchaseable.
            val result = binarySearchPrice(amount, merchandise, playerFunds)
            amount = result.first
            price = result.second
            if (amount <= 0) {
                val singleItemPrice = roundDoubleString(merchandise.itemBuyPrice(1))
                sender.sendMessage("${RED}You can't buy any more of ${material.name}.")
                sender.sendMessage(
                        "${RED}You only have ${WHITE}${getWalletString(player)}${RED}, and you need ${WHITE}${singleItemPrice}${RED} for 1."
                )
                return true
            }
        }

        val itemStack = ItemStack(material, amount)

        walletSubtract(player, price)
        merchandise.buy(amount.toDouble())
        val remaining = player.inventory.addItem(itemStack)
        if (remaining.isNotEmpty()) {
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
        player.sendMessage(
                "${GREEN}You bought ${WHITE}${amount} ${material.name}${GREEN} for ${WHITE}${roundDoubleString(price)}"
        )
        player.sendMessage("${GREEN}Your wallet now contains ${WHITE}${getWalletString(player)}")
        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        val completions = mutableListOf<String>()
        if (args.size == 1) {
            val items =
                    Material.values().filter { material -> material.isItem() }.map { material ->
                        material.toString()
                    }
            StringUtil.copyPartialMatches(args[0], items, completions)
        }
        completions.sort()
        return completions
    }
}

/** Allows the player to sell items in exchange for money. */
object SellCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // Check permissions
        if (!sender.hasPermission("fantasycostco.sell")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return true
        }

        // Check arguments
        if (args.size > 0) {
            "${RED}This command doesn't take any arguments"
            return false
        }
        val player: Player = sender
        val item = player.inventory.itemInMainHand
        val itemCount = item.amount
        val merchandise = getMerchandise(item)
        val price = merchandise.itemSellPrice(item.amount)

        if (merchandise.itemSellPrice(item.amount).isNaN()) {
            player.sendMessage("Don't sell air man!")
            return false
        }
        val damageable = item.getItemMeta() as Damageable
        if (damageable.hasDamage()) {
            player.sendMessage("Sorry, but we can't accept damaged goods :/")
            return true
        } else if (CostcoGlobals.isNotAccepted(item.type)) {
            player.sendMessage("Sorry, but we don't accept that item :/")
            return true
        }

        if (getMembershipCard(player).justLooking) {
            val newWallet = roundDoubleString(price + getWalletRounded(player))
            val roundedPrice = roundDoubleString(price)
            player.sendMessage(
                    "${GREEN}You would receive ${WHITE}${roundedPrice}${GREEN} in the sale and have ${WHITE}${newWallet}${GREEN} in your wallet, but for now you're just looking"
            )
            return true
        }

        walletAdd(player, price)
        val playerFunds = getWalletRounded(player)
        player.sendMessage("${playerFunds}")
        player.inventory.setItemInMainHand(null)
        merchandise.sell(itemCount.toDouble())
        player.sendMessage(
                "${GREEN}You received ${WHITE}${roundDoubleString(price)}${GREEN} in the sale"
        )
        player.sendMessage(
                "${GREEN}You now have ${WHITE}${getWalletString(player)}${GREEN} in your wallet"
        )

        // tryDiscordBroadcast("TAX FRAUD 🚨🚨⚠️⚠️ **__A  L  E  R  T__** ⚠️⚠️🚨🚨")
        // tryOnlyDiscord("https://tenor.com/view/burnt-demonic-demon-scream-screaming-gif-13844791")

        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return listOf<String>()
    }
}

// region wallet
/** Shows the player how much money they have in their wallet. */
object GetWalletCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // Check permission
        if (!sender.hasPermission("fantasycostco.wallet")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return true
        }

        // Check args
        if (args.size > 0) {
            "${RED}This command doesn't take any arguments"
            return false
        }
        val player: Player = sender
        player.sendMessage("${GREEN}You have ${WHITE}${getWalletString(player)}")
        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return listOf<String>()
    }
}

/** Sets the amount of money a player has in their wallet */
object SetWalletCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        // If sender does not have permission, return false
        if (!sender.hasPermission("fantasycostco.set-wallet")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return false
        }
        var player: Player?
        // Make sure there are two arguments
        if (args.size == 0) {
            sender.sendMessage("${RED}You must specify an amount to set the wallet to")
            return false
        } else if (args.size == 1) {
            // Make sure they are a player
            if (sender !is Player) {
                sender.sendMessage(
                        "${RED}You must be a player if don't specify the player to set the wallet of"
                )
                return false
            }
            player = sender
        } else if (args.size == 2) {

            // Get player
            val playerArg: String = args[0]
            player = Bukkit.getPlayer(playerArg)
            if (player == null) {
                sender.sendMessage("${RED}Player ${WHITE}${playerArg}${RED} not found")
                return false
            }
        } else {
            sender.sendMessage("${RED}This command only takes two arguments")
            return false
        }

        // Get amount
        val amount = args[0].toDoubleOrNull()
        if (amount == null) {
            sender.sendMessage("${RED}Not a valid amount.")
            return false
        }
        // Set wallet with amount
        setWallet(player, amount)
        if (player == sender) {
            sender.sendMessage("${GREEN}You have set your wallet to ${WHITE}${amount}")
        } else {
            sender.sendMessage(
                    "${GREEN}You have set ${WHITE}${player.name}${GREEN}'s wallet to ${WHITE}${amount}"
            )
        }
        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return listOf<String>()
    }
}
// endregion


/**
 * Gets or changes whether a player will buy as many items as possible if they can't afford
 * the requested amount
 */
object BuyPossibleCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // Check args number
        if (args.size == 0) {
            // TODO: Rework message? the usage from false might be good enough tho
            // sender.sendMessage(
            //         "${RED}You must specify whether you want to be just looking or not"
            // )
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = getMembershipCard(sender)

        // Get argument
        val status = when(args[0]) {
            "true" -> true
            "false" -> false
            "get" -> membershipCard.buyMaxItems
            "toggle" -> !membershipCard.buyMaxItems
            else -> { return false }
        }

        membershipCard.buyMaxItems = status
        if (membershipCard.buyMaxItems) {
            sender.sendMessage(
                    "You ${if (args[0] == "get") "currently" else "now" } ${GREEN}will${WHITE} buy as many items as you can afford if you don't have enough money on /buy calls"
            )
        } else {
            sender.sendMessage(
                    "You ${RED}${if (args[0] == "get") "currently" else "now" } ${RED}will not${WHITE} buy as many items as you can afford if you don't have enough money on /buy calls"
            )
        }
        return true
    }
    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return if (args.size == 1) listOf<String>("true", "false", "get", "toggle") else listOf<String>()
    }
}

// endregion

// region buy amount
/** Allows a player to set how many items they want to buy */
object SetBuyAmountCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // If sender does not have permission, return false
        if (!sender.hasPermission("fantasycostco.buy-amount")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return true
        }

        // Make sure there's an argument to set the buy amount to
        if (args.size == 0) {
            sender.sendMessage("${RED}You must specify the amount you want to buy")
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }
        // Make sure argument is an int
        val amount = args[0].toIntOrNull()
        if (amount == null) {
            sender.sendMessage("${RED}Not a valid amount.")
            return false
        }
        val membershipCard = getMembershipCard(sender)
        membershipCard.buyGoal = amount
        sender.sendMessage("${GREEN}Your buy goal is now set to ${WHITE}${amount}")

        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return listOf<String>()
    }
}

/** Lets a player know how many items they want to buy as previously set */
object GetBuyAmountCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }
        if (!sender.hasPermission("fantasycostco.buy-amount")) {
            sender.sendMessage("${RED}You don't have permission to use this command.")
            return true
        }
        val membershipCard = getMembershipCard(sender)

        // Check args number
        if (args.size > 0) {
            sender.sendMessage("${RED}This command takes no arguments")
            return false
        }

        sender.sendMessage("${GREEN}Your buy goal is ${WHITE}${membershipCard.buyGoal}")
        return true
    }
    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return listOf<String>()
    }
}
// endregion


/**
 * Gets or changes the value of whether a player is "Just Looking" so that they don't actually buy/sell
 * anything, but can see the prices based on how many they're buying or selling.
 */
object JustLookingCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }

        // Check args number
        if (args.size == 0) {
            // TODO: Rework message? the usage from false might be good enough tho
            // sender.sendMessage(
            //         "${RED}You must specify whether you want to be just looking or not"
            // )
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = getMembershipCard(sender)

        // Get argument
        val status = when(args[0]) {
            "true" -> true
            "false" -> false
            "get" -> membershipCard.justLooking
            "toggle" -> !membershipCard.justLooking
            else -> { return false }
        }

        membershipCard.justLooking = status
        if (membershipCard.justLooking) {
            sender.sendMessage(
                    "You are ${if (args[0] == "get") "now" else "currently" } ${GREEN}just looking${WHITE} and will not buy or sell anything"
            )
        } else {
            sender.sendMessage(
                    "You are ${RED}${if (args[0] == "get") "not" else "no longer" }${WHITE} just looking, and will buy/sell items as you normally would"
            )
        }
        return true
    }
    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return if (args.size == 1) listOf<String>("true", "false", "get", "toggle") else listOf<String>()
    }
}


/**
 * Gets or changes a player's ability to set a particular sign to be a buy sign, allowing players to buy
 * the corresponding item from it.
 */
object OrdainCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${RED}You have to be a player to use this command.")
            return true
        }
        
        // ? pretty sure this isnt necessary
        // If sender does not have permission
        // if (!sender.hasPermission("fantasycostco.ordain-sign")) {
        //     sender.sendMessage("${RED}You don't have permission to use this command.")
        //     return true
        // }

        // Check args number
        if (args.size == 0) {
            // TODO: Rework message? the usage from false might be good enough tho
            // sender.sendMessage(
            //         "${RED}You must specify whether you want to be ordaining signs or not"
            // )
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = getMembershipCard(sender)

        // Get argument
        val status = when(args[0]) {
            "true" -> true
            "false" -> false
            "get" -> membershipCard.ordainingSign
            "toggle" -> !membershipCard.ordainingSign
            else -> { return false }
        }

        membershipCard.ordainingSign = status
        if (membershipCard.ordainingSign) {
            sender.sendMessage(
                    "You are ${if (args[0] == "get") "now" else "currently" } ${GREEN}ordaining signs${WHITE} and will set them to be buy signs"
            )
        } else {
            sender.sendMessage(
                    "You are ${RED}${if (args[0] == "get") "not" else "no longer" }${WHITE} ordaining signs, and will not set them to be buy signs"
            )
        }
        return true
    }
    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        return if (args.size == 1) listOf<String>("true", "false", "get", "toggle") else listOf<String>()
    }
}