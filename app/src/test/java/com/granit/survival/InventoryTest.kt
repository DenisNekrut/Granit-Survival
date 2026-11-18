package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryTest {
    @Test
    fun addMergesItemsIntoExistingStack() {
        val inventory = Inventory()
        assertEquals(0, inventory.add(ItemType.WOOD, 12))
        assertEquals(0, inventory.add(ItemType.WOOD, 5))
        assertEquals(17, inventory.count(ItemType.WOOD))
        assertEquals(17, inventory.slot(0)?.quantity)
    }

    @Test
    fun addRespectsStackLimitAndReportsOverflow() {
        val inventory = Inventory()
        assertEquals(0, inventory.add(ItemType.METAL, 25))
        assertEquals(20, inventory.slot(0)?.quantity)
        assertEquals(5, inventory.slot(1)?.quantity)
    }

    @Test
    fun fullInventoryReturnsUnstoredAmount() {
        val inventory = Inventory()
        assertEquals(0, inventory.add(ItemType.FABRIC, 320))
        assertEquals(81, inventory.add(ItemType.WOOD, 81))
        assertEquals(0, inventory.count(ItemType.WOOD))
    }

    @Test
    fun removeIsAtomicWhenThereAreNotEnoughItems() {
        val inventory = Inventory()
        inventory.add(ItemType.WATER, 3)
        assertFalse(inventory.remove(ItemType.WATER, 4))
        assertEquals(3, inventory.count(ItemType.WATER))
    }

    @Test
    fun removeCanSpanMultipleStacks() {
        val inventory = Inventory()
        inventory.add(ItemType.CANNED_FOOD, 25)
        assertTrue(inventory.remove(ItemType.CANNED_FOOD, 22))
        assertEquals(3, inventory.count(ItemType.CANNED_FOOD))
    }
}
