@file:UseSerializers(EnchantmentSerializer::class)

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
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment

object Cereal {
    var wallets = HashMap<UUID, Double>()
    var merch = HashMap<BaseMerchandise, Merchandise>()
    var dataPath = File("wallets.json")

    fun saveWallets() {
        val jsonString = Json.encodeToString<Map<UUID, Double>>(wallets)
        dataPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    fun loadWallets(): HashMap<UUID, Double> {
        val readFile = dataPath.bufferedReader().readText()
        return HashMap(Json.decodeFromString<Map<UUID, Double>>(readFile))
    }

    fun saveMerch() {
        val jsonString = Json.encodeToString(merch)
        dataPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    fun loadMerch(): HashMap<BaseMerchandise, Merchandise> {
        val readFile = dataPath.bufferedReader().readText()
        return HashMap(Json.decodeFromString<Map<BaseMerchandise, Merchandise>>(readFile))
    }

    fun saveAll() {
        // TODO: Add merchandise saving here also
        val logger = Bukkit.getServer().getLogger()
        logger.info("[FantasyCostco] Saving wallets")
        saveWallets()
        logger.info("[FantasyCostco] Saving Merch")
        saveMerch()
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

object EnchantmentSerializer : KSerializer<Enchantment> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Enchantment", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Enchantment) {
        val string = value.key.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Enchantment {
        // TODO: Does this cause an uh-oh? a fucky wucky? We'll have to see!
        return Enchantment.getByKey(NamespacedKey.fromString(decoder.decodeString()))!!
    }
}

// object MaterialSerializer : KSerializer<Material> {
//     override val descriptor: SerialDescriptor =
//             PrimitiveSerialDescriptor("Material", PrimitiveKind.STRING)







































//     override fun serialize(encoder: Encoder, value: Material) {
//         val string = value.name
//         encoder.encodeString(string)
//     }







































//     override fun deserialize(decoder: Decoder): Material {
//         // TODO: Fuck around and find out
//         return Material.getMaterial(decoder.decodeString())!!
//     }
// }
