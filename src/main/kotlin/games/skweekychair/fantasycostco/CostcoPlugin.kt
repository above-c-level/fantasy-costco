package games.skweekychair.fantasycostco

import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.plugin.java.JavaPlugin

var wallets = HashMap<String, Double>()

class CostcoPlugin : JavaPlugin() {

    override fun onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(CostcoListener(), this)
        saveDefaultConfig()
        var config = getConfig()
        CostcoGlobals.spigotConfig = config

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)
    }

    override fun onDisable() {
        saveAll()
        Bukkit.getServer().getLogger().info("[FantasyCostco] Shutting down :)")
    }
}

class CostcoListener : Listener {
    // Listeners should help us do things such as save the wallet or current
    // material prices

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getServer().getLogger().info("[FantasyCostco] Player joined")
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onWorldSave(event: WorldSaveEvent) {
        // Only save on the saving of overworld so that we don't save the data three times lol
        if (event.world.environment == World.Environment.NORMAL) {
            saveAll()
        }
    }
}
