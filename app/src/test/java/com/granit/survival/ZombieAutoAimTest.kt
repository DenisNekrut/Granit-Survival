package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ZombieAutoAimTest {
    @Test
    fun nearestVisibleInfectedWithinSevenTilesBecomesAutoAimTarget() {
        val map = TileMap(width = 20, height = 12, zone = WorldZone.FOREST)
        val player = PlayerState(x = 2.5f, y = 2.5f)
        val nearer = ZombieActor(x = 6.5f, y = 2.5f)
        val farther = ZombieActor(x = 9.5f, y = 2.5f)

        assertSame(nearer, ZombieSystem.findVisibleTarget(listOf(farther, nearer), player, map))
        assertSame(farther, ZombieSystem.findVisibleTarget(listOf(farther), player, map))
    }

    @Test
    fun infectedBeyondSevenTilesDoesNotShowAnAimTarget() {
        val map = TileMap(width = 20, height = 12, zone = WorldZone.FOREST)
        val player = PlayerState(x = 2.5f, y = 2.5f)
        val distant = ZombieActor(x = 10.5f, y = 2.5f)

        assertNull(ZombieSystem.findVisibleTarget(listOf(distant), player, map))
    }

    @Test
    fun solidWallBlocksAutoAimEvenWhenInRange() {
        val map = TileMap(width = 40, height = 24, zone = WorldZone.BUNKER)
        val player = PlayerState(x = 10.5f, y = 8.5f)
        val infected = ZombieActor(x = 14.5f, y = 8.5f)

        assertFalse(map.hasLineOfSight(player.x, player.y, infected.x, infected.y))
        assertNull(ZombieSystem.findVisibleTarget(listOf(infected), player, map))
    }

    @Test
    fun gameEngineOnlyExposesAutoAimInOutdoorZonesAndDropsItOutOfRange() {
        val engine = GameEngine()
        assertNull(engine.autoAimTarget)
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = 20.5f
        engine.player.y = 12.5f
        val infected = ZombieActor(x = 26.5f, y = 12.5f)
        engine.zombies.clear()
        engine.zombies += infected

        assertSame(infected, engine.autoAimTarget)
        infected.x = 28.5f
        assertNull(engine.autoAimTarget)
    }

    @Test
    fun autoAimPulseHitsOnlyTheSelectedActor() {
        val selected = ZombieActor(x = 6.5f, y = 2.5f)
        val other = ZombieActor(x = 7.5f, y = 2.5f)
        val group = mutableListOf(selected, other)

        assertSame(selected, ZombieSystem.pulseHitTarget(group, selected))
        assertEquals(4f, selected.health, 0.001f)
        assertEquals(8f, other.health, 0.001f)
        assertFalse(group.isEmpty())
    }
}
