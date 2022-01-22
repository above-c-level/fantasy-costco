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
        val merchandise = getMerchandise(item)
        // sender.sendMessage("The sell price is ${merchandise.itemSellPrice(item.amount)}")

        walletAdd(player, 0.0)
        // saveWallets(wallets)
        // sender.sendMessage("${loadWallets()}")
        tryDiscordBroadcast("hmmmmm game lagging lol what can you do")
        tryDiscordBroadcast("hmmmmm game lagging lol what can you do")
        tryDiscordBroadcast("hmmmmm game lagging lol what can you do")
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
