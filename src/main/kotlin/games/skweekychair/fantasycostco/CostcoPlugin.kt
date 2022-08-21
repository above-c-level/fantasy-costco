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

/** The main class of the plugin. */
class CostcoPlugin : JavaPlugin() {
    /** Called when the plugin is enabled by the server. */
    override fun onEnable() {
        Bukkit.getServer().getPluginManager().registerEvents(CostcoListener(), this)
        saveDefaultConfig()
        var config = getConfig()
        CostcoGlobals.spigotConfig = config

        Cereal.walletPath = File(getDataFolder(), "wallets.json")
        Cereal.merchPath = File(getDataFolder(), "merch.json")

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)
        getCommand("wallet")?.setExecutor(WalletCommand)
        getCommand("set-wallet")?.setExecutor(SetWalletCommand)
        getCommand("toggle-buy-possible")?.setExecutor(ToggleBuyPossibleCommand)

        Cereal.loadAll()

        val scheduler = Bukkit.getServer().getScheduler()
        scheduler.runTaskTimer(
                this,
                Perturber(),
                CostcoGlobals.secondsBetweenPriceMotion,
                CostcoGlobals.secondsBetweenPriceMotion
        )
    }

    /** Called when the plugin is disabled by the server. */
    override fun onDisable() {
        Cereal.saveAll()
        LogInfo("Shutting down :)")
    }
}

/** The listener for the plugin. */
class CostcoListener : Listener {
    // Listeners should help us do things such as save the wallet or current
    // material prices

    /**
     * Called when a player joins the server.
     *
     * @param event The event.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // LogInfo("Player joined")
    }

    /**
     * Called when a player interacts with a block.
     *
     * @param event The event.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {

        if (event.getHand() == EquipmentSlot.HAND && event.getAction() == Action.RIGHT_CLICK_BLOCK
        ) {
            val helditem = event.getMaterial().name
            // Block is null when right clicking air with an item
            val block = event.getClickedBlock()
            val blockname = block?.getType()?.name
            val player = event.getPlayer()
            player.sendMessage("You right-clicked on $blockname with $helditem")
            // If the block is some kind of sign
            if (block != null && block.getType().name.contains("SIGN")) {
                // choose a random merchandise
                val randomMerch = Cereal.merch.keys.random()
                // Add the sign's location to the merch
                val location = block.getLocation()
                AddSignToMerch(randomMerch, location)
                ClearSign(location)
                UpdateSign(
                        location,
                        mutableListOf(
                                Pair(0, randomMerch.material.name),
                                Pair(2, Cereal.merch[randomMerch]!!.shownPrice.toString())
                        )
                )
            }
            val blockdata = block?.blockData?.getAsString()
            player.sendMessage(blockdata)
        }
    }

    /**
     * Called when the server saves the world.
     *
     * @param event The event.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onWorldSave(event: WorldSaveEvent) {
        // Only save on the saving of overworld so that we don't save the data three times lol
        if (event.world.environment == World.Environment.NORMAL) {
            Cereal.saveAll()
        }
    }
}
