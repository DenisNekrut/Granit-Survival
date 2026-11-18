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
}
