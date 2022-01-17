package games.skweekychair.fantasycostco

import org.bukkit.command.TabExecutor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

object SellCommand : TabExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, lbl: String, args: Array<String>): Boolean {return false}

    override fun onTabComplete(sender: CommandSender, cmd: Command, lbl: String, args: Array<String>): List<String> {return listOf<String>()}
}

object BuyCommand : TabExecutor {

    override fun onCommand(sender: CommandSender, cmd: Command, lbl: String, args: Array<String>): Boolean {return false}

    override fun onTabComplete(sender: CommandSender, cmd: Command, lbl: String, args: Array<String>): List<String> {return listOf<String>()}
}