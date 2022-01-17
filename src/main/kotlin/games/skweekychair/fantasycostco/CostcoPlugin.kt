package games.skweekychair.fantasycostco

import org.bukkit.Bukkit
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
}

class CostcoListener : Listener {
    // Listeners should help us do things such as save the wallet or current
    // material prices
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.broadcastMessage("PlayerJoinEvent successfully listened to")
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onWorldSave(event: WorldSaveEvent) {
        Bukkit.broadcastMessage("WorldSaveEvent successfully listened to")
    }
}
