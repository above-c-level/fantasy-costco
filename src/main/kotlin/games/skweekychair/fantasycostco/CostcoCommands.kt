package games.skweekychair.fantasycostco

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.StringUtil

/** Implements the /sell command for the plugin. */
object SellCommand : TabExecutor {

    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}You have to be a player to use this command.")
            return false
        }
        val player: Player = sender
        val item = player.inventory.itemInMainHand
        val itemCount = item.amount
        val material = item.type
        val merchandise = getMerchandise(item)

        if (merchandise.itemSellPrice(item.amount).isNaN()) {
            player.sendMessage("Don't sell air man!")
            return true
        }
        player.sendMessage("The sell price is ${merchandise.itemSellPrice(item.amount)}")

        walletAdd(player, merchandise.itemSellPrice(item.amount))
        player.sendMessage("${getWallet(player)}")
        player.inventory.setItemInMainHand(null)
        merchandise.sell(itemCount.toDouble())

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

object BuyCommand : TabExecutor {

    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}You have to be a player to use this command.")
            return true
        }
        val player: Player = sender

        if (args.size == 0) {
            sender.sendMessage("${ChatColor.RED}You must specify an item to buy.")
            return false
        } else if (args.size == 1) {
            sender.sendMessage("${ChatColor.RED}You must specify how many of the item to buy.")
            return false
        } else if (args.size > 2) {
            sender.sendMessage("${ChatColor.RED}This command only takes two arguments")
            return false
        }

        val material = Material.matchMaterial(args[0])

        if (material == null || !material.isItem) {
            sender.sendMessage("${ChatColor.RED}Not an item.")
            return false
        }

        var amount = args[1].toIntOrNull()

        if (amount == null) {
            sender.sendMessage("${ChatColor.RED}Not an amount.")
            return false
        }

        if (amount < 0) {
            sender.sendMessage("${ChatColor.RED}I can't give you negative items dude :/")
            return false
        } else if (amount == 0) {
            sender.sendMessage("${ChatColor.GREEN}Aight here you go")
            return true
        } else if (amount > 27 * material.maxStackSize) {
            sender.sendMessage("${ChatColor.RED}You can't buy that many items.")
            return false
        }
        // This below thing shouldn't strictly be necessary
        // else if (amount > material.maxStackSize) {
        //     sender.sendMessage("${ChatColor.RED}You asked for more than stack size.")
        //     return false
        // }
        val merchandise = getMerchandise(material)
        var price = merchandise.itemBuyPrice(amount)
        val playerFunds = getWallet(player)

        if (price > playerFunds) {
            val buyMaxItems = getBuyMaxItems(player)
            if (buyMaxItems) {
                // Since the price is nonlinear, we can do binary search to find the largest number
                // of items purchaseable.
                // Worst case scenario is the player wants a full inventory, so even if we increase
                // the amount a player can buy up to 36 stacks, this is guaranteed
                // to take <= 11 iterations,
                var low: Int = 1
                var high: Int = amount
                var iters: Int = 0
                while (low < high) {
                    iters++
                    val mid = (low + high) / 2
                    val midBuyPrice = merchandise.itemBuyPrice(mid)
                    if (midBuyPrice > playerFunds) {
                        high = mid - 1
                    } else {
                        low = mid + 1
                    }
                }
                amount = high - 1
                price = merchandise.itemBuyPrice(amount)
                if (amount <= 0) {
                    sender.sendMessage("${ChatColor.RED}You can't buy any more of ${material.name}.")
                    sender.sendMessage("${ChatColor.RED}You only have ${playerFunds}, and you need ${merchandise.itemBuyPrice(1)} for 1.")
                    return false
                }
            } else {
                sender.sendMessage("${ChatColor.RED}Honey, you ain't got the money fo' that.")
                sender.sendMessage(
                        "${ChatColor.RED}You only have ${playerFunds}, and you need ${price}."
                )
                return false
            }
        }

        val hiddenPrice = merchandise.hiddenPrice
        sender.sendMessage(
                "${ChatColor.GREEN}Hidden price of ${material.name} is for ${hiddenPrice}"
        )

        val itemStack = ItemStack(material, amount)

        // val item = player.inventory.itemInMainHand

        // if (item.type != Material.AIR) {
        //     sender.sendMessage(
        //             "${ChatColor.RED}I don't want to be mean and overwrite one of you items."
        //     )
        //     return false
        // }

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
        player.sendMessage("${ChatColor.GREEN}You bought ${amount} ${material.name} for ${price}")
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

object SetWalletCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        // set_wallet:
        //     description: Set how many Blockcoins a player has in their wallet
        //     usage: /<command> [player] [amount]
        //     permission: fantasycostco.set_wallet

        // If sender does not have permission, return false
        if (!sender.hasPermission("fantasycostco.set-wallet")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return false
        }
        // Make sure there are two arguments
        if (args.size == 0) {
            sender.sendMessage("${ChatColor.RED}You must specify a player to set their wallet")
            return false
        } else if (args.size == 1) {
            sender.sendMessage("${ChatColor.RED}You must specify the amount to set their wallet to")
            return false
        } else if (args.size > 2) {
            sender.sendMessage("${ChatColor.RED}This command only takes two arguments")
            return false
        }

        // Get player
        val playerArg: String = args[0]
        val player = Bukkit.getPlayer(playerArg)
        if (player == null) {
            sender.sendMessage("${ChatColor.RED}Player not found.")
            return false
        }
        // Get amount
        val amount = args[1].toDoubleOrNull()
        if (amount == null) {
            sender.sendMessage("${ChatColor.RED}Not a valid amount.")
            return false
        }
        // Set wallet with amount
        setWallet(player, amount)
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

object WalletCommand : TabExecutor {

    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}You have to be a player to use this command.")
            return false
        }
        val player: Player = sender
        player.sendMessage("${getWallet(player)}")
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

object ToggleBuyPossibleCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("${ChatColor.RED}You have to be a player to use this command.")
            return false
        }

        // If sender does not have permission, return false
        if (!sender.hasPermission("fantasycostco.toggle-buy-possible")) {
            sender.sendMessage("${ChatColor.RED}You don't have permission to use this command.")
            return false
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