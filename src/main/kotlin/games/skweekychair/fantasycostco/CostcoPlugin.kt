package games.skweekychair.fantasycostco

import org.bukkit.plugin.java.JavaPlugin


class CostcoPlugin : JavaPlugin() {

    companion object {
        var instance: CostcoPlugin? = null
        private set;
    }


    override fun onEnable() {
        saveDefaultConfig()
        var config = getConfig()
        CostcoGlobals.spigotConfig = config

        getCommand("buy").executor = BuyCommand
        getCommand("sell").executor = SellCommand

        instance = this
    }

}
