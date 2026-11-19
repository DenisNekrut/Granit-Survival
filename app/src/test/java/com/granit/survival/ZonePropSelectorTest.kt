package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZonePropSelectorTest {
    @Test
    fun selectionIsDeterministicForTheSameZoneAndTile() {
        WorldZone.values().forEach { zone ->
            for (x in 0 until 40) for (y in 0 until 30) {
                assertEquals(
                    ZonePropSelector.spriteFor(zone, x, y),
                    ZonePropSelector.spriteFor(zone, x, y)
                )
            }
        }
    }

    @Test
    fun eachZoneHasA variedSparseDecorationSet() {
        val expected = mapOf(
            WorldZone.BUNKER to setOf("crate"),
            WorldZone.FOREST to setOf("bush", "tree"),
            WorldZone.RUINS to setOf("barrel", "debris"),
            WorldZone.ROAD to setOf("streetlamp", "wreck")
        )
        WorldZone.values().forEach { zone ->
            val selected = (0 until 100).flatMap { x ->
                (0 until 100).mapNotNull { y -> ZonePropSelector.spriteFor(zone, x, y) }
            }
            assertTrue("Expected props for $zone", selected.isNotEmpty())
            assertTrue("Decoration density should stay sparse for $zone", selected.size < 1_500)
            assertEquals(expected.getValue(zone), selected.toSet())
        }
    }
}
