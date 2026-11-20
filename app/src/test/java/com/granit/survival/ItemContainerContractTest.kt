package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemContainerContractTest {
    @Test
    fun backpackAndBunkerStorageExposeTheSameFixedSlotContract() {
        val containers: List<ItemContainer> = listOf(
            Inventory(columns = 4, rows = 2),
            BunkerStorage(columns = 4, rows = 2)
        )

        containers.forEach { container ->
            assertEquals(4, container.columns)
            assertEquals(2, container.rows)
            assertEquals(8, container.snapshot().size)
            assertEquals(null, container.slot(7))
            assertTrue(container.canAdd(ItemType.WOOD, 3))
            assertEquals(0, container.add(ItemType.WOOD, 3))
            assertEquals(ItemStack(ItemType.WOOD, 3), container.slot(0))
            assertEquals(3, container.count(ItemType.WOOD))
            assertTrue(container.removeAt(0, 2))
            assertEquals(ItemStack(ItemType.WOOD, 1), container.slot(0))
        }
    }

    @Test
    fun invalidRestoreDoesNotPartiallyChangeContainerContents() {
        val container: ItemContainer = Inventory(columns = 2, rows = 1, stackLimit = 5)
        container.add(ItemType.METAL, 2)
        val before = container.snapshot()

        val invalid = listOf(
            ItemStack(ItemType.WOOD, 3),
            ItemStack(ItemType.WATER, 6)
        )
        val rejected = runCatching { container.restore(invalid) }.isFailure

        assertTrue(rejected)
        assertEquals(before, container.snapshot())
    }

    @Test
    fun outOfRangeSlotAndInvalidAmountsAreRejectedWithoutMutation() {
        val container: ItemContainer = BunkerStorage(columns = 1, rows = 1)
        container.add(ItemType.FABRIC, 2)
        val before = container.snapshot()

        assertEquals(null, container.slot(1))
        assertFalse(container.removeAt(1, 1))
        assertFalse(container.removeAt(0, 0))
        assertFalse(container.canAdd(ItemType.WOOD, 0))
        assertEquals(before, container.snapshot())
    }

    @Test
    fun transferApiAcceptsAnyItemContainerImplementation() {
        val source: ItemContainer = Inventory(columns = 2, rows = 1)
        val destination = BunkerStorage(columns = 2, rows = 1)
        source.add(ItemType.WOOD, 6)

        assertTrue(destination.deposit(source, ItemType.WOOD, 4))
        assertEquals(2, source.count(ItemType.WOOD))
        assertEquals(4, destination.count(ItemType.WOOD))
        assertTrue(destination.withdraw(source, ItemType.WOOD, 3))
        assertEquals(5, source.count(ItemType.WOOD))
        assertEquals(1, destination.count(ItemType.WOOD))
    }

}
