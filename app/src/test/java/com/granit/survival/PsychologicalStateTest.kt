package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PsychologicalStateTest {
    @Test
    fun expeditionAndMovementAffectIndependentIndicators() {
        val state = PsychologicalState()
        val initial = state.snapshot()
        state.onZoneChange(WorldZone.BUNKER, WorldZone.FOREST)
        state.advanceAwake(60, moving = true, zone = WorldZone.FOREST)
        assertTrue(state.stress > initial.stress)
        assertTrue(state.loneliness > initial.loneliness)
        assertTrue(state.fatigue > initial.fatigue)
        assertTrue(state.morale < initial.morale)
    }

    @Test
    fun sleepAndReturnToBunkerImproveRecoveryIndicators() {
        val state = PsychologicalState()
        state.onZoneChange(WorldZone.BUNKER, WorldZone.FOREST)
        state.advanceAwake(120, moving = true, zone = WorldZone.FOREST)
        val beforeSleep = state.snapshot()
        state.sleep(480)
        assertTrue(state.stress < beforeSleep.stress)
        assertTrue(state.fatigue < beforeSleep.fatigue)
        assertTrue(state.morale > beforeSleep.morale)
        state.onZoneChange(WorldZone.FOREST, WorldZone.BUNKER)
        assertTrue(state.stress < beforeSleep.stress)
    }

    @Test
    fun snapshotRestoresAllFourValuesWithinBounds() {
        val state = PsychologicalState()
        state.restore(PsychologicalStateSnapshot(120f, -10f, 40f, 50f))
        assertEquals(100f, state.stress)
        assertEquals(0f, state.loneliness)
        assertEquals(40f, state.fatigue)
        assertEquals(50f, state.morale)
    }
}
