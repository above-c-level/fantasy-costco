package games.skweekychair.fantasycostco

import org.bukkit.plugin.java.JavaPlugin

class CostcoPlugin : JavaPlugin() {

    override fun onEnable() {
        saveDefaultConfig()
        CostcoGlobals.spigotConfig = getConfig()
    }

}
