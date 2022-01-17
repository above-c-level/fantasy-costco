package games.skweekychair.fantasycostco

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

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
        val player = sender

        val item = player.inventory.itemInMainHand
        val merchandise = Merchandise(item.type, CostcoGlobals.startingMass, 5.0)
        sender.sendMessage("The sell price is ${merchandise.itemSellPrice(item.amount)}")
        for (i in 1..10) {
            merchandise.hold()
        }
        sender.sendMessage("The sell price is ${merchandise.itemSellPrice(item.amount)}")

        walletAdd(player, 1.9)
        saveWallets(wallets)

        sender.sendMessage("${loadWallets()}")

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
        return false
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
