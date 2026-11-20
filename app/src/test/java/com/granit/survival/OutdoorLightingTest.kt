package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutdoorLightingTest {
    @Test
    fun streetlampsDoNotGlowInFullDaylight() {
        val profile = OutdoorLighting.streetlampProfile(12 * 60, WeatherType.CLEAR)
        assertEquals(0f, profile.intensity, 0.001f)
    }

    @Test
    fun streetlampsReachFullIntensityAtNight() {
        val profile = OutdoorLighting.streetlampProfile(0, WeatherType.CLEAR)
        assertTrue(profile.intensity > 0.95f)
        assertTrue(profile.radius in 2.4f..3.5f)
    }

    @Test
    fun weatherReducesStreetlampIntensityWithoutTurningItOff() {
        val clear = OutdoorLighting.streetlampProfile(0, WeatherType.CLEAR)
        val rain = OutdoorLighting.streetlampProfile(0, WeatherType.RAIN)
        val fog = OutdoorLighting.streetlampProfile(0, WeatherType.FOG)
        assertTrue(clear.intensity > rain.intensity)
        assertTrue(clear.intensity > fog.intensity)
        assertTrue(rain.intensity > 0f)
    }

    @Test
    fun onlyIntactStreetlampsEmitLocalLight() {
        assertTrue(OutdoorLighting.isLightSource("streetlamp"))
        assertTrue(OutdoorLighting.isLightSource("streetlamp_weathered"))
        assertFalse(OutdoorLighting.isLightSource("streetlamp_broken"))
        assertFalse(OutdoorLighting.isLightSource("tree"))
        assertFalse(OutdoorLighting.isLightSource(null))
    }

    @Test
    fun streetlampPositionsAreDeterministicAndMatchActualMapProps() {
        val map = createZoneMap(WorldZone.ROAD, seed = 481516L)
        val first = OutdoorLighting.streetlampPositions(map)
        val second = OutdoorLighting.streetlampPositions(map)
        assertEquals(first, second)
        first.forEach { (centerX, centerY) ->
            val tileX = kotlin.math.floor(centerX).toInt()
            val tileY = kotlin.math.floor(centerY - 0.72f).toInt()
            assertTrue(OutdoorLighting.isLightSource(map.environmentPropAt(tileX, tileY)))
        }
    }

    @Test
    fun lightProfileRemainsBoundedAcrossEveryMinuteAndWeather() {
        for (minute in 0 until WorldClock.MINUTES_PER_DAY) {
            WeatherType.values().forEach { weather ->
                val profile = OutdoorLighting.streetlampProfile(minute, weather)
                assertTrue(profile.intensity in 0f..1f)
                assertTrue(profile.radius in 2.4f..3.5f)
            }
        }
    }
}
