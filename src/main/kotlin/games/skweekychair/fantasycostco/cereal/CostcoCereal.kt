@file:UseSerializers(EnchantmentSerializer::class, LocationSerializer::class)

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
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment

/**
 * A bowl.
 *
 * ...which happens to be an API for serialization so that other files can more easily serialize and
 * deserialize objects used in the plugin.
 */
object Cereal {
    var wallets = HashMap<UUID, Double>()
    var merch = HashMap<BaseMerchandise, Merchandise>()
    var walletPath = File("wallets.json")
    var merchPath = File("merch.json")

    val walletsSerializer: KSerializer<Map<UUID, Double>> =
            MapSerializer(UuidSerializer, Double.serializer())

    /** Saves all user wallets to a file. */
    fun saveWallets() {
        val jsonString = Json.encodeToString(walletsSerializer, wallets)
        walletPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    /**
     * Loads all user wallets from a file.
     * @return A map of player UUIDs to the amount contained in their wallets.
     */
    fun loadWallets(): HashMap<UUID, Double> {
        val readFile = walletPath.bufferedReader().readText()
        return HashMap(Json.decodeFromString(walletsSerializer, readFile))
    }

    /** Saves all merch to a file. */
    fun saveMerch() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonString = json.encodeToString(merch)
        merchPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    /**
     * Loads all merch from a file.
     * @return A map of merch to the amount contained in their inventories.
     */
    fun loadMerch(): HashMap<BaseMerchandise, Merchandise> {
        val json = Json { allowStructuredMapKeys = true }
        val readFile = merchPath.bufferedReader().readText()
        return HashMap(json.decodeFromString<Map<BaseMerchandise, Merchandise>>(readFile))
    }

    /** Saves wallets and merch simultaneously. */
    fun saveAll() {
        val logger = Bukkit.getServer().getLogger()
        logger.info("[FantasyCostco] Saving wallets")
        saveWallets()
        logger.info("[FantasyCostco] Saving Merch")
        saveMerch()
    }
    /** Loads wallets and merch simultaneously. */
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

/**
 * A serializer for UUIDs. This is necessary Kotlin requires a description of the way in which the
 * UUIDs used in our HashMaps should be serialized.
 */
object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    /**
     * Serializes a UUID to a string.
     * @param encoder The encoder to use.
     * @param value The UUID to serialize.
     */
    override fun serialize(encoder: Encoder, value: UUID) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    /**
     * Deserializes a UUID from a string.
     * @param decoder The decoder to use.
     * @return The UUID.
     */
    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

/**
 * A serializer for Enchantments. This is necessary Kotlin requires a description of the way in
 * which an Enchantment should be serialized.
 */
object EnchantmentSerializer : KSerializer<Enchantment> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Enchantment", PrimitiveKind.STRING)

    /**
     * Serializes an Enchantment to a string.
     * @param encoder The encoder to use.
     * @param value The Enchantment to serialize.
     */
    override fun serialize(encoder: Encoder, value: Enchantment) {
        val string = value.key.toString()
        encoder.encodeString(string)
    }

    /**
     * Deserializes an Enchantment from a string.
     * @param decoder The decoder to use.
     * @return The Enchantment.
     */
    override fun deserialize(decoder: Decoder): Enchantment {
        // TODO: Does this cause an uh-oh? a fucky wucky? We'll have to see!
        return Enchantment.getByKey(NamespacedKey.fromString(decoder.decodeString()))!!
    }
}

/**
 * A serializer for Locations. This is necessary Kotlin requires a description of the way in which
 * an Location should be serialized.
 */
object LocationSerializer : KSerializer<Location> {
    // Serializes the Location's x, y, and z to double and world to String
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("Location", PrimitiveKind.STRING)

    /**
     * Serializes a Location to a string.
     * @param encoder The encoder to use.
     * @param value The Location to serialize.
     */
    override fun serialize(encoder: Encoder, value: Location) {
        encoder.encodeString("${value.world?.name},${value.x},${value.y},${value.z}")
    }

    /**
     * Deserializes a Location from a string.
     * @param decoder The decoder to use.
     * @return The Location.
     */
    override fun deserialize(decoder: Decoder): Location {
        val string = decoder.decodeString()
        val split = string.split(",")
        val world = Bukkit.getWorld(split[0])
        val x = split[1].toDouble()
        val y = split[2].toDouble()
        val z = split[3].toDouble()
        return Location(world, x, y, z)
    }
}
