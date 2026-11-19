package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTimeAndZombieTest {
    @Test
    fun worldClockContinuesDuringOrdinarySimulationFrames() {
        val engine = GameEngine()
        val start = engine.worldClock.totalMinutes
        repeat(4) { engine.updateFrame(0.25f, EngineInput()) }
        assertEquals(start + 1L, engine.worldClock.totalMinutes)
    }

    @Test
    fun readingBookConsumesItsFullReadingDuration() {
        val engine = GameEngine()
        engine.player.x = 7.5f
        engine.player.y = 2.5f
        val start = engine.worldClock.totalMinutes

        assertTrue(engine.readBook("old_novel"))
        assertEquals(start + BookCatalog.byId("old_novel")!!.readMinutes, engine.worldClock.totalMinutes)
        assertTrue(engine.bookLibrary.hasRead("old_novel"))
    }

    @Test
    fun storageTransfersConsumeTimeOnlyWhenTransferSucceeds() {
        val engine = GameEngine()
        engine.player.x = 4.5f
        engine.player.y = 2.5f
        engine.inventory.add(ItemType.WOOD, 2)
        val start = engine.worldClock.totalMinutes

        assertTrue(engine.transferToStorage(ItemType.WOOD, 2))
        assertEquals(start + 1L, engine.worldClock.totalMinutes)
        assertFalse(engine.transferToStorage(ItemType.WOOD, 1))
        assertEquals(start + 1L, engine.worldClock.totalMinutes)
    }

    @Test
    fun infectedSpawnOnlyInOutdoorZones() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        val forest = createZoneMap(WorldZone.FOREST)
        assertTrue(ZombieSystem.spawn(WorldZone.BUNKER, bunker, 9.5f, 7f).isEmpty())
        assertTrue(ZombieSystem.spawn(WorldZone.FOREST, forest, 20.5f, 12.5f).isNotEmpty())
        assertTrue(ZombieSystem.spawn(WorldZone.RUINS, createZoneMap(WorldZone.RUINS, 91L), 34.5f, 12.5f).isNotEmpty())
        assertTrue(ZombieSystem.spawn(WorldZone.ROAD, createZoneMap(WorldZone.ROAD, 92L), 5.5f, 12.5f).isNotEmpty())
    }

    @Test
    fun outdoorZonesSpawnDifferentInfectedArchetypes() {
        val forest = ZombieSystem.spawn(WorldZone.FOREST, createZoneMap(WorldZone.FOREST), 20.5f, 12.5f)
        val ruins = ZombieSystem.spawn(WorldZone.RUINS, createZoneMap(WorldZone.RUINS, 91L), 34.5f, 12.5f)
        val road = ZombieSystem.spawn(WorldZone.ROAD, createZoneMap(WorldZone.ROAD, 92L), 5.5f, 12.5f)

        assertTrue(forest.any { it.variant == InfectedVariant.RUNNER })
        assertTrue(ruins.any { it.variant == InfectedVariant.BRUTE })
        assertTrue(road.any { it.variant == InfectedVariant.RUNNER })
        assertTrue(road.any { it.variant == InfectedVariant.BRUTE })
    }

    @Test
    fun infectedVariantsHaveDifferentMovementAndDurability() {
        assertTrue(InfectedVariant.RUNNER.moveSpeed > InfectedVariant.SHAMBLER.moveSpeed)
        assertTrue(InfectedVariant.BRUTE.health > InfectedVariant.SHAMBLER.health)
        assertTrue(InfectedVariant.BRUTE.moveSpeed < InfectedVariant.SHAMBLER.moveSpeed)
    }

    @Test
    fun pulseCarbineDisablesAnInfectedInTwoHits() {
        val player = PlayerState(x = 10f, y = 10f, facing = PlayerFacing.RIGHT)
        val infected = ZombieActor(x = 12f, y = 10f)
        val group = mutableListOf(infected)

        assertTrue(ZombieSystem.pulseHit(group, player, player.facing) === infected)
        assertEquals(4f, infected.health, 0.001f)
        assertTrue(ZombieSystem.pulseHit(group, player, player.facing) === infected)
        assertTrue(group.isEmpty())
    }
}
