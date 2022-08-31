package games.skweekychair.fantasycostco

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.bukkit.ChatColor.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.GlowItemFrame
import org.bukkit.inventory.ItemStack

object FrameUtils {
    // This is a bit hacky BUT SO ARE ITEM FRAMES
    // Basically I'm just offsetting by whatever amount happens to work to sort of recenter
    // to reasonable values so that we can check within a reasonable distance
    val offset = 0.46875
    val distance = 0.5
    /**
     * Checks whether there exists a glow item frame at the current location. Since item frames and
     * glow item frames are entities, we have to use .getNearbyEntities() to check for nearby
     * frames.
     * @param location The location to check.
     * @return True if there is a glow item frame at the location, false otherwise.
     */
    fun isGlowItemFrame(location: Location): Boolean {
        var entities = getEntitiesByLocation(location)

        for (entity in entities) {
            if (entity is org.bukkit.entity.GlowItemFrame) {
                return true
            }
        }
        return false
    }

    fun addOrUpdateFrame(location: Location, baseMerch: BaseMerchandise) {
        // Check if a glow item frame is already here
        if (!FrameUtils.isGlowItemFrame(location)) {
            // `itemFrameLocation` is directly under the sign block
            // Make sure this block is air for adding the glow item frame
            if (location.getBlock().getType() == Material.AIR) {
                val world = location.world!!
                val frame = world.spawnEntity(location, EntityType.GLOW_ITEM_FRAME) as GlowItemFrame
                frame.setItem(ItemStack(baseMerch.material))
                frame.setCustomName(MerchUtils.getMerchandise(baseMerch).getName())
                // Make sure that only creative players can break the frame
                frame.setInvulnerable(true)
                // Also make sure that no items drop from frame
                frame.setItemDropChance(0.0F)
            }
        } else {
            // There is a glow item frame there, so get it
            val entities = getEntitiesByLocation(location)
            val frame = entities.first { it is GlowItemFrame } as GlowItemFrame
            // Update the frame with the new item
            frame.setItem(ItemStack(baseMerch.material))
            frame.setCustomName(MerchUtils.getMerchandise(baseMerch).getName())
        }
    }

    /**
     * A helper method that gets a list of all entities within a block, with an offset accounts for
     * rounding to nearest even since that's what it looks like minecraft does.
     * @param location The location to check.
     * @return A list of all entities within the block.
     */
    private fun getEntitiesByLocation(location: Location): Collection<Entity> {
        val world = location.world!!
        // We need to clone so we don't have knock-on effects, for some reason calling .add
        // on the location mutates the location
        val newLocation = location.clone().add(offset, 0.0, offset)
        return world.getNearbyEntities(newLocation, distance, 0.5, distance)
    }
}
