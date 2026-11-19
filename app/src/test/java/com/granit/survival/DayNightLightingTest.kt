package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayNightLightingTest {
    @Test
    fun daylightIsBrightestAroundMidday() {
        assertEquals(0f, DayNightLighting.outdoorDarkness(12 * 60), 0.001f)
        assertTrue(DayNightLighting.outdoorDarkness(2 * 60) > 0.60f)
        assertTrue(DayNightLighting.outdoorDarkness(23 * 60) > 0.60f)
    }

    @Test
    fun dawnAndDuskTransitionGradually() {
        val beforeDawn = DayNightLighting.outdoorDarkness(4 * 60 + 30)
        val sunrise = DayNightLighting.outdoorDarkness(6 * 60)
        val morning = DayNightLighting.outdoorDarkness(7 * 60 + 30)
        val sunset = DayNightLighting.outdoorDarkness(18 * 60)
        val evening = DayNightLighting.outdoorDarkness(20 * 60)
        assertTrue(beforeDawn > sunrise)
        assertTrue(sunrise > morning)
        assertTrue(evening > sunset)
        assertTrue(DayNightLighting.twilightTint(6 * 60) > 0f)
        assertTrue(DayNightLighting.twilightTint(18 * 60) > 0f)
    }

    @Test
    fun lightingCurveAcceptsEveryMinuteAndStaysInRange() {
        for (minute in 0 until WorldClock.MINUTES_PER_DAY) {
            val darkness = DayNightLighting.outdoorDarkness(minute)
            assertTrue("darkness out of range at $minute", darkness in 0f..0.62f)
            assertTrue("twilight out of range at $minute", DayNightLighting.twilightTint(minute) in 0f..0.12f)
        }
    }

    @Test
    fun adjacentMinutesDoNotProduceAbruptLightingJumps() {
        for (minute in 0 until WorldClock.MINUTES_PER_DAY - 1) {
            val darknessDelta = kotlin.math.abs(
                DayNightLighting.outdoorDarkness(minute + 1) -
                    DayNightLighting.outdoorDarkness(minute)
            )
            val twilightDelta = kotlin.math.abs(
                DayNightLighting.twilightTint(minute + 1) -
                    DayNightLighting.twilightTint(minute)
            )
            assertTrue("darkness jump at $minute: $darknessDelta", darknessDelta < 0.02f)
            assertTrue("twilight jump at $minute: $twilightDelta", twilightDelta < 0.02f)
        }
    }

    @Test
    fun outOfRangeTimesAreClampedSafely() {
        assertEquals(DayNightLighting.outdoorDarkness(0), DayNightLighting.outdoorDarkness(-100), 0f)
        assertEquals(DayNightLighting.outdoorDarkness(1439), DayNightLighting.outdoorDarkness(9000), 0f)
    }
}
