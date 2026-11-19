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
    fun eachZoneHasAVariedSparseDecorationSet() {
        val expected = mapOf(
            WorldZone.BUNKER to setOf("crate", "floor_grate"),
            WorldZone.FOREST to setOf("bush", "tree", "grass_patch", "fallen_log"),
            WorldZone.RUINS to setOf("barrel", "debris", "metal_scrap", "puddle"),
            WorldZone.ROAD to setOf("streetlamp", "wreck", "road_sign", "puddle")
        )
        WorldZone.values().forEach { zone ->
            val selected = (0 until 100).flatMap { x ->
                (0 until 100).mapNotNull { y -> ZonePropSelector.spriteFor(zone, x, y) }
            }
            assertTrue("Expected props for $zone", selected.isNotEmpty())
            selected.toSet().forEach { spriteId ->
                assertTrue("Missing atlas sprite $spriteId in $zone", PixelSpriteAtlas.sprite(spriteId) != null)
            }
            assertTrue("Decoration density should stay sparse for $zone", selected.size < 1_500)
            assertEquals(expected.getValue(zone), selected.toSet())
        }
    }
}
