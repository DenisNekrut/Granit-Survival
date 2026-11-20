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

}
