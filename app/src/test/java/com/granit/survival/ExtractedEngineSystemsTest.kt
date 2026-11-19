package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractedEngineSystemsTest {
    @Test
    fun inventorySystemMovesItemsWithoutLosingThem() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        val system = InventorySystem(backpack, storage)
        backpack.add(ItemType.WOOD, 7)

        assertTrue(system.transferToStorage(ItemType.WOOD, 7))
        assertEquals(0, backpack.count(ItemType.WOOD))
        assertEquals(7, storage.count(ItemType.WOOD))
        assertTrue(system.transferFromStorage(ItemType.WOOD, 3))
        assertEquals(3, backpack.count(ItemType.WOOD))
        assertEquals(4, storage.count(ItemType.WOOD))
    }

    @Test
    fun inventorySystemRejectsPartialTransfers() {
        val backpack = Inventory()
        val storage = BunkerStorage(columns = 1, rows = 1, stackLimit = 2)
        val system = InventorySystem(backpack, storage)
        backpack.add(ItemType.METAL, 4)

        assertFalse(system.transferToStorage(ItemType.METAL, 4))
        assertEquals(4, backpack.count(ItemType.METAL))
        assertEquals(0, storage.count(ItemType.METAL))
    }

    @Test
    fun zoneTransitionRulesChooseMatchingReturnDoors() {
        assertEquals(TileType.EXIT_TO_BUNKER,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.BUNKER, WorldZone.FOREST))
        assertEquals(TileType.EXIT_TO_RUINS,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.RUINS, WorldZone.FOREST))
        assertEquals(TileType.EXIT_TO_FOREST,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.FOREST, WorldZone.RUINS))
        assertEquals(null, ZoneTransitionSystem.arrivalExitFor(WorldZone.FOREST, WorldZone.FOREST))
    }

    @Test
    fun proceduralZonesRegenerateOnlyWhenRequested() {
        assertTrue(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.RUINS, true))
        assertFalse(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.RUINS, false))
        assertFalse(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.BUNKER, true))
    }
}
