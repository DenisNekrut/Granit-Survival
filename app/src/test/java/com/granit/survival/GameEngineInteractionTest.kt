package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineInteractionTest {
    @Test
    fun storageCanOnlyBeUsedNearbyAndTransfersItemsSafely() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 6)

        assertFalse(engine.canAccessStorage())
        assertFalse(engine.transferToStorage(ItemType.WOOD))

        engine.player.x = 15.5f
        engine.player.y = 2.5f
        assertTrue(engine.canAccessStorage())
        assertTrue(engine.transferToStorage(ItemType.WOOD))
        assertEquals(0, engine.inventory.count(ItemType.WOOD))
        assertEquals(6, engine.bunkerStorage.count(ItemType.WOOD))
    }

    @Test
    fun sleepRequiresBeingNearTheBunkerBed() {
        val engine = GameEngine()
        assertFalse(engine.canSleep())

        engine.player.x = 6.0f
        engine.player.y = 2.5f
        assertTrue(engine.canSleep())
    }

    @Test
    fun consumingFoodAndWaterUsesOneItemAndAdvancesTime() {
        val engine = GameEngine()
        engine.needs.advanceAwake(500)
        engine.inventory.add(ItemType.CANNED_FOOD, 1)
        engine.inventory.add(ItemType.WATER, 1)
        val startMinutes = engine.worldClock.totalMinutes

        assertTrue(engine.consumeFood())
        assertTrue(engine.drinkWater())

        assertEquals(0, engine.inventory.count(ItemType.CANNED_FOOD))
        assertEquals(0, engine.inventory.count(ItemType.WATER))
        assertEquals(startMinutes + 10, engine.worldClock.totalMinutes)
    }
}
