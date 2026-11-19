package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun eachZoneUsesItsOwnDecorationSet() {
        assertEquals("crate", ZonePropSelector.spriteFor(WorldZone.BUNKER, 0, 0))
        assertEquals("bush", ZonePropSelector.spriteFor(WorldZone.FOREST, 0, 0))
        assertEquals("barrel", ZonePropSelector.spriteFor(WorldZone.RUINS, 0, 0))
        assertEquals("streetlamp", ZonePropSelector.spriteFor(WorldZone.ROAD, 0, 0))
        assertEquals("wreck", ZonePropSelector.spriteFor(WorldZone.ROAD, 1, 5))
        assertNull(ZonePropSelector.spriteFor(WorldZone.BUNKER, 1, 0))
    }
}
