/*
 * OKTW Galaxy Project
 * Copyright (C) 2018-2021
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package one.oktw.galaxy.block.pipe

import net.minecraft.block.ChestBlock
import net.minecraft.block.InventoryProvider
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SidedInventory
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import one.oktw.galaxy.mixin.interfaces.InventoryAvailableSlots

object PipeUtil {
    fun Inventory.getAvailableSlots(side: Direction): IntArray {
        if (this is SidedInventory) return this.getAvailableSlots(side)
        if (this is InventoryAvailableSlots) return (this as InventoryAvailableSlots).availableSlots

        return (0 until this.size()).toList().toIntArray()
    }

    fun ItemStack.canMergeWith(item: ItemStack): Boolean {
        return when {
            this.item !== item.item -> false
            this.damage != item.damage -> false
            this.count > this.maxCount -> false
            else -> ItemStack.areTagsEqual(this, item)
        }
    }

    fun getInventory(world: ServerWorld, pos: BlockPos): Inventory? {
        return when (val blockEntity = world.getBlockEntity(pos)) {
            is InventoryProvider -> blockEntity.getInventory(world.getBlockState(pos), world, pos)
            is ChestBlockEntity -> {
                val blockState = world.getBlockState(pos)
                ChestBlock.getInventory(blockState.block as ChestBlock, blockState, world, pos, true)
            }
            is Inventory -> blockEntity
            else -> null
        }
    }
}