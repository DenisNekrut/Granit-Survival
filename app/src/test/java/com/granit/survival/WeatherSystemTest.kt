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
}
