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
    fun everyZoneUsesMultipleVariantsWithoutIncreasingDecorationDensity() {
        val expected = mapOf(
            WorldZone.BUNKER to setOf("crate", "crate_weathered", "crate_painted", "floor_grate", "floor_grate_rust"),
            WorldZone.FOREST to setOf("bush", "bush_dry", "bush_lush", "tree", "tree_autumn", "tree_dead", "grass_patch", "grass_patch_dry", "fallen_log", "fallen_log_mossy"),
            WorldZone.RUINS to setOf("barrel", "barrel_rust", "barrel_dark", "debris", "debris_light", "metal_scrap", "metal_scrap_copper", "puddle", "puddle_ice"),
            WorldZone.ROAD to setOf("streetlamp", "streetlamp_broken", "wreck", "wreck_rust", "wreck_blue", "road_sign", "road_sign_faded", "puddle", "puddle_ice")
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

    @Test
    fun wallTilesAlsoUseDeterministicVisualVariants() {
        WorldZone.values().forEach { zone ->
            val ids = (0 until 80).flatMap { x ->
                (0 until 80).map { y -> ZonePropSelector.wallSpriteFor(zone, x, y) }
            }.toSet()
            assertTrue("Expected wall variants for $zone", ids.size >= 2)
            ids.forEach { id ->
                assertTrue("Missing wall variant $id", PixelSpriteAtlas.sprite(id) != null)
            }
        }
    }

    @Test
    fun repeatedCoordinatesDoNotRandomlyChangeTheirVisualVariant() {
        WorldZone.values().forEach { zone ->
            val first = ZonePropSelector.spriteFor(zone, 17, 23)
            repeat(20) {
                assertEquals(first, ZonePropSelector.spriteFor(zone, 17, 23))
            }
        }
    }
}
