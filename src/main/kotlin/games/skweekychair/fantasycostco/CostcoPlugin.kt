package games.skweekychair.fantasycostco

import org.bukkit.plugin.java.JavaPlugin

var wallets = HashMap<String, Double>()

class CostcoPlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        var config = getConfig()
        CostcoGlobals.spigotConfig = config

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)
    }
}
