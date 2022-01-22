@file:UseSerializers(EnchantmentSerializer::class)

package games.skweekychair.fantasycostco

import java.io.File
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
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
    var walletPath = File("wallets.json")
    var merchPath = File("merch.json")

    val walletsSerializer: KSerializer<Map<UUID, Double>> =
            MapSerializer(UuidSerializer, Double.serializer())

    fun saveWallets() {
        val jsonString = Json.encodeToString(walletsSerializer, wallets)
        walletPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    fun loadWallets(): HashMap<UUID, Double> {
        val readFile = walletPath.bufferedReader().readText()
        return HashMap(Json.decodeFromString(walletsSerializer, readFile))
    }

    fun saveMerch() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonString = json.encodeToString(merch)
        merchPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    fun loadMerch(): HashMap<BaseMerchandise, Merchandise> {
        val json = Json { allowStructuredMapKeys = true }
        val readFile = merchPath.bufferedReader().readText()
        return HashMap(json.decodeFromString<Map<BaseMerchandise, Merchandise>>(readFile))
    }

    fun saveAll() {
        val logger = Bukkit.getServer().getLogger()
        logger.info("[FantasyCostco] Saving wallets")
        saveWallets()
        logger.info("[FantasyCostco] Saving Merch")
        saveMerch()
    }
    fun loadAll() {
        val logger = Bukkit.getServer().getLogger()
        try {
            wallets = loadWallets()
        } catch (e: Throwable) {
            logger.warning("[FantasyCostco] Failed to load wallets: ${e.message}")
            wallets = HashMap<UUID, Double>()
        }
        try {
            merch = loadMerch()
        } catch (e: Throwable) {
            logger.warning("[FantasyCostco] Failed to load merch: ${e.message}")
            merch = HashMap<BaseMerchandise, Merchandise>()
        }
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