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
    fun inventorySystemDoesNotMoveItemsUntilAnExplicitSlotTransfer() {
        val backpack = Inventory()
        val storage = BunkerStorage()
        val system = InventorySystem(backpack, storage)
        backpack.add(ItemType.WOOD, 4)
        backpack.add(ItemType.WATER, 2)

        // Creating/accessing the storage system must not transfer anything.
        assertEquals(4, backpack.count(ItemType.WOOD))
        assertEquals(2, backpack.count(ItemType.WATER))
        assertEquals(0, storage.count(ItemType.WOOD))
        assertEquals(0, storage.count(ItemType.WATER))

        assertTrue(system.transferToStorageAt(0, 2))
        assertEquals(2, backpack.count(ItemType.WOOD))
        assertEquals(2, backpack.count(ItemType.WATER))
        assertEquals(2, storage.count(ItemType.WOOD))
        assertEquals(0, storage.count(ItemType.WATER))
    }

    @Test
    fun clockAndNeedsSystemAdvancesTheSharedTimelineForActions() {
        val engine = GameEngine()
        val system = ClockAndNeedsSystem(
            engine.worldClock, engine.bunkerPower, engine.statistics,
            engine.needs, engine.psychology
        )
        val start = engine.worldClock.totalMinutes

        assertTrue(system.advanceActionMinutes(5, engine.zone, false) { })

        assertEquals(start + 5L, engine.worldClock.totalMinutes)
        assertEquals(100f - SurvivalNeeds.HUNGER_LOSS_PER_MINUTE * 5f, engine.needs.hunger, 0.001f)
    }


    @Test
    fun clockAndNeedsSystemInvokesRollbackWhenAnActionCausesDeath() {
        val engine = GameEngine()
        engine.needs.restore(SurvivalNeedsSnapshot(health = 0f, hunger = 0f, thirst = 0f, energy = 0f))
        val system = ClockAndNeedsSystem(
            engine.worldClock, engine.bunkerPower, engine.statistics,
            engine.needs, engine.psychology
        )
        var rollbackCalls = 0

        val survived = system.advanceActionMinutes(1, engine.zone, false) { rollbackCalls++ }

        assertFalse(survived)
        assertEquals(1, rollbackCalls)
    }

    @Test
    fun zoneTransitionRulesChooseMatchingReturnDoors() {
        assertEquals(TileType.EXIT_TO_BUNKER,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.BUNKER, WorldZone.FOREST))
        assertEquals(TileType.EXIT_TO_RUINS,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.RUINS, WorldZone.FOREST))
        assertEquals(TileType.EXIT_TO_FOREST,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.FOREST, WorldZone.RUINS))
        assertEquals(TileType.EXIT_TO_RUINS,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.RUINS, WorldZone.COMMUNICATION_NODE))
        assertEquals(TileType.EXIT_TO_COMMUNICATION_NODE,
            ZoneTransitionSystem.arrivalExitFor(WorldZone.COMMUNICATION_NODE, WorldZone.RUINS))
        assertFalse(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.COMMUNICATION_NODE, true))
        assertEquals(null, ZoneTransitionSystem.arrivalExitFor(WorldZone.FOREST, WorldZone.FOREST))
    }

    @Test
    fun proceduralZonesRegenerateOnlyWhenRequested() {
        assertTrue(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.RUINS, true))
        assertFalse(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.RUINS, false))
        assertFalse(ZoneTransitionSystem.shouldGenerateFreshMap(WorldZone.BUNKER, true))
    }
}
