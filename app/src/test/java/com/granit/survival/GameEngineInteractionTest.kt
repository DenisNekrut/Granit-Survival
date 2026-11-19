package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineInteractionTest {
    @Test
    fun playerFacingTracksFourMovementDirectionsAndKeepsLastDirectionWhenIdle() {
        val engine = GameEngine()

        engine.updateFrame(1f / 60f, EngineInput(moveX = 1f))
        assertEquals(PlayerFacing.RIGHT, engine.player.facing)

        engine.updateFrame(1f / 60f, EngineInput(moveY = -1f))
        assertEquals(PlayerFacing.UP, engine.player.facing)

        engine.updateFrame(1f / 60f, EngineInput(moveX = -1f))
        assertEquals(PlayerFacing.LEFT, engine.player.facing)

        engine.updateFrame(1f / 60f, EngineInput(moveY = 1f))
        assertEquals(PlayerFacing.DOWN, engine.player.facing)

        engine.updateFrame(1f / 60f, EngineInput())
        assertEquals(PlayerFacing.DOWN, engine.player.facing)
    }

    @Test
    fun interactionCanBeTriggeredAgainAfterTheSurfaceReleasesInput() {
        val engine = GameEngine()
        engine.player.x = 1.5f
        engine.player.y = 8.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(1, engine.inventory.count(ItemType.WOOD))

        engine.releaseInputState()
        engine.player.x = 4.5f
        engine.player.y = 7.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = true))

        assertEquals(1, engine.inventory.count(ItemType.METAL))
    }

    @Test
    fun characterStatisticsTrackCollectionMovementTimeAndVisitedZones() {
        val engine = GameEngine()
        engine.player.x = 2.5f
        engine.player.y = 8.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(1, engine.statistics.snapshot().resourcesCollected)

        engine.changeZone(WorldZone.FOREST)
        repeat(65) {
            engine.updateFrame(1f / 60f, EngineInput(moveX = 1f))
        }
        val stats = engine.statistics.snapshot()
        assertTrue(stats.distanceTravelled > 0f)
        assertTrue(stats.expeditionMinutes > 0L)
        assertTrue(WorldZone.FOREST in stats.visitedZones)
    }

    @Test
    fun storageCanOnlyBeUsedNearbyAndTransfersItemsSafely() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 6)

        assertFalse(engine.canAccessStorage())
        assertFalse(engine.transferToStorage(ItemType.WOOD))

        engine.player.x = 13.5f
        engine.player.y = 6.5f
        assertTrue(engine.canAccessStorage())
        assertTrue(engine.transferToStorage(ItemType.WOOD))
        assertEquals(0, engine.inventory.count(ItemType.WOOD))
        assertEquals(6, engine.bunkerStorage.count(ItemType.WOOD))
    }

    @Test
    fun sleepRequiresBeingNearTheBunkerBed() {
        val engine = GameEngine()
        assertFalse(engine.canSleep())

        engine.player.x = 2.5f
        engine.player.y = 2.5f
        assertTrue(engine.canSleep())
    }

    @Test
    fun collectingResourcesAndChangingZonesConsumeGameTime() {
        val engine = GameEngine()
        engine.player.x = 2.5f
        engine.player.y = 8.5f
        val start = engine.worldClock.totalMinutes

        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(start + GameEngine.RESOURCE_GATHER_MINUTES, engine.worldClock.totalMinutes)
        assertEquals(1, engine.inventory.count(ItemType.WOOD))

        engine.changeZone(WorldZone.BUNKER)
        engine.player.x = 8.5f
        engine.player.y = 2.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.FOREST, engine.zone)
        assertEquals(start + GameEngine.RESOURCE_GATHER_MINUTES + GameEngine.ZONE_TRAVEL_MINUTES,
            engine.worldClock.totalMinutes)
    }

    @Test
    fun inventorySurvivesRealExitInteractionsAcrossAllZones() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 7)
        engine.inventory.add(ItemType.WATER, 3)

        fun interactAt(x: Float, y: Float) {
            engine.player.x = x
            engine.player.y = y
            engine.updateFrame(1f / 60f, EngineInput(interact = false))
            engine.updateFrame(1f / 60f, EngineInput(interact = true))
            assertEquals(7, engine.inventory.count(ItemType.WOOD))
            assertEquals(3, engine.inventory.count(ItemType.WATER))
        }

        interactAt(8.5f, 2.5f) // bunker -> forest
        assertEquals(WorldZone.FOREST, engine.zone)
        interactAt(1.5f, 12.5f) // forest -> ruins
        assertEquals(WorldZone.RUINS, engine.zone)
        interactAt(38.5f, 12.5f) // ruins -> forest
        assertEquals(WorldZone.FOREST, engine.zone)
        interactAt(38.5f, 12.5f) // forest -> road
        assertEquals(WorldZone.ROAD, engine.zone)
        interactAt(1.5f, 12.5f) // road -> forest
        assertEquals(WorldZone.FOREST, engine.zone)
        interactAt(20.5f, 22.5f) // forest -> bunker
        assertEquals(WorldZone.BUNKER, engine.zone)
    }

    @Test
    fun craftedItemsCannotBeCollectedAsWorldResourceNodes() {
        val map = TileMap()
        assertFalse(map.collectResource(ResourceLocation(1, 8, ItemType.MEDICAL_KIT)))
        assertFalse(map.collectResource(ResourceLocation(1, 8, ItemType.BANDAGE)))
        assertEquals(TileType.RESOURCE_WOOD, map[1, 8])
    }

    @Test
    fun fatalFoodActionRollsBackAndReportsFailure() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.CANNED_FOOD, 1)
        engine.sleepUntilMorning()
        engine.needs.restore(SurvivalNeedsSnapshot(0.1f, 0f, 0f, 80f))

        assertFalse(engine.consumeFood())
        assertEquals(1, engine.inventory.count(ItemType.CANNED_FOOD))
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

    @Test
    fun inventoryPersistsAcrossZoneTransitionsAndSaveRestore() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 7)
        engine.inventory.add(ItemType.WATER, 3)

        listOf(WorldZone.FOREST, WorldZone.RUINS, WorldZone.ROAD, WorldZone.BUNKER).forEach { zone ->
            engine.changeZone(zone)
            assertEquals(7, engine.inventory.count(ItemType.WOOD))
            assertEquals(3, engine.inventory.count(ItemType.WATER))
        }

        val snapshot = engine.createSaveSnapshot()
        engine.inventory.remove(ItemType.WOOD, 7)
        engine.inventory.remove(ItemType.WATER, 3)
        engine.restoreSaveSnapshot(snapshot)

        assertEquals(7, engine.inventory.count(ItemType.WOOD))
        assertEquals(3, engine.inventory.count(ItemType.WATER))
        assertEquals(WorldZone.BUNKER, engine.zone)
    }
}
