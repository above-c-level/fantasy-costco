package games.skweekychair.fantasycostco

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.io.IOUtil
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
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
        CostcoGlobals.merchPricesConfig = getResourcesConfig("merchprices")
        CostcoGlobals.fixedPricesConfig = getResourcesConfig("fixedprices")
        CostcoGlobals.enchantmentPricesConfig = getResourcesConfig("enchantmentprices")
        CostcoGlobals.notAcceptedConfig = getResourcesConfig("notaccepted")

        Cereal.walletPath = File(getDataFolder(), "wallets.json")
        Cereal.merchPath = File(getDataFolder(), "merch.json")

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)

        getCommand("get-wallet")?.setExecutor(GetWalletCommand)
        getCommand("set-wallet")?.setExecutor(SetWalletCommand)

        getCommand("buy-as-much-as-possible")?.setExecutor(BuyPossibleCommand)

        getCommand("amount")?.setExecutor(AmountCommand)

        getCommand("just-looking")?.setExecutor(JustLookingCommand)

        getCommand("ordain")?.setExecutor(OrdainCommand)
        getCommand("balance-top")?.setExecutor(BalanceTopCommand)

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
    /**
     * Gets the configuration file for the given resource.
     * @param resourceName The name of the resource without the extension.
     * @return The configuration file.
     */
    fun getResourcesConfig(name: String): FileConfiguration {
        // val configFile = File("plugins/KingDefence", "messages.yml")
        // ConfigCfg = YamlConfiguration.loadConfiguration(ConfigFile)

        val input = this.javaClass.getResourceAsStream("/" + name + ".yml")
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile(name, ".yml")
            tempFile.deleteOnExit()
            val out: FileOutputStream = FileOutputStream(tempFile)
            IOUtil.copy(input, out)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (tempFile == null) {
            throw RuntimeException("Could not create temporary file.")
        }

        return YamlConfiguration.loadConfiguration(tempFile)
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
        ensureWallet(event.player)
    }

    /**
     * Called when a player interacts with a block.
     *
     * @param event The event.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK
        ) {
            return
        }
        val block = event.getClickedBlock()

        // If the player is not right clicking on a block, do nothing
        if (block == null) {
            return
        }
        // If the player is not right clicking on a sign, do nothing
        val blockname = block.getType().name
        if (!blockname.contains("SIGN")) {
            return
        }
        val player = event.getPlayer()
        val membershipCard = getMembershipCard(player)
        val itemStack = event.item
        val helditemName = event.getMaterial().name

        // First, see whether there is already a sign with SignData there
        val signLocation = block.location
        val signData: SignData? = Cereal.signs[signLocation]
        // The player is not ordaining the sign, so deal with buying/selling
        if (!membershipCard.ordainingSign) {
            // Player is right clicking without an item, check if buying
            if (helditemName == "AIR") {
                if (signData == null) {
                    // This is not a costco sign there, so return
                    return
                }
                // There is a sign there, so attempt to buy
                // TODO: buy
                player.sendMessage("Pretend you're buying the sign you clicked on")
                // Player is right clicking with an item
            } else {
                if (signData == null) {
                    // There is not a costco sign there, so return
                    return
                }
                // There is a sign there, so attempt to sell
                // TODO: sell
                player.sendMessage("Pretend you're selling stuff")

            }
            // TODO: do the sign stuff for make better sale time and buy item for player :)
            // The player is ordaining signs
        } else {
            // This is supposed to be a sell sign
            if (helditemName == "AIR") {
                if (signData == null) {
                    // There is not a sign there, so initialize it
                    AddSignData(signLocation, SignType.SELL_ONE)
                } else {
                    // There is a sign there, so cycle through sell options
                    signData.nextSellOption()
                }
                // This is supposed to be a buy sign
            } else {
                if (signData == null) {
                    // There is not a sign there, so initialize it
                    if (itemStack == null) {
                        LogWarning(
                                "Null item stack for player ${player.name} while ordaining a buy sign"
                        )
                        return
                    }
                    val baseMerch = BaseMerchandise(itemStack.type, itemStack.enchantments)
                    AddSignToMerch(baseMerch, signLocation, SignType.BUY_ONE)
                } else {
                    // There is a sign there, so cycle through sell options
                    signData.nextBuyOption()
                }
            }
        }
        val blockdata = block?.blockData?.getAsString()
        player.sendMessage(blockdata)
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
