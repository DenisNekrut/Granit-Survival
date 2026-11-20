package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GranitWorldPaletteTest {
    @Test
    fun everyMaterialRampHasThreeValidRgbStops() {
        GranitWorldPalette.materialRamps.keys.forEach { name ->
            val ramp = GranitWorldPalette.materialRamp(name)
            listOf(ramp.shadow, ramp.mid, ramp.highlight).forEach { color ->
                assertEquals(3, color.size)
                color.forEach { assertTrue(it in 0f..1f) }
            }
        }
    }

    @Test
    fun eachZoneHasItsOwnFloorAndAccent() {
        WorldZone.values().forEach { zone ->
            assertEquals(3, GranitWorldPalette.zoneFloor(zone).size)
            assertEquals(3, GranitWorldPalette.zoneAccent(zone).size)
        }
        assertTrue(WorldZone.values().map { GranitWorldPalette.zoneFloor(it).toList() }.toSet().size > 1)
    }

    @Test
    fun weatherGradesAreBoundedAndClearWeatherIsNeutral() {
        assertTrue(GranitWorldPalette.weatherOverlay(WeatherType.CLEAR).all { it == 0f })
        WeatherType.values().forEach { weather ->
            val overlay = GranitWorldPalette.weatherOverlay(weather)
            assertEquals(4, overlay.size)
            overlay.forEach { assertTrue(it in 0f..1f) }
            assertTrue(overlay[3] <= 0.25f)
        }
        assertTrue(GranitWorldPalette.weatherOverlay(WeatherType.RAIN)[3] >
            GranitWorldPalette.weatherOverlay(WeatherType.OVERCAST)[3])
    }

    @Test
    fun returnedMaterialRampsCannotMutateCanonicalPalette() {
        val first = GranitWorldPalette.materialRamp("steel")
        first.mid[0] = 1f
        assertTrue(GranitWorldPalette.materialRamp("steel").mid[0] < 1f)
    }
}
