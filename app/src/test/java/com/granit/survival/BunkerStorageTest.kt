package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerStorageTest {
    @Test
    fun depositAndWithdrawMoveItemsBetweenContainers() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        backpack.add(ItemType.WOOD, 12)

        assertTrue(storage.deposit(backpack, ItemType.WOOD, 7))
        assertEquals(5, backpack.count(ItemType.WOOD))
        assertEquals(7, storage.count(ItemType.WOOD))

        val fieldBag = Inventory()
        assertTrue(storage.withdraw(fieldBag, ItemType.WOOD, 4))
        assertEquals(3, storage.count(ItemType.WOOD))
        assertEquals(4, fieldBag.count(ItemType.WOOD))
    }

    @Test
    fun failedTransferDoesNotLoseItems() {
        val backpack = Inventory()
        val storage = BunkerStorage(columns = 1, rows = 1, stackLimit = 5)
        backpack.add(ItemType.METAL, 8)

        assertFalse(storage.deposit(backpack, ItemType.METAL, 8))
        assertEquals(8, backpack.count(ItemType.METAL))
        assertEquals(0, storage.count(ItemType.METAL))
    }
    @Test
    fun openingStorageViewDoesNotMoveBackpackItems() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        backpack.add(ItemType.WOOD, 12)

        // Reading the two containers is what the storage screen does on open.
        backpack.snapshot()
        storage.snapshot()
        assertEquals(12, backpack.count(ItemType.WOOD))
        assertEquals(0, storage.count(ItemType.WOOD))
    }

    @Test
    fun slotTransferMovesOnlyTheSelectedStackAndQuantity() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        backpack.add(ItemType.WOOD, 25)
        backpack.add(ItemType.METAL, 3)

        assertTrue(storage.depositAt(backpack, 1, 2))
        assertEquals(23, backpack.count(ItemType.WOOD))
        assertEquals(3, backpack.count(ItemType.METAL))
        assertEquals(2, storage.count(ItemType.WOOD))
        assertEquals(2, storage.slot(0)?.quantity)

        assertTrue(storage.withdrawAt(backpack, 0, 1))
        assertEquals(24, backpack.count(ItemType.WOOD))
        assertEquals(1, storage.count(ItemType.WOOD))
    }

    @Test
    fun failedSlotTransferDoesNotMutateEitherContainer() {
        val backpack = Inventory()
        val storage = BunkerStorage(columns = 1, rows = 1, stackLimit = 2)
        backpack.add(ItemType.METAL, 5)

        assertFalse(storage.depositAt(backpack, 0, 5))
        assertEquals(5, backpack.count(ItemType.METAL))
        assertEquals(0, storage.count(ItemType.METAL))
    }


    @Test
    fun slotTransferCanFillExistingStackAndThenAnEmptySlot() {
        val backpack = Inventory(columns = 2, rows = 1, stackLimit = 20)
        val storage = BunkerStorage(columns = 2, rows = 1, stackLimit = 20)
        backpack.add(ItemType.WOOD, 7)
        storage.restore(listOf(ItemStack(ItemType.WOOD, 18), null))

        assertTrue(storage.depositAt(backpack, 0, 7))
        assertEquals(0, backpack.count(ItemType.WOOD))
        assertEquals(20, storage.slot(0)?.quantity)
        assertEquals(5, storage.slot(1)?.quantity)
    }

    @Test
    fun slotTransferRejectsFullDestinationWithoutChangingEitherSlot() {
        val backpack = Inventory(columns = 1, rows = 1, stackLimit = 20)
        val storage = BunkerStorage(columns = 1, rows = 1, stackLimit = 20)
        backpack.add(ItemType.WOOD, 6)
        storage.restore(listOf(ItemStack(ItemType.METAL, 20)))
        val sourceBefore = backpack.snapshot()
        val destinationBefore = storage.snapshot()

        assertFalse(storage.depositAt(backpack, 0, 6))
        assertEquals(sourceBefore, backpack.snapshot())
        assertEquals(destinationBefore, storage.snapshot())
    }

    @Test
    fun slotTransferRejectsAmountLargerThanSelectedStackWithoutMutation() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        backpack.add(ItemType.WOOD, 4)
        val sourceBefore = backpack.snapshot()
        val destinationBefore = storage.snapshot()

        assertFalse(storage.depositAt(backpack, 0, 5))
        assertEquals(sourceBefore, backpack.snapshot())
        assertEquals(destinationBefore, storage.snapshot())
    }

    @Test
    fun transferFromStorageFailsWhenBackpackHasNoCapacity() {
        val backpack = Inventory(columns = 1, rows = 1, stackLimit = 5)
        val storage = BunkerStorage(columns = 1, rows = 1, stackLimit = 5)
        backpack.add(ItemType.METAL, 5)
        storage.restore(listOf(ItemStack(ItemType.WOOD, 3)))
        val backpackBefore = backpack.snapshot()
        val storageBefore = storage.snapshot()

        assertFalse(storage.withdrawAt(backpack, 0, 3))
        assertEquals(backpackBefore, backpack.snapshot())
        assertEquals(storageBefore, storage.snapshot())
    }

    @Test
    fun partialSlotTransferKeepsRemainderInOriginalSourceCell() {
        val backpack = Inventory(columns = 2, rows = 1, stackLimit = 20)
        val storage = BunkerStorage(columns = 2, rows = 1, stackLimit = 20)
        backpack.add(ItemType.WOOD, 11)

        assertTrue(storage.depositAt(backpack, 0, 4))
        assertEquals(7, backpack.slot(0)?.quantity)
        assertEquals(null, backpack.slot(1))
        assertEquals(4, storage.slot(0)?.quantity)
    }

}
