package games.skweekychair.fantasycostco

import java.util.PriorityQueue
import java.util.UUID
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
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
            amount = MemberUtils.getMembershipCard(player).buyGoal
        }
        if (amount == null) {
            sender.sendMessage("${RED}Not an amount.")
            return false
        }

        val merchandise = MerchUtils.getMerchandise(material)
        val membershipCard = MemberUtils.getMembershipCard(player)
        if (membershipCard.justLooking) {
            BuyUtils.handleJustLooking(player, merchandise, amount)
            return true
        }
        // Deal with actually purchasing items
        BuyUtils.handleBuyAmount(player, merchandise, amount)
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

        // Check arguments
        if (args.size > 0) {
            "${RED}This command doesn't take any arguments"
            return false
        }
        val player: Player = sender
        val membershipCard = MemberUtils.getMembershipCard(player)
        if (membershipCard.justLooking) {
            SellUtils.handleJustLookingStack(player)
            return true
        }
        SellUtils.handleSellStack(player)
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
        player.sendMessage("${GREEN}You have ${WHITE}${MemberUtils.getWalletString(player)}")
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
        MemberUtils.setWallet(player, amount)
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
 * Gets or changes whether a player will buy as many items as possible if they can't afford the
 * requested amount
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
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = MemberUtils.getMembershipCard(sender)

        // Get argument
        val status =
                when (args[0]) {
                    "true" -> true
                    "false" -> false
                    "get" -> membershipCard.buyMaxItems
                    "t" -> true
                    "f" -> false
                    else -> {
                        return false
                    }
                }

        membershipCard.buyMaxItems = status
        if (membershipCard.buyMaxItems) {
            sender.sendMessage(
                    "You ${if (args[0] == "get") "currently" else "now" } ${GREEN}will${WHITE} buy as many items as you can afford if you don't have enough money on /buy calls"
            )
        } else {
            sender.sendMessage(
                    "You ${if (args[0] == "get") "currently" else "now" } ${RED}will not${WHITE} buy as many items as you can afford if you don't have enough money on /buy calls"
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
        return if (args.size == 1) listOf<String>("true", "false", "get") else listOf<String>()
    }
}

// endregion

/** Allows a player to set or get how many items they want to buy */
object AmountCommand : TabExecutor {
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

        // Make sure there's an argument to set the buy amount to
        if (args.size == 0) {
            sender.sendMessage(
                    "${RED}You must specify the amount you want to buy or that you want to get your current goal"
            )
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = MemberUtils.getMembershipCard(sender)

        // Make sure argument is an int
        var amount = args[0].toIntOrNull()

        if (args[0] == "get") {
            amount = membershipCard.buyGoal
        } else if (amount == null) {
            sender.sendMessage("${RED}Not a valid amount.")
            return false
        }

        membershipCard.buyGoal = amount
        sender.sendMessage(
                "${GREEN}Your buy goal is ${if (args[0] == "get") "" else "now set to "}${WHITE}${amount}"
        )

        return true
    }

    override fun onTabComplete(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): List<String> {
        // TODO: Can we maybe add a different approach, like "get" and "10 items" and "12 stacks"?
        return if (args.size == 1) listOf<String>("get", "0..2304") else listOf<String>()
    }
}

object UseAmountCommand : TabExecutor {
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
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = MemberUtils.getMembershipCard(sender)

        // Get argument
        val status =
                when (args[0]) {
                    "true" -> true
                    "false" -> false
                    "get" -> membershipCard.useAmount
                    "t" -> true
                    "f" -> false
                    else -> {
                        return false
                    }
                }

        membershipCard.useAmount = status
        if (membershipCard.justLooking) {
            sender.sendMessage(
                    "You are ${if (args[0] == "get") "currently" else "now" } ${GREEN}using${WHITE} the amount you set to override sign amounts"
            )
        } else {
            sender.sendMessage(
                    "You are ${RED}${if (args[0] == "get") "not" else "no longer" }${WHITE}using the amount you set to override sign amounts"
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
        return if (args.size == 1) listOf<String>("true", "false", "get") else listOf<String>()
    }
}

/**
 * Gets or changes the value of whether a player is "Just Looking" so that they don't actually
 * buy/sell anything, but can see the prices based on how many they're buying or selling.
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
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = MemberUtils.getMembershipCard(sender)

        // Get argument
        val status =
                when (args[0]) {
                    "true" -> true
                    "false" -> false
                    "get" -> membershipCard.justLooking
                    "t" -> true
                    "f" -> false
                    else -> {
                        return false
                    }
                }

        membershipCard.justLooking = status
        if (membershipCard.justLooking) {
            sender.sendMessage(
                    "You are ${if (args[0] == "get") "currently" else "now" } ${GREEN}just looking${WHITE} and will not buy or sell anything"
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
        return if (args.size == 1) listOf<String>("true", "false", "get") else listOf<String>()
    }
}

/**
 * Gets or changes a player's ability to set a particular sign to be a buy sign, allowing players to
 * buy the corresponding item from it.
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

        // Check args number
        if (args.size == 0) {
            return false
        } else if (args.size > 1) {
            sender.sendMessage("${RED}This command only takes one argument")
            return false
        }

        val membershipCard = MemberUtils.getMembershipCard(sender)

        // Get argument
        val status =
                when (args[0]) {
                    "true" -> true
                    "false" -> false
                    "get" -> membershipCard.ordainingSign
                    "t" -> true
                    "f" -> false
                    else -> {
                        return false
                    }
                }

        membershipCard.ordainingSign = status
        if (membershipCard.ordainingSign) {
            sender.sendMessage(
                    "You are ${if (args[0] == "get") "currently" else "now" } ${GREEN}ordaining signs${WHITE} and will set them to be buy signs"
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
        return if (args.size == 1) listOf<String>("true", "false", "get") else listOf<String>()
    }
}

object BalanceTopCommand : TabExecutor {
    override fun onCommand(
            sender: CommandSender,
            cmd: Command,
            lbl: String,
            args: Array<String>
    ): Boolean {

        if (args.size > 1) {
            sender.sendMessage("${RED}This command takes at most one argument")
            return false
        }

        var page = 1

        if (args.size == 1) {
            val arg = args[0].toIntOrNull()
            if (arg == null) {
                sender.sendMessage("${RED}Not a valid page number")
                return false
            }
            page = arg
        }

        val pq =
                PriorityQueue<Pair<UUID, MembershipCard>>({ a, b ->
                    val subtracted = a.second.balance - b.second.balance
                    if (subtracted < 0) {
                        -1
                    } else if (subtracted > 0) {
                        1
                    } else {
                        0
                    }
                })

        for ((uuid, membershipCard) in Cereal.wallets) {
            pq.add(Pair(uuid, membershipCard))
        }

        val itemsPerPage = 5
        val offset = (page - 1) * itemsPerPage
        val end = offset + (itemsPerPage - 1)

        for (place in offset..end) {
            val card = pq.poll()
            if (card == null) {
                sender.sendMessage("End of leaderboard.")
                break
            }
            sender.sendMessage(
                    "${place+1}. ${Bukkit.getOfflinePlayer(card.first).name}: ${MemberUtils.roundDoubleString(card.second.balance)}"
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
        return listOf()
    }
}

object PayCommand : TabExecutor {
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

        if (args.size != 2) {
            sender.sendMessage(
                    "${RED}This command takes two arguments, the name of a player to pay and an amount to pay them"
            )
            return false
        }

        val payer: Player = sender
        val payee = Bukkit.getPlayer(args[0])

        if (payee == null) {
            sender.sendMessage(
                    "${RED}The named player could not be found. (Are they online? Is it spelled correctly?)"
            )
            return false
        }

        // In case the player tab completes to have the blockcoin symbol
        var argstring = args[1]
        if (argstring.startsWith("₿")) {
            // Strip out prefix
            argstring = argstring.substring(1)
        }
        var amount = argstring.toDoubleOrNull()

        if (amount == null) {
            sender.sendMessage("${RED}Not a valid amount")
            return false
        }
        amount = MemberUtils.roundDouble(amount)

        if (MemberUtils.getWallet(payer) < amount) {
            sender.sendMessage("${RED}You can't afford to send that much.")
            sender.sendMessage(
                    "${RED}You have ${WHITE}${MemberUtils.getWalletRounded(payer)}${RED} and tried to send ${WHITE}${MemberUtils.roundDoubleString(amount)}"
            )
            // returning true cuz they know how to use the command
            return true
        }

        MemberUtils.walletSubtract(payer, amount)
        MemberUtils.walletAdd(payee, amount)

        sender.sendMessage(
                "${GREEN}You paid ${WHITE}${MemberUtils.roundDoubleString(amount)}${GREEN} to ${payee.name}"
        )
        payee.sendMessage(
                "${GREEN}You were paid ${WHITE}${MemberUtils.roundDoubleString(amount)}${GREEN} by ${payer.name}"
        )

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
            val items = Bukkit.getOnlinePlayers().map { it.name }
            StringUtil.copyPartialMatches(args[0], items, completions)
        } else if (args.size == 2) {
            completions.add("₿")
        }
        completions.sort()
        return completions
    }
}
