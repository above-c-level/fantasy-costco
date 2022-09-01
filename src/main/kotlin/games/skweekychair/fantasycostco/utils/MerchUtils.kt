package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

object MerchUtils {
    /**
     * Gets merchandise from the set of Merchandise. If it does not exist, it is added
     * automatically.
     * @param name The name of the merchandise.
     * @return The merchandise.
     */
    fun getMerchandise(baseMerch: BaseMerchandise): Merchandise {
        if (baseMerch.material == Material.ENCHANTED_BOOK) {
            val enchantments = baseMerch.enchantments
            var bestBook: BaseMerchandise? = null
            var bestPrice: Double = 0.0
            for ((enchantment, level) in enchantments) {
                val singleEnchantment = mapOf(enchantment to level)
                val singleBook = BaseMerchandise(Material.ENCHANTED_BOOK, singleEnchantment)
                // Ensure that each enchanted book exists
                if (singleBook !in Cereal.merch) {
                    val bestGuessPrice = CostcoGlobals.startingEnchantmentPrice(enchantment, level)
                    Cereal.merch[singleBook] =
                            Merchandise(
                                    baseMerch.material,
                                    singleEnchantment,
                                    CostcoGlobals.startingMass,
                                    bestGuessPrice
                            )
                }
                val singleBookPrice = Cereal.merch[singleBook]!!.shownPrice
                if (singleBookPrice > bestPrice) {
                    bestPrice = singleBookPrice
                    bestBook = singleBook
                }
            }
            val best = Cereal.merch[bestBook!!]
            return best!!
        }

        if (baseMerch !in Cereal.merch) {
            val material = baseMerch.material
            if (CostcoGlobals.hasFixedPrice(material)) {
                val bestGuessPrice = CostcoGlobals.fixedPrice(baseMerch.material)
                Cereal.merch[baseMerch] =
                        Merchandise(
                                baseMerch.material,
                                CostcoGlobals.startingMass,
                                bestGuessPrice,
                                hasFixedPrice = true
                        )
            } else {
                val bestGuessPrice = CostcoGlobals.startingMerchPrice(baseMerch.material)
                Cereal.merch[baseMerch] =
                        Merchandise(baseMerch.material, CostcoGlobals.startingMass, bestGuessPrice)
            }
        }

        return Cereal.merch[baseMerch]!!
    }

    /**
     * Gets the merchandise sold at the given location. If there is no merchandise at the location,
     * a null reference exception is thrown.
     * @param location The location to check.
     * @return The merchandise at the location.
     */
    fun getMerchandiseAtLocation(location: Location): Merchandise {
        val baseMerch = Cereal.purchasePoints[location]!!
        return getMerchandise(baseMerch)
    }

    /**
     * Gets merchandise from the set of Merchandise. If it does not exist, it is added
     * automatically.
     * @param material The item type as defined by bukkit's Material
     * @param enchantments The enchantments and levels of enchantments on the merchandise
     * @return The merchandise.
     */
    fun getMerchandise(
            material: Material,
            enchantments: Map<Enchantment, Int> = HashMap<Enchantment, Int>()
    ): Merchandise {

        val baseMerch = BaseMerchandise(material, enchantments)
        return getMerchandise(baseMerch)
    }

    /**
     * Gets merchandise from the set of Merchandise. If it does not exist, it is added
     * automatically.
     * @param item The item type as defined by bukkit's ItemStack
     * @return The merchandise.
     */
    fun getMerchandise(item: ItemStack): Merchandise {
        val baseMerch = BaseMerchandise(item.type, getItemEnchants(item))
        return getMerchandise(baseMerch)
    }
    /**
     * Enchanted books store their information differently. This method gets the map of enchantments
     * an item has, regardless of whether it's an enchanted book. If there are no enchantments,
     * returns an empty Map<Enchantment, Int>
     * @param item The item to get the enchantments of.
     * @return The map of enchantments and levels.
     */
    fun getItemEnchants(item: ItemStack): Map<Enchantment, Int> {
        if (item.type == Material.ENCHANTED_BOOK) {
            val meta = item.itemMeta
            if (meta !is EnchantmentStorageMeta) {
                return HashMap()
            }
            return meta.storedEnchants
        } else {
            return item.enchantments
        }
    }
}
