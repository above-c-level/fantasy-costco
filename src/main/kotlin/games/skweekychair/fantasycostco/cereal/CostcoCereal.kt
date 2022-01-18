package games.skweekychair.fantasycostco.cereal

import games.skweekychair.fantasycostco.BaseMerchandise
import games.skweekychair.fantasycostco.Merchandise
import java.io.File
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Bukkit

var wallets = HashMap<String, Double>()
var merch = HashMap<BaseMerchandise, Merchandise>()

var dataPath = File("wallets.json") // CostcoPlugin.instance.getDataPath

// TODO: *Ideally*, return type should be HashMap<UUID, Double>
fun saveWallets(wallets: HashMap<String, Double>) {
    val jsonString = Json.encodeToString(wallets)
    dataPath.bufferedWriter().use { out -> out.write(jsonString) }
}

fun loadWallets(): HashMap<String, Double> {
    val readFile = dataPath.bufferedReader().readText()
    return Json.decodeFromString(readFile)
}

fun saveAll() {
    // TODO: Add merchandise saving here also
    val logger = Bukkit.getServer().getLogger()
    logger.info("[FantasyCostco] Saving wallets")
    saveWallets(wallets)
}
