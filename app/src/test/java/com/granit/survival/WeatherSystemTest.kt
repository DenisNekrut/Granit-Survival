package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSystemTest {
    @Test
    fun weatherCycleIsDeterministicAndRepeats() {
        assertEquals(WeatherType.CLEAR, WeatherSystem.forDay(1))
        assertEquals(WeatherType.FOG, WeatherSystem.forDay(2))
        assertEquals(WeatherType.RAIN, WeatherSystem.forDay(3))
        assertEquals(WeatherType.COLD, WeatherSystem.forDay(4))
        assertEquals(WeatherType.OVERCAST, WeatherSystem.forDay(5))
        assertEquals(WeatherSystem.forDay(1), WeatherSystem.forDay(6))
    }

    @Test
    fun weatherAffectsOutdoorMovementButNotBunkerMovement() {
        val engine = GameEngine()
        engine.worldClock.advanceMinutes(24 * 60)
        assertEquals(WeatherType.FOG, engine.weather)
        assertEquals(1f, engine.weatherMovementMultiplier)
        engine.changeZone(WorldZone.FOREST)
        assertTrue(engine.weatherMovementMultiplier < 1f)
        assertEquals(WeatherType.FOG.outdoorMovementMultiplier, engine.weatherMovementMultiplier)
    }

    @Test
    fun checkpointRollbackRestoresWeatherByRestoringDay() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        assertEquals(WeatherType.FOG, engine.weather)
        engine.worldClock.advanceMinutes(24 * 60)
        assertEquals(WeatherType.RAIN, engine.weather)
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(WeatherType.FOG, engine.weather)
    }

    @Test
    fun weatherParticlePositionsAreDeterministicAndStayInsideMap() {
        val first = WeatherParticleField.position(17, 12.5, 24, 18, 3.2f, -0.18f)
        val repeated = WeatherParticleField.position(17, 12.5, 24, 18, 3.2f, -0.18f)
        assertEquals(first, repeated)
        assertTrue(first.first >= 0f && first.first < 24f)
        assertTrue(first.second >= 0f && first.second < 18f)
    }

    @Test
    fun weatherParticlesMoveWithSimulationTime() {
        val start = WeatherParticleField.position(4, 0.0, 24, 18, 2.5f, 0.12f)
        val later = WeatherParticleField.position(4, 3.0, 24, 18, 2.5f, 0.12f)
        assertTrue(start != later)
    }
}
