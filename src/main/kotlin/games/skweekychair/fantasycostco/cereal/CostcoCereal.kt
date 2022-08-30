@file:UseSerializers(EnchantmentSerializer::class, LocationSerializer::class)

package games.skweekychair.fantasycostco

import java.io.File
import java.util.UUID
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
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
    var wallets = HashMap<UUID, MembershipCard>()
    var merch = HashMap<BaseMerchandise, Merchandise>()
    var purchasePoints = HashMap<Location, BaseMerchandise>()
    var signs = HashMap<Location, SignData>()
    var walletPath = File("wallets.json")
    var merchPath = File("merch.json")
    var signsPath = File("signs.json")

    val walletsSerializer: KSerializer<Map<UUID, MembershipCard>> =
            MapSerializer(UuidSerializer, MembershipCardSerializer)

    val signsSerializer: KSerializer<Map<Location, SignData>> =
            MapSerializer(LocationSerializer, SignDataSerializer)

    /** Saves all user wallets to a file. */
    fun saveWallets() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonString = json.encodeToString(walletsSerializer, wallets)
        walletPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    /** Saves all merch to a file. */
    fun saveMerch() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonString = json.encodeToString(merch)
        merchPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    /** Saves all signs to a file so we can get sell points */
    fun saveSigns() {
        val json = Json { allowStructuredMapKeys = true }
        val jsonString = json.encodeToString(signsSerializer, signs)
        signsPath.bufferedWriter().use { out -> out.write(jsonString) }
    }

    /**
     * Loads all user wallets from a file.
     * @return A map of player UUIDs to the amount contained in their wallets.
     */
    fun loadWallets(): HashMap<UUID, MembershipCard> {
        val readFile = walletPath.bufferedReader().readText()
        val json = Json { allowStructuredMapKeys = true }
        return HashMap(
                json.decodeFromString<Map<UUID, MembershipCard>>(walletsSerializer, readFile)
        )
    }

    /**
     * Loads all merch from a file.
     * @return A map of merch to the amount contained in their inventories.
     */
    fun loadMerch(): HashMap<BaseMerchandise, Merchandise> {
        val readFile = merchPath.bufferedReader().readText()
        val json = Json { allowStructuredMapKeys = true }
        return HashMap(json.decodeFromString<Map<BaseMerchandise, Merchandise>>(readFile))
    }

    /** Loads all signs from a file. */
    fun loadSigns(): HashMap<Location, SignData> {
        val readFile = signsPath.bufferedReader().readText()
        val json = Json { allowStructuredMapKeys = true }
        return HashMap(json.decodeFromString<Map<Location, SignData>>(signsSerializer, readFile))
    }

    /** Saves all serialized json data */
    fun saveAll() {
        saveWallets()
        saveMerch()
        saveSigns()
    }

    /** Loads all serialized json data */
    fun loadAll() {
        try {
            wallets = loadWallets()
        } catch (e: Throwable) {
            LogWarning("Failed to load wallets: ${e.message}")
            wallets = HashMap<UUID, MembershipCard>()
        }
        try {
            merch = loadMerch()
        } catch (e: Throwable) {
            LogWarning("Failed to load merch: ${e.message}")
            merch = HashMap<BaseMerchandise, Merchandise>()
        }
        try {
            // Load purchase points from merch as well to map Location to BaseMerchandise
            for (baseMerch in merch) {
                val merchVal: Merchandise = merch[baseMerch.key]!!
                val signList: MutableList<Location> = merchVal.listOfSigns
                for (signLocation in signList) {
                    purchasePoints[signLocation] =
                            BaseMerchandise(merchVal.material, merchVal.enchantments)
                }
            }
        } catch (e: Throwable) {
            LogWarning("Failed to init purchase points: ${e.message}")
        }
        try {
            signs = loadSigns()
        } catch (e: Throwable) {
            LogWarning("Failed to load signs: ${e.message}")
            signs = HashMap<Location, SignData>()
        }
    }
}

/** A player class for holding player data. */
@Serializable
data class MembershipCard(
        // Doubles can represent as high as 140737488355328 (2^47) in increments of 0.01 with no
        // loss of precision.
        @SerialName("balance") var balance: Double,
        @SerialName("buyGoal") var buyGoal: Int = 0,
        @SerialName("buyMaxItems") var buyMaxItems: Boolean = false,
        @SerialName("justLooking") var justLooking: Boolean = false,
        @SerialName("ordainingSign") var ordainingSign: Boolean = false,
        @SerialName("useAmount") var useAmount: Boolean = false
)

object MembershipCardSerializer : KSerializer<MembershipCard> {
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("MembershipCard", PrimitiveKind.STRING)

    /**
     * Serializes MembershipCard to a string.
     * @param encoder The encoder to use.
     * @param value The UUID to serialize.
     */
    override fun serialize(encoder: Encoder, value: MembershipCard) {
        val json = Json { allowStructuredMapKeys = true }
        encoder.encodeString(json.encodeToString(value))
    }

    /**
     * Deserializes MembershipCard from a string.
     * @param decoder The decoder to use.
     * @return The MembershipCard.
     */
    override fun deserialize(decoder: Decoder): MembershipCard {
        val decodedString = decoder.decodeString()
        val json = Json { allowStructuredMapKeys = true }
        val jsonElement = json.decodeFromString<MembershipCard>(decodedString)
        return jsonElement
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

/**
 * A serializer for SignData. This is necessary Kotlin requires a description of the way in which a
 * SignData should be serialized.
 */
object SignDataSerializer : KSerializer<SignData> {
    // Serializes the SignData's text to a string
    override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("SignData", PrimitiveKind.STRING)
    /**
     * Serializes a SignData to a string.
     * @param encoder The encoder to use.
     * @param value The SignData to serialize.
     */
    override fun serialize(encoder: Encoder, value: SignData) {
        val json = Json { allowStructuredMapKeys = true }
        encoder.encodeString(json.encodeToString(value))
    }
    /**
     * Deserializes a SignData from a string.
     * @param decoder The decoder to use.
     * @return The SignData.
     */
    override fun deserialize(decoder: Decoder): SignData {
        val decodedString = decoder.decodeString()
        val json = Json { allowStructuredMapKeys = true }
        val jsonElement = json.decodeFromString<SignData>(decodedString)
        return jsonElement
    }
}
