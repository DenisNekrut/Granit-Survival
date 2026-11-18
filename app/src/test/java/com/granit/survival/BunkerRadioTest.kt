package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerRadioTest {
    @Test
    fun radioRequiresBunkerAndNearbyRadioObject() {
        val engine = GameEngine()
        assertFalse(engine.canUseRadio())
        engine.player.x = 24.5f
        engine.player.y = 2.5f
        assertTrue(engine.canUseRadio())
        engine.changeZone(WorldZone.FOREST)
        assertFalse(engine.canUseRadio())
    }

    @Test
    fun oneBroadcastIsAvailablePerDayAndConsumesTime() {
        val engine = GameEngine()
        engine.player.x = 24.5f
        engine.player.y = 2.5f
        val start = engine.worldClock.totalMinutes
        val event = engine.listenToRadio()
        assertNotNull(event)
        assertEquals(start + 10, engine.worldClock.totalMinutes)
        assertNull(engine.listenToRadio())
        assertFalse(engine.canUseRadio())
    }

    @Test
    fun broadcastNearMidnightDoesNotConsumeTheNextDaysTransmission() {
        val engine = GameEngine()
        engine.worldClock.advanceMinutes(23 * 60 + 55 - 7 * 60)
        engine.player.x = 24.5f
        engine.player.y = 2.5f
        assertNotNull(engine.listenToRadio())
        assertEquals(2, engine.worldClock.day)
        assertTrue(engine.canUseRadio())
    }

    @Test
    fun listeningStateRollsBackWithWakeCheckpoint() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        engine.player.x = 24.5f
        engine.player.y = 2.5f
        assertNotNull(engine.listenToRadio())
        assertTrue(engine.respawnAtWakeCheckpoint())
        // Respawn restores the bunker checkpoint position; walk back to the radio before checking access.
        engine.player.x = 24.5f
        engine.player.y = 2.5f
        assertTrue(engine.canUseRadio())
    }
}
