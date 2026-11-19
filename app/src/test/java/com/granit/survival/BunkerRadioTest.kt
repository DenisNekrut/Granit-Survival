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
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        assertTrue(engine.canUseRadio())
        engine.changeZone(WorldZone.FOREST)
        assertFalse(engine.canUseRadio())
    }

    @Test
    fun oneBroadcastIsAvailablePerDayAndConsumesTime() {
        val engine = GameEngine()
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        val start = engine.worldClock.totalMinutes
        val event = engine.listenToRadio()
        assertNotNull(event)
        assertTrue(event!!.description.contains("туман")) // day 1 radio previews day 2 weather
        assertEquals(start + 10, engine.worldClock.totalMinutes)
        assertNull(engine.listenToRadio())
        assertFalse(engine.canUseRadio())
    }

    @Test
    fun broadcastNearMidnightDoesNotConsumeTheNextDaysTransmission() {
        val engine = GameEngine()
        engine.worldClock.advanceMinutes(23 * 60 + 55 - 7 * 60)
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        assertNotNull(engine.listenToRadio())
        assertEquals(2, engine.worldClock.day)
        assertTrue(engine.canUseRadio())
    }

    @Test
    fun listeningStateRollsBackWithWakeCheckpoint() {
        val engine = GameEngine()
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        // Capture a powered wake checkpoint at the radio so this test isolates radio rollback.
        engine.checkpointSystem.capture(engine)
        assertNotNull(engine.listenToRadio())
        assertTrue(engine.respawnAtWakeCheckpoint())
        // The checkpoint restores both the unspent daily broadcast and generator fuel.
        assertTrue(engine.canUseRadio())
    }
}
