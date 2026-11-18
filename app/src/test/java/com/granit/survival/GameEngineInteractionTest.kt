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
    fun collectingResourcesAndChangingZonesConsumeGameTime() {
        val engine = GameEngine()
        engine.player.x = 7.5f
        engine.player.y = 8.5f
        val start = engine.worldClock.totalMinutes

        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(start + GameEngine.RESOURCE_GATHER_MINUTES, engine.worldClock.totalMinutes)
        assertEquals(1, engine.inventory.count(ItemType.WOOD))

        engine.changeZone(WorldZone.BUNKER)
        engine.player.x = 20.5f
        engine.player.y = 0.8f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.FOREST, engine.zone)
        assertEquals(start + GameEngine.RESOURCE_GATHER_MINUTES + GameEngine.ZONE_TRAVEL_MINUTES,
            engine.worldClock.totalMinutes)
    }

    @Test
    fun craftedItemsCannotBeCollectedAsWorldResourceNodes() {
        val map = TileMap()
        assertFalse(map.collectResource(ResourceLocation(7, 8, ItemType.MEDICAL_KIT)))
        assertFalse(map.collectResource(ResourceLocation(7, 8, ItemType.BANDAGE)))
        assertEquals(TileType.RESOURCE_WOOD, map[7, 8])
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
