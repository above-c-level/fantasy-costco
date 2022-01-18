package games.skweekychair.fantasycostco

import java.io.File
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.WorldSaveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin



class CostcoPlugin : JavaPlugin() {
    override fun onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(CostcoListener(), this)
        saveDefaultConfig()
        var config = getConfig()
        CostcoGlobals.spigotConfig = config

        dataPath = File(getDataFolder(), "wallets.json")

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)

        val scheduler = Bukkit.getServer().getScheduler()
        scheduler.runTaskTimer(
                this,
                Perturber(),
                CostcoGlobals.secondsBetweenPriceMotion,
                CostcoGlobals.secondsBetweenPriceMotion
        )
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
    fun onPlayerInteract(event: PlayerInteractEvent) {

        if (event.getHand() == EquipmentSlot.HAND && event.getAction() == Action.RIGHT_CLICK_BLOCK
        ) {
            val helditem = event.getMaterial().name
            // Block is null when right clicking air with an item
            val block = event.getClickedBlock()?.getType()?.name
            event.getPlayer().sendMessage("You right-clicked on $block with $helditem")
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onWorldSave(event: WorldSaveEvent) {
        // Only save on the saving of overworld so that we don't save the data three times lol
        if (event.world.environment == World.Environment.NORMAL) {
            saveAll()
        }
    }
}
