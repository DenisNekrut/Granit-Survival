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
            val tileX = kotlin.math.round(centerX - 0.5f).toInt()
            val tileY = kotlin.math.round(centerY - 0.72f).toInt()
            assertTrue(OutdoorLighting.isLightSource(map.environmentPropAt(tileX, tileY)))
        }
    }

    @Test
    fun morningNoonAndEveningShadowsFollowTheSunDirection() {
        val morning = OutdoorLighting.shadowProfile(8 * 60)
        val noon = OutdoorLighting.shadowProfile(12 * 60)
        val evening = OutdoorLighting.shadowProfile(16 * 60)

        assertTrue(morning.offsetX < 0f)
        assertEquals(0f, noon.offsetX, 0.001f)
        assertTrue(evening.offsetX > 0f)
        assertTrue(morning.lengthScale > noon.lengthScale)
        assertTrue(evening.lengthScale > noon.lengthScale)
    }

    @Test
    fun nightShadowsAreShortAndSubdued() {
        val night = OutdoorLighting.shadowProfile(0)
        val day = OutdoorLighting.shadowProfile(12 * 60)
        assertTrue(kotlin.math.abs(night.offsetX) < 0.05f)
        assertTrue(night.opacityScale < day.opacityScale)
        assertTrue(night.lengthScale in 0.7f..1.2f)
    }

    @Test
    fun shadowProfileStaysBoundedAcrossEveryMinute() {
        for (minute in 0 until WorldClock.MINUTES_PER_DAY) {
            val profile = OutdoorLighting.shadowProfile(minute)
            assertTrue(profile.offsetX in -0.19f..0.19f)
            assertTrue(profile.lengthScale in 0.7f..1.2f)
            assertTrue(profile.opacityScale in 0.45f..0.95f)
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
