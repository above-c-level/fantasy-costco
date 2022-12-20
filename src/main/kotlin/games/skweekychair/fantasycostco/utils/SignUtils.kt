package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.apache.commons.lang.WordUtils
import org.bukkit.ChatColor
import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockState
import org.bukkit.block.Sign
import org.bukkit.enchantments.Enchantment

/** An enum that gives all the ways a sign might be used. */
@Serializable
enum class SignType {
    SELL_ONE,
    SELL_STACK,
    SELL_TYPE,
    SELL_ALL,
    BUY_ONE,
    BUY_STACK,
    BUY_SHULKER_BOX,
    TRUE_PRICE
}

/**
 * A class that represents a sign. The sign has a location and some internal values which represent
 * whether it is a sell or buy sign, as well as the
 */
@Serializable
data class SignData(
        var signType: SignType,
        @Transient var lastUpdated: Long = 0L,
        @Transient var lastUpdatedBy: String = ""
) {
    /** Cycle to the next sell option */
    fun nextSellOption() {
        when (signType) {
            SignType.SELL_ONE -> signType = SignType.SELL_STACK
            SignType.SELL_STACK -> signType = SignType.SELL_TYPE
            SignType.SELL_TYPE -> signType = SignType.SELL_ONE
            else -> {
                signType = SignType.SELL_ONE
            }
        }
    }

    /** Cycle to the next buy option */
    fun nextBuyOption() {
        when (signType) {
            SignType.BUY_ONE -> signType = SignType.BUY_STACK
            SignType.BUY_STACK -> signType = SignType.BUY_SHULKER_BOX
            SignType.BUY_SHULKER_BOX -> signType = SignType.TRUE_PRICE
            SignType.TRUE_PRICE -> signType = SignType.BUY_ONE
            else -> {
                signType = SignType.BUY_ONE
            }
        }
    }
    fun isSelling() =
            signType in
                    listOf(
                            SignType.SELL_ONE,
                            SignType.SELL_STACK,
                            SignType.SELL_TYPE,
                            SignType.SELL_ALL
                    )
    fun isBuying() =
            signType in
                    listOf(
                            SignType.BUY_ONE,
                            SignType.BUY_STACK,
                            SignType.BUY_SHULKER_BOX,
                            SignType.TRUE_PRICE
                    )
}

object SignUtils {
    /** Updates the signs with the new prices. */
    fun updateAllSigns(merch: Merchandise) {
        val staleLocations: MutableList<Location> = mutableListOf()
        if (merch.listOfSigns.size == 0) {
            return
        }
        for (signLocation in merch.listOfSigns) {
            if (this.updateSignIsStale(signLocation, merch)) {
                staleLocations.add(signLocation)
            }
        }

        for (staleLocation in staleLocations) {
            this.removeSignFromMerch(staleLocation)
        }
    }

    /**
     * Updates the information on the sign at the given location. If the sign is not a valid sign,
     * it initializes a new sign to TRUE_PRICE.
     * @param signLocation The location of the sign to update.
     * @param merch The merchandise to update the sign with. Can be null for sell signs, but should
     * never be null for buy signs.
     * @return True if the sign is stale, false otherwise.
     */
    fun updateSignIsStale(signLocation: Location, merch: Merchandise? = null): Boolean {
        val blockState: BlockState = signLocation.getBlock().state
        if (blockState !is Sign) {
            return true
        }

        var thisSign: SignData? = Cereal.signs[signLocation]
        if (thisSign == null) {
            thisSign = SignData(SignType.TRUE_PRICE)
            Cereal.signs[signLocation] = thisSign
        }

        when (thisSign.signType) {
            // The first ones here are the sell signs
            SignType.SELL_ONE -> {
                this.clearSign(signLocation)
                this.updateSignLine(signLocation, 1, "Sell One")
                this.updateSignLine(signLocation, 2, "Item")
                this.colorFormat(signLocation, false)
            }
            SignType.SELL_STACK -> {
                this.clearSign(signLocation)
                this.updateSignLine(signLocation, 1, "Sell a stack")
                this.updateSignLine(signLocation, 2, "of items")
                this.colorFormat(signLocation, false)
            }
            SignType.SELL_TYPE -> {
                this.clearSign(signLocation)
                this.updateSignLine(signLocation, 0, "Sell all")
                this.updateSignLine(signLocation, 1, "of this type")
                this.updateSignLine(signLocation, 2, "(in inventory)")
                this.colorFormat(signLocation, false)
            }
            SignType.SELL_ALL -> {
                this.clearSign(signLocation)
                this.updateSignLine(signLocation, 0, "Sell EVERYTHING")
                this.updateSignLine(signLocation, 1, "(yes, your whole")
                this.updateSignLine(signLocation, 2, "ENTIRE")
                this.updateSignLine(signLocation, 3, "inventory)")
                this.colorFormat(signLocation, false)
            }
            // Here are the buy signs
            SignType.TRUE_PRICE -> {
                this.updateSignLine(signLocation, 2, "Ideal Price:")
                this.updateSignLine(
                        signLocation,
                        3,
                        MemberUtils.roundDoubleString(merch!!.shownPrice)
                )
                this.colorFormat(signLocation, true)
            }
            SignType.BUY_ONE -> {
                val singlePrice = merch!!.itemBuyPrice(1)
                this.updateSignLine(signLocation, 2, "Buy One")
                this.updateSignLine(signLocation, 3, MemberUtils.roundDoubleString(singlePrice))
                this.colorFormat(signLocation, true)
            }
            SignType.BUY_STACK -> {
                this.updateSignLine(signLocation, 2, "Buy Stack")
                val stackPrice = merch!!.itemBuyPrice(merch.material.maxStackSize)
                this.updateSignLine(signLocation, 3, MemberUtils.roundDoubleString(stackPrice))
                this.colorFormat(signLocation, true)
            }
            SignType.BUY_SHULKER_BOX -> {
                this.updateSignLine(signLocation, 2, "Buy full Shulker")
                val shulkerMerch = Material.getMaterial("SHULKER_BOX")!!
                // Ideal price of shulker box because they're already buying upwards of 1728 items
                val shulkerPrice =
                        MerchUtils.getMerchandise(BaseMerchandise(shulkerMerch)).shownPrice
                val filledPrice =
                        merch!!.itemBuyPrice(merch.material.maxStackSize * 27) + shulkerPrice
                this.updateSignLine(signLocation, 3, MemberUtils.roundDoubleString(filledPrice))
                this.colorFormat(signLocation, true)
            }
        }

        return false
    }

    /**
     * Updates the name listed on a sign.
     * @param signLocation The location of the sign.
     */
    private fun updateSignName(signLocation: Location, merch: Merchandise?) {
        // This shouldn't be called with any null merch
        if (merch == null) {
            logIfDebug("Merch is null in updateSignName for sign at $signLocation")
            return
        }
        var signText: List<String>
        if (merch.material == Material.ENCHANTED_BOOK) {
            signText = this.enchantedBookName(merch)
        } else {
            signText = this.nameFormat(merch.material.name)
        }
        // Add blank string until signText has length 2
        while (signText.size < 2) {
            signText = signText.plus("")
        }
        for (i in 0 until signText.size) {
            this.updateSignLine(signLocation, i, signText[i])
        }
    }

    private fun enchantedBookName(merch: Merchandise): List<String> {
        val enchantments = merch.enchantments
        // Should only ever be one single enchantment
        if (enchantments.size == 0) {
            logIfDebug("No enchantments in enchanted book merch")
            return listOf("Enchanted Book")
        }
        val enchantment = enchantments.keys.first()
        val level = enchantments[enchantment]!!
        val enchantmentName = enchantedBookString(enchantment, level)
        val signText = nameFormat(enchantmentName)
        return signText
    }

    fun enchantedBookNameString(merch: Merchandise): String {
        val enchantments = merch.enchantments
        // Should only ever be one single enchantment
        if (enchantments.size == 0) {
            logIfDebug("No enchantments in enchanted book merch")
            return "Enchanted Book"
        }
        val enchantment = enchantments.keys.first()
        val level = enchantments[enchantment]!!
        val enchantmentName = enchantedBookString(enchantment, level)
        val signText = nameFormat(enchantmentName).joinToString(" ")
        return signText
    }

    private fun enchantedBookString(enchantment: Enchantment, level: Int): String {
        val enchantName = enchantment.key.toString()
        val name =
                when (enchantName) {
                    "minecraft:fire_protection" -> "Fire Protection"
                    "minecraft:sharpness" -> "Sharpness"
                    "minecraft:flame" -> "Flame"
                    "minecraft:soul_speed" -> "Soul Speed"
                    "minecraft:aqua_affinity" -> "Aqua Affinity"
                    "minecraft:punch" -> "Punch"
                    "minecraft:loyalty" -> "Loyalty"
                    "minecraft:depth_strider" -> "Depth Strider"
                    "minecraft:vanishing_curse" -> "Curse of Vanishing"
                    "minecraft:unbreaking" -> "Unbreaking"
                    "minecraft:knockback" -> "Knockback"
                    "minecraft:luck_of_the_sea" -> "Luck of the Sea"
                    "minecraft:binding_curse" -> "Curse of Binding"
                    "minecraft:fortune" -> "Fortune"
                    "minecraft:protection" -> "Protection"
                    "minecraft:efficiency" -> "Efficiency"
                    "minecraft:mending" -> "Mending"
                    "minecraft:frost_walker" -> "Frost Walker"
                    "minecraft:lure" -> "Lure"
                    "minecraft:looting" -> "Looting"
                    "minecraft:piercing" -> "Piercing"
                    "minecraft:blast_protection" -> "Blast Protection"
                    "minecraft:smite" -> "Smite"
                    "minecraft:multishot" -> "Multishot"
                    "minecraft:fire_aspect" -> "Fire Aspect"
                    "minecraft:channeling" -> "Channeling"
                    "minecraft:sweeping" -> "Sweeping"
                    "minecraft:thorns" -> "Thorns"
                    "minecraft:bane_of_arthropods" -> "Bane of Arthropods"
                    "minecraft:respiration" -> "Respiration"
                    "minecraft:riptide" -> "Riptide"
                    "minecraft:silk_touch" -> "Silk Touch"
                    "minecraft:quick_charge" -> "Quick Charge"
                    "minecraft:projectile_protection" -> "Projectile Protection"
                    "minecraft:impaling" -> "Impaling"
                    "minecraft:feather_falling" -> "Feather Falling"
                    "minecraft:power" -> "Power"
                    "minecraft:infinity" -> "Infinity"
                    else -> enchantName
                }
        if (enchantment.maxLevel == 1) {
            return name
        }
        return "$name $level"
    }

    /**
     * Updates the sign at the given location.
     * @param signLocation The location of the sign to update.
     * @param updateName Whether to update the name of the item on the sign. Defaults to true.
     */
    fun updateSign(signLocation: Location, updateName: Boolean = true, merch: Merchandise? = null) {
        var isStale = updateSignIsStale(signLocation, merch)
        if (isStale) {
            this.removeSignFromMerch(signLocation)
            return
        }
        if (updateName) {
            this.updateSignName(signLocation, merch)
        }
    }

    /**
     * Formats the color of a sign to fit a standard design.
     * @param signLocation The location of the sign to format.
     * @param isBuySign Whether the sign is a buy sign.
     */
    fun colorFormat(signLocation: Location, isBuySign: Boolean) {
        val sign: BlockState = signLocation.getBlock().state
        if (sign !is Sign) {
            return
        }
        sign.setGlowingText(false)
        var colors: List<String>
        if (isBuySign) {
            colors = listOf("§#0000FF", "§#0000FF", "${BLACK}", "${BLACK}")
        } else {
            colors = listOf("${BLACK}", "${BLACK}", "${BLACK}", "${BLACK}")
        }

        for (i in 0 until sign.lines.size) {
            sign.setLine(i, colors[i] + ChatColor.stripColor(sign.getLine(i)))
        }
        sign.update()
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
        // Join as many words as possible <= 16 chars
        var line = ""
        while (words.size > 0) {
            val currentWord =
                    when (words[0]) {
                        "1" -> "I"
                        "2" -> "II"
                        "3" -> "III"
                        "4" -> "IV"
                        "5" -> "V"
                        else -> words[0]
                    }
            words.removeAt(0)

            // 15 chars + 1 space = 16 chars
            if (line.length + currentWord.length > 15) {
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

    /**
     * Removes a merchandise-sign association
     * @param location The location of the sign.
     * @return True if there was an association (which was then removed), false otherwise.
     */
    fun removeSignFromMerch(location: Location): Boolean {
        if (location in Cereal.signs) {
            Cereal.signs.remove(location)
        }
        if (location in Cereal.purchasePoints) {
            // logIfDebug("Removing ${location.blockX} ${location.blockY} ${location.blockZ}")
            // Grab the base merch associated with the location.
            val baseMerchAtLocation = Cereal.purchasePoints[location]
            // Get the old merch associated with the location.
            val oldMerch = MerchUtils.getMerchandise(baseMerchAtLocation!!)
            // Remove the location from the list of purchase points
            Cereal.purchasePoints.remove(location)
            // Lastly, remove the location from the old merch
            oldMerch.listOfSigns.remove(location)
            return true
        }
        return false
    }

    /**
     * Adds a sign to the merchandise at the given location. If a sign already exists, it is
     * overwritten. Also removes the sign from the merchandise it was previously associated with.
     * @param baseMerch The BaseMerchandise to associate the sign with.
     * @param location The location of the sign.
     * @param signType The type of the sign.
     */
    fun addSignToMerch(baseMerch: BaseMerchandise, location: Location, signType: SignType) {
        // TODO: Check for null signs?
        // First, check if the location already is catalogued, and if so, remove it.
        removeSignFromMerch(location)
        // Add the location to the purchase points and the merch.
        Cereal.purchasePoints[location] = baseMerch
        val merch = MerchUtils.getMerchandise(baseMerch)
        merch.listOfSigns.add(location)
        Cereal.signs[location] = SignData(signType)
        this.updateSign(location, merch = MerchUtils.getMerchandise(baseMerch))
    }

    /**
     * Removes the sign data from a location.
     * @param location The location of the sign.
     */
    fun removeSignData(location: Location) {
        Cereal.signs.remove(location)
    }
    /**
     * Adds sign data to a given location. If a sign already exists, it is overwritten. Also removes
     * the sign from the location it was previously associated with.
     * @param location The location of the sign.
     * @param signType The type of the sign.
     */
    fun addSignData(location: Location, signType: SignType) {
        // First, check if the location already is catalogued, and if so, remove it.
        removeSignData(location)
        Cereal.signs[location] = SignData(signType)
    }

    /**
     * Rotates the sign type at the given location.
     * @param location The location of the sign.
     * @return true if the sign was rotated, false if the sign could not be found
     */
    fun rotateSign(location: Location): Boolean {
        val signData = Cereal.signs[location]
        if (signData == null) {
            return false
        }
        if (signData.isSelling()) {
            signData.nextSellOption()
            updateSign(location, false)
            colorFormat(location, false)
        } else {
            signData.nextBuyOption()
            val merch = MerchUtils.getMerchandiseAtLocation(location)
            if (signData.signType == SignType.BUY_STACK && merch.material.maxStackSize == 1) {
                signData.nextBuyOption()
            }
            updateSign(location, false, merch)
            colorFormat(location, true)
        }

        return true
    }

    /**
     * Updates all signs currently set to selling shulker boxes of items to have the correct value.
     * Should be called when a shulker box is sold.
     */
    fun updateAllShulkerSigns() {
        for ((location, signData) in Cereal.signs) {
            if (signData.signType == SignType.BUY_SHULKER_BOX) {
                val merch = MerchUtils.getMerchandiseAtLocation(location)
                updateSign(location, false, merch)
            }
        }
    }

    /**
     * Simple helper function to determine whether a location contains a sign.
     * @param location The location to check.
     * @return True if the location contains a sign, false otherwise.
     */
    fun isSignBlock(location: Location): Boolean {
        return "SIGN" in location.block.type.toString()
    }
}
