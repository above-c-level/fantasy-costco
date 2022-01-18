package games.skweekychair.fantasycostco

import java.io.File
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.bukkit.Bukkit



object Cereal {
    var wallets = HashMap<String, Double>()
    var merch = HashMap<BaseMerchandise, Merchandise>()
    var dataPath = File("wallets.json")

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
}

object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}
