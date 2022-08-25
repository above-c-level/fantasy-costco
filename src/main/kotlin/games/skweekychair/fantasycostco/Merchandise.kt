@file:UseSerializers(EnchantmentSerializer::class, LocationSerializer::class, UuidSerializer::class)

package games.skweekychair.fantasycostco

import java.util.Objects
import java.util.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.apache.commons.lang.WordUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.Sign
import org.bukkit.enchantments.Enchantment

/** Modifies the price of items pseudorandomly without affecting the best-guess price */
class Perturber : Runnable {
    override fun run() {
        for (item in Cereal.merch.values) {
            item.hold()
            item.updateAllSigns()
        }
        // Bukkit.broadcastMessage("Perturbed prices of ${Cereal.merch.size} items")
    }
}

/**
 * Represents a single item type in the shop.
 * @param material The base material as defined by Bukkit
 * @param enchantments The enchantments on the item
 */
@Serializable
open class BaseMerchandise(
        open val material: Material,
        open val enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BaseMerchandise) {
            return false
        }
        if (this.material == other.material && this.enchantments.equals(other.enchantments)) {
            return true
        }
        return false
    }
    override fun hashCode() = Objects.hash(material, enchantments)
}

/**
 * Represents a single item in the shop with enchantments as well as various variables that our
 * gradient descent algorithm uses to determine the best price.
 * @param material The material of the item.
 * @param mass The `mass` of our point in the gradient descent algorithm. It's used as a way of
 * determining an items "inertia" or resistance to change.
 * @param hiddenPrice The true, raw best guess at the price of the item.
 * @param shownPrice The price as displayed to players, which is smoothed to prevent too much price
 * movement too quickly, and also to add pseudorandom noise to the price even when no sales occur.
 * @param enchantments The enchantments on the item.
 * @param listOfSigns The signs that reference this merchandise
 * @constructor Creates a new merchandise item.
 */
@Serializable
class Merchandise(
        val material: Material,
        var mass: Double,
        var hiddenPrice: Double,
        var shownPrice: Double,
        val enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>(),
        val listOfSigns: MutableList<Location> = mutableListOf(),
        val hasFixedPrice: Boolean = false
) {
    /**
     * The constructor for a new merchandise item.
     * @param material The material of the item.
     * @param mass The `mass` of our point in the gradient descent algorithm.
     * @param startingPrice Our best guess as to the ideal starting price of the item.
     */
    constructor(
            material: Material,
            mass: Double,
            startingPrice: Double
    ) : this(material, mass, startingPrice, startingPrice)

    /**
     * The constructor for a new merchandise item.
     * @param material The material of the item.
     * @param mass The `mass` of our point in the gradient descent algorithm.
     * @param startingPrice Our best guess as to the ideal starting price of the item.
     * @param hasFixedPrice Whether or not this item has a fixed price.
     */
    constructor(
            material: Material,
            mass: Double,
            startingPrice: Double,
            hasFixedPrice: Boolean
    ) : this(material, mass, startingPrice, startingPrice, hasFixedPrice = hasFixedPrice)

    /**
     * Checks whether this merchandise item is equal to another.
     * @param other The other merchandise item to compare to.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is BaseMerchandise) {
            return false
        }
        if (this.material == other.material && this.enchantments.equals(other.enchantments)) {
            return true
        }
        return false
    }
    override fun hashCode() = Objects.hash(material, enchantments)

    /**
     * Increases the 'mass' of the commodity, making it harder to move in the future. Only increases
     * the mass if it's not already at `maximumMass`
     */
    fun addMass() {
        if (this.mass < CostcoGlobals.maximumMass) {
            this.mass += CostcoGlobals.massPerTransaction
            // this.mass = Math.min(CostcoGlobals.maximumMass, this.mass)
        }
    }

    /**
     * Prevents the shown price from moving more than `maxPctChange` than its previous value. For
     * example, if the hidden price moves from 1 to 2 but `maxPctChange` is 0.1, the new shown price
     * is 1.10.
     */
    fun smoothPrice() {
        val upperLimit: Double = (1.0 + CostcoGlobals.maxPctChange) * this.shownPrice
        val lowerLimit: Double = (1.0 - CostcoGlobals.maxPctChange) * this.shownPrice
        if (this.hiddenPrice < upperLimit && this.hiddenPrice > lowerLimit) {
            this.shownPrice = this.hiddenPrice
        } else if (this.hiddenPrice > upperLimit) {
            this.shownPrice = upperLimit
        } else {
            this.shownPrice = lowerLimit
        }
    }

    /**
     * Returns a rounded double representation of the price of the item.
     * @return The rounded shown price
     */
    fun roundedPrice(): Double {
        return roundDouble(this.shownPrice)
    }

    /**
     * Returns a rounded buy price of `amount` of this item
     * @param amount The amount of the item to buy.
     * @return The price of the item.
     */
    fun itemBuyPrice(amount: Int): Double {
        if (this.hasFixedPrice) {
            return this.shownPrice
        }
        return roundDouble(buyPrice(this.shownPrice, amount, this.material.getMaxStackSize()))
    }

    /**
     * Returns a rounded sell price of `amount` of this item
     * @param amount The amount of the item to sell.
     * @return The price of the item.
     */
    fun itemSellPrice(amount: Int): Double {
        if (this.hasFixedPrice) {
            return this.shownPrice
        }
        return roundDouble(sellPrice(this.shownPrice, amount, this.material.getMaxStackSize()))
    }

    /** Pseudo-randomly perturbs the price of the item without affecting the best-guess price. */
    private fun perturbPrice() {
        // hopefully this is either unnecessary or doesn't happen often
        // but just in case
        this.hiddenPrice = Math.abs(this.hiddenPrice)
        val massVar: Double =
                lerp(
                        this.mass,
                        0.0,
                        CostcoGlobals.massVarMin,
                        CostcoGlobals.maximumMass,
                        CostcoGlobals.massVarMax
                )
        val variation: Double = this.hiddenPrice * CostcoGlobals.varMultiplier * massVar
        val gaussian: Double = Random().nextGaussian() * variation
        this.hiddenPrice += gaussian
        val dist = Math.abs(this.hiddenPrice - this.shownPrice)
        val correctionGain =
                lerpClamp(
                        dist,
                        this.hiddenPrice,
                        this.hiddenPrice / 2.0,
                        CostcoGlobals.clampMultiplier
                )
        this.shownPrice =
                Math.abs(
                        (1.0 - correctionGain) * this.shownPrice + correctionGain * this.hiddenPrice
                )
    }

    /**
     * Calculates the magnitude of the change of price given a transaction
     * @param numItems The number of items in the transaction
     * @return The magnitude of the change of price.
     */
    private fun pushAmount(numItems: Double): Double {
        val dist: Double = Math.abs(this.shownPrice - this.hiddenPrice) + 1.0
        val sqrtPrice: Double = Math.sqrt(this.shownPrice)
        val multItems: Double = Math.sqrt(numItems) * CostcoGlobals.purchaseSizeMultiplier
        return Math.sqrt(multItems / this.mass * dist * sqrtPrice)
    }

    /** First smooths changes to price, then perturbs the price. */
    internal fun hold() {
        // Return early to fix price
        if (this.hasFixedPrice) {
            return
        }
        // Don't worry about messing with prices if nobody can see them
        if (this.listOfSigns.size == 0) {
            return
        }
        this.smoothPrice()
        this.perturbPrice()
    }

    /**
     * Updates our best guess at the price of the item by buying, smooths the price, adds mass, and
     * perturbs the price.
     */
    fun buy(numItems: Double) {
        // Return early to fix price
        if (this.hasFixedPrice) {
            return
        }
        this.hiddenPrice += pushAmount(numItems)
        this.smoothPrice()
        this.addMass()
        this.perturbPrice()
        this.updateAllSigns()
    }

    /**
     * Updates our best guess at the price of the item by selling, smooths the price, adds mass, and
     * perturbs the price.
     */
    fun sell(numItems: Double) {
        // Return early to fix price
        if (this.hasFixedPrice) {
            return
        }
        this.hiddenPrice -= pushAmount(numItems)
        this.smoothPrice()
        this.addMass()
        this.perturbPrice()
        this.updateAllSigns()
    }

    fun updateSignIsStale(signLocation: Location): Boolean {
        val blockState: BlockState = signLocation.getBlock().state
        if (blockState !is Sign) {
            return true
        }

        var thisSign: SignData? = Cereal.signs[signLocation]
        if (thisSign == null) {
            thisSign = SignData(SignType.TRUE_PRICE)
            Cereal.signs[signLocation] = thisSign
        }

        if (thisSign.signType == SignType.TRUE_PRICE) {
            this.updateSignLine(signLocation, 2, "Ideal Price:")
            this.updateSignLine(signLocation, 3, roundDoubleString(this.shownPrice))
        } else if (thisSign.signType == SignType.SELL_ONE) {
            this.clearSign(signLocation)
            this.updateSignLine(signLocation, 1, "Sell One")
            this.updateSignLine(signLocation, 2, "Item")
        } else if (thisSign.signType == SignType.SELL_STACK) {
            this.clearSign(signLocation)
            this.updateSignLine(signLocation, 1, "Sell a stack")
            this.updateSignLine(signLocation, 2, "of items")
        } else if (thisSign.signType == SignType.SELL_TYPE) {
            this.clearSign(signLocation)
            this.updateSignLine(signLocation, 0, "Sell all")
            this.updateSignLine(signLocation, 1, "of this type")
            this.updateSignLine(signLocation, 2, "(in inventory)")
        } else if (thisSign.signType == SignType.SELL_ALL) {
            this.clearSign(signLocation)
            this.updateSignLine(signLocation, 0, "Sell EVERYTHING")
            this.updateSignLine(signLocation, 1, "(yes, your whole")
            this.updateSignLine(signLocation, 2, "ENTIRE")
            this.updateSignLine(signLocation, 3, "inventory)")
        } else if (thisSign.signType == SignType.BUY_ONE) {
            val singlePrice = this.itemSellPrice(1)
            this.updateSignLine(signLocation, 2, "Buy One")
            this.updateSignLine(signLocation, 3, roundDoubleString(singlePrice))
        } else if (thisSign.signType == SignType.BUY_STACK) {
            this.updateSignLine(signLocation, 2, "Buy Stack")
            val stackPrice = this.itemSellPrice(this.material.maxStackSize)
            this.updateSignLine(signLocation, 3, roundDoubleString(stackPrice))
        } else if (thisSign.signType == SignType.BUY_SHULKER_BOX) {
            this.updateSignLine(signLocation, 2, "Buy full Shulker")
            val shulkerMerch = Material.getMaterial("SHULKER_BOX")!!
            // Ideal price of shulker box because they're already buying upwards of 1728 items
            val shulkerPrice = getMerchandise(BaseMerchandise(shulkerMerch)).shownPrice
            val filledPrice = this.itemSellPrice(this.material.maxStackSize * 27) + shulkerPrice
            this.updateSignLine(signLocation, 3, roundDoubleString(filledPrice))
        }

        return false
    }

    /** Updates the signs with the new prices. */
    fun updateAllSigns() {
        logIfDebug("Updating all signs")
        val staleLocations: MutableList<Location> = mutableListOf()
        if (this.listOfSigns.size == 0) {
            return
        }
        for (signLocation in this.listOfSigns) {
            if (this.updateSignIsStale(signLocation)) {
                staleLocations.add(signLocation)
            }
        }

        for (staleLocation in staleLocations) {
            RemoveSignFromMerch(staleLocation)
        }
    }

    fun updateSign(signLocation: Location, updateName: Boolean = true) {
        var isStale = updateSignIsStale(signLocation)
        if (isStale) {
            RemoveSignFromMerch(signLocation)
            return
        }
        if (updateName) {
            this.updateSignName(signLocation)
        }
    }

    /**
     * Updates the name listed on a sign.
     * @param signLocation The location of the sign.
     */
    private fun updateSignName(signLocation: Location) {
        var signText: List<String> = this.nameFormat(this.material.name)
        // Add blank string until signText has length 2
        while (signText.size < 2) {
            signText = signText.plus("")
        }
        for (i in 0 until signText.size) {
            this.updateSignLine(signLocation, i, signText[i])
        }
    }

    /**
     * Updates the sign at a given location with the given text. If `location` does not have a sign,
     * nothing happens.
     * @param location The location of the sign.
     * @param update A List of Pairs, where the first element is the line number and the second is
     * the text.
     */
    private fun updateSignText(location: Location, update: List<Pair<Int, String>>) {
        val blockState: BlockState = location.getBlock().state
        if (blockState !is Sign) {
            logIfDebug("Could not find sign")
            return
        }
        val sign: Sign = blockState
        for (i in 0 until update.size) {
            val pair = update[i]
            sign.setLine(pair.first, pair.second)
        }
        sign.update()
    }

    /**
     * Updates the sign at a given location with the given text. If `location` does not have a sign,
     * nothing happens.
     * @param location The location of the sign.
     * @param updateLine The line number to update.
     * @param text The text to update the line with.
     */
    private fun updateSignLine(location: Location, updateLine: Int, text: String) {
        this.updateSignText(location, mutableListOf(Pair(updateLine, text)))
    }

    /**
     * Clears all text from a sign at the given location. If `location` does not have a sign,
     * nothing happens.
     * @param location The location of the sign.
     */
    private fun clearSign(location: Location) {
        this.updateSignText(location, listOf(Pair(0, ""), Pair(1, ""), Pair(2, ""), Pair(3, "")))
    }

    /**
     * Formats the name of an item into a List of Strings that *should* fit on two lines of a
     * Minecraft sign.
     * @param nameIn The name of the item.
     * @return A List of Strings for putting on a Minecraft sign.
     */
    private fun nameFormat(nameIn: String): List<String> {
        // We only care about name, and we want to fit whole words
        // into chunks of 15 chars
        var name = nameIn.replace('_', ' ')
        name = WordUtils.capitalizeFully(name)
        var wordList = name.split(" ")
        var lines = joinWords(wordList)
        if (lines.size <= 2) {
            return lines
        }
        name = shortenWords(name)
        wordList = name.split(" ")
        lines = joinWords(wordList)
        return lines
    }

    /**
     * Takes in a list of words and returns a list of the joined words such that the length of each
     * line is less than 16 characters for Minecraft signs.
     * @param wordsIn The list of words to join.
     * @return A list of the joined words.
     */
    private fun joinWords(wordsIn: List<String>): List<String> {
        var words: MutableList<String> = wordsIn.toMutableList()
        val lines = mutableListOf<String>()
        // Join as many words as possible <= 15 chars
        var line = ""
        while (words.size > 0) {
            val currentWord = words[0]
            words.removeAt(0)

            // 14 chars + 1 space = 15 chars
            if (line.length + currentWord.length > 14) {
                lines.add(line)
                line = ""
            }
            line += "${currentWord} "
        }
        lines.add(line)
        return lines
    }

    /**
     * Takes in a string containing words and shortens common words that are too long so that when
     * split and joined, they can fit on a sign.
     * @param nameIn The string of words to shorten.
     * @return The shortened string of words.
     */
    private fun shortenWords(nameIn: String): String {
        // These are in decreasing order of frequency
        // (e.g. polished shows up most, followed by slab, etc)
        // Ties were decided by choosing longest first
        var name = nameIn
        name = name.replace("Polished", "Plshd")
        name = name.replace("Slab", "Slb")
        name = name.replace("Blackstone", "Blkstn")
        name = name.replace("Concrete", "Cncrt")
        name = name.replace("Weathered", "Wthrd")
        name = name.replace("Cobblestone", "Cblstn")
        name = name.replace("Stairs", "Strs")
        name = name.replace("Stained", "Stn")
        name = name.replace("Terracotta", "Trcta")
        name = name.replace("Deepslate", "Dpslt")
        return name
    }
}
