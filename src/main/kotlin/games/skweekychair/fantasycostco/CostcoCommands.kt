package games.skweekychair.fantasycostco

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import org.bukkit.Material
import org.bukkit.inventory.ItemStack


/**
 * Implements the /sell command for the plugin.
 */
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
        val merchandise = getMerchandise(item)

        if (merchandise.itemSellPrice(item.amount).isNaN()) {
            player.sendMessage("Don't sell air man!")
            return true;
        }
        player.sendMessage("The sell price is ${merchandise.itemSellPrice(item.amount)}")

        walletAdd(player, merchandise.itemSellPrice(item.amount))
        player.sendMessage("${Cereal.wallets[player.uniqueId]}")
        player.inventory.setItemInMainHand(null);
        merchandise.sell()


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
        }


        if (args.size == 1) {
            sender.sendMessage("${ChatColor.RED}You must specify how many of the item to buy.")
            return false
        }

        val material = Material.matchMaterial(args[0])

        if (material == null) {
            sender.sendMessage("${ChatColor.RED}Not an item.")
            return false
        }

        val amount = args[1].toIntOrNull()

        if (amount == null) {
            sender.sendMessage("${ChatColor.RED}Not an amount.")
            return false
        }

        if (amount < 0) {
            sender.sendMessage("${ChatColor.RED}I can't give you negative items dude :/")
            return false
        } else if (amount >= material.maxStackSize) {
            sender.sendMessage("${ChatColor.RED}You asked for more than stack size.")
            return false
        }

        val merchandise = getMerchandise(material)

        if (merchandise.itemBuyPrice(amount) > Cereal.wallets[player.uniqueId]) {

        }

        val itemStack = ItemStack(material, amount)

        val item = player.inventory.itemInMainHand

        if (item.type != Material.AIR) {
            sender.sendMessage("${ChatColor.RED}I don't want to be mean and overwrite one of you items.")
            return false
        }

        player.inventory.itemInMainHand(itemStack)

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
        player.sendMessage("${Cereal.wallets[player.uniqueId]}")
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