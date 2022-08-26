package games.skweekychair.fantasycostco

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.io.IOUtil
import org.bukkit.Bukkit
import org.bukkit.ChatColor.*
import org.bukkit.Material
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
        Cereal.signsPath = File(getDataFolder(), "signs.json")

        getCommand("buy")?.setExecutor(BuyCommand)
        getCommand("sell")?.setExecutor(SellCommand)

        getCommand("get-wallet")?.setExecutor(GetWalletCommand)
        getCommand("set-wallet")?.setExecutor(SetWalletCommand)

        getCommand("buy-as-much-as-possible")?.setExecutor(BuyPossibleCommand)

        getCommand("amount")?.setExecutor(AmountCommand)

        getCommand("just-looking")?.setExecutor(JustLookingCommand)

        getCommand("ordain")?.setExecutor(OrdainCommand)
        getCommand("balance-top")?.setExecutor(BalanceTopCommand)

        getCommand("pay")?.setExecutor(PayCommand)

        Cereal.loadAll()

        val scheduler = Bukkit.getServer().getScheduler()
        scheduler.runTaskTimer(
                this,
                Perturber(),
                CostcoGlobals.secondsBetweenPriceMotion,
                CostcoGlobals.secondsBetweenPriceMotion
        )
        StartupSplashArt()
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
        if (event.getHand() != EquipmentSlot.HAND) {
            return
        }
        val player = event.getPlayer()
        val membershipCard = getMembershipCard(player)
        val ordainingSign = membershipCard.ordainingSign
        val block = event.getClickedBlock()

        // If the block in question is air, do nothing
        if (block == null) {
            return
        }
        // If the block isn't a sign, do nothing
        val blockname = block.getType().name
        if (!blockname.contains("SIGN")) {
            return
        }

        val signLocation = block.location
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && ordainingSign) {
            // Destroy the sign if it's registered

            if (signLocation in Cereal.purchasePoints) {
                SignUtils.removeSignFromMerch(signLocation)
            } else {
                // removeSignFromMerch does the same thing, but only if the sign is registered
                // as a purchase point, so this'll take care of stragglers
                SignUtils.removeSignData(signLocation)
            }
            block.setType(Material.AIR, true)
            return
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val itemStack = event.item
        val heldItemName = event.getMaterial().name

        // First, see whether there is already a sign with SignData there

        val signData: SignData? = Cereal.signs[signLocation]

        val holdingAir = heldItemName == "AIR"
        val signFound = signData != null


        if (ordainingSign && holdingAir && signFound) {
            // Case 1
            if (ordainingSign && Cereal.merch.size == 0) {
                // If the player is ordaining and there are no merch, let them know
                player.sendMessage("${RED}There's no merch to sell")
                return
            }
            // If the sign is a sell sign
            if (signData!!.isSelling()) {
                // There is a sign there, so cycle through sell options
                signData.nextSellOption()
                SignUtils.updateSign(signLocation, false)
                return
            }
            // If the sign was a buy sign, it should be associated with a merch
            if (!SignUtils.removeSignFromMerch(signLocation)){
                SignUtils.removeSignData(signLocation)
            }
            // They're holding air, so it's a sell sign
            SignUtils.addSignData(signLocation, SignType.SELL_ONE)
            SignUtils.updateSign(signLocation, false)
        } else if (ordainingSign && holdingAir && !signFound) {
            // Case 2
            // There is not a sign there, so initialize it
            SignUtils.addSignData(signLocation, SignType.SELL_ONE)
            SignUtils.updateSign(signLocation, false)
        } else if (ordainingSign && !holdingAir && signFound) {
            // Case 3
            val baseMerch = BaseMerchandise(itemStack!!.type, itemStack.enchantments)
            // If the sign is a buy sign
            if (signData!!.isBuying()) {
                // There is a sign there, so cycle through sell options
                signData.nextBuyOption()
                SignUtils.updateSign(signLocation, merch = getMerchandise(baseMerch))
                return
            }

            // If the sign is already ordained *but* the player is trying to ordain
            // it as a buy sign and it was a sell sign, then remove the sign and ordain
            // it as a buy sign
            SignUtils.removeSignData(signLocation)
            // They're holding an item, so it's a buy sign

            SignUtils.addSignToMerch(baseMerch, signLocation, SignType.BUY_ONE)
            SignUtils.updateSign(signLocation, merch = getMerchandise(baseMerch))
        } else if (ordainingSign && !holdingAir && !signFound) {
            // Case 4
            // There is not a sign there, so initialize it
            val baseMerch = BaseMerchandise(itemStack!!.type, itemStack.enchantments)
            SignUtils.addSignToMerch(baseMerch, signLocation, SignType.BUY_ONE)
            SignUtils.updateSign(signLocation, merch = getMerchandise(baseMerch))
        } else if (!ordainingSign && signFound) {
            logIfDebug("Case 5")
            // There is a sign there, so attempt to buy or sell
            // TODO: buy/sell
            player.sendMessage("Pretend you're buying from the sign you clicked on")
            // Player is right clicking with an item
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
