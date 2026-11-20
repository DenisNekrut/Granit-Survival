package com.granit.survival

import org.junit.Assert.assertTrue
import org.junit.Test

class PixelSpriteAtlasTest {
    @Test
    fun allSpritesHaveConsistentFramesAndDefinedPaletteSymbols() {
        assertTrue(PixelSpriteAtlas.validate().joinToString(), PixelSpriteAtlas.validate().isEmpty())
        assertTrue(PixelSpriteAtlas.allIds().containsAll(
            setOf("item_wood", "item_metal", "item_fabric", "item_food", "item_water", "item_medical_kit", "item_bandage", "player", "player_back", "player_side_left", "player_side_right", "exit_hatch", "ladder_hatch", "vault_door", "control_console", "dining_table", "generator", "medical_station", "locker", "bookshelf", "tree", "bush", "rubble", "streetlamp", "ceiling_lamp", "crate", "storage", "radio", "bunker_panel", "road_barrier", "barrel", "wreck", "debris", "wood", "metal", "fabric", "food", "water", "bandage", "bed", "workbench", "floor_grate", "grass_patch", "fallen_log", "metal_scrap", "puddle", "road_sign", "infected_shambler", "infected_runner", "infected_brute")
        ))
    }

    @Test
    fun everySpriteUsesTheReplacementColdSurvivalPalette() {
        PixelSpriteAtlas.allIds().forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            sprite.palette.values.forEach { color ->
                assertTrue("$id red channel must be normalized", color[0] in 0f..1f)
                assertTrue("$id green channel must be normalized", color[1] in 0f..1f)
                assertTrue("$id blue channel must be normalized", color[2] in 0f..1f)
            }
        }
        val playerCloth = requireNotNull(PixelSpriteAtlas.sprite("player")).palette.getValue('A')
        assertTrue("Character cloth must no longer use the former tan source colour",
            kotlin.math.abs(playerCloth[0] - 0.64f) > 0.08f ||
                kotlin.math.abs(playerCloth[1] - 0.48f) > 0.08f ||
                kotlin.math.abs(playerCloth[2] - 0.25f) > 0.08f)
        val water = requireNotNull(PixelSpriteAtlas.sprite("item_water")).palette.getValue('W')
        assertTrue("Water must use the new steel-blue family", water[2] > water[0])
    }

    @Test
    fun everyWorldSpriteFitsThe128PixelMapTile() {
        assertTrue("Map tile standard must be 128 logical pixels", SpriteWorldMetrics.TILE_PIXELS == 128)
        PixelSpriteAtlas.allIds().forEach { id ->
            val footprint = SpriteWorldMetrics.footprint(id, 1f)
            assertTrue("$id width must fit one 128px tile", footprint.width <= 1.0001f)
            assertTrue("$id height must fit one 128px tile", footprint.height <= 1.0001f)
            assertTrue("$id longest side should use the tile extent",
                kotlin.math.abs(maxOf(footprint.width, footprint.height) - 1f) < 0.0001f)
        }
    }

    @Test
    fun spriteDimensionsAreNormalizedIndependentlyOfSourceResolution() {
        val player = SpriteWorldMetrics.footprint("player", 1f)
        assertTrue("32x48 player is fitted proportionally inside a 128x128 tile",
            kotlin.math.abs(player.width - (32f / 48f)) < 0.0001f &&
                kotlin.math.abs(player.height - 1f) < 0.0001f)
        val prop = SpriteWorldMetrics.footprint("tree", 1f)
        assertTrue("32x32 props occupy one square tile", prop.width == 1f && prop.height == 1f)
        val infected = SpriteWorldMetrics.footprint("infected_shambler", 1f)
        assertTrue("12x24 infected sprite is normalized by its longer side",
            kotlin.math.abs(infected.width - 0.5f) < 0.0001f && infected.height == 1f)
        val halfTile = SpriteWorldMetrics.footprint("tree", 0.5f)
        assertTrue("Explicit half-tile sprites scale proportionally",
            halfTile.width == 0.5f && halfTile.height == 0.5f)
    }

    @Test
    fun playerHasFourDirectionalWalkFrames() {
        listOf("player", "player_back", "player_side_left", "player_side_right").forEach { id ->
            assertTrue("$id should provide a four-frame walk cycle",
                requireNotNull(PixelSpriteAtlas.sprite(id)).frames.size == 4)
        }
    }

    @Test
    fun generatedPlayerFramesUseTheExpectedHighResolutionCell() {
        listOf("player", "player_back", "player_side_left", "player_side_right").forEach { id ->
            val frames = requireNotNull(PixelSpriteAtlas.sprite(id)).frames
            assertTrue("$id should have 4 frames", frames.size == 4)
            assertTrue("$id frames should be 32x48", frames.all { frame ->
                frame.size == 48 && frame.all { it.length == 32 }
            })
        }
    }

    @Test
    fun environmentSpritesFitTheGeneratedAtlasCell() {
        listOf("exit_hatch", "ladder_hatch", "vault_door", "control_console", "dining_table", "ceiling_lamp").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id must fit the 16x16 atlas cell", sprite.frames.all { frame ->
                frame.size <= 16 && frame.all { it.length <= 16 }
            })
        }
        listOf("tree", "bush", "rubble", "streetlamp", "road_sign", "wreck", "crate", "barrel", "road_barrier",
            "bunker_panel", "debris", "floor_grate", "grass_patch", "fallen_log", "metal_scrap", "puddle").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id should use detailed 32x32 environment art", sprite.frames.all { frame ->
                frame.size == 32 && frame.all { it.length == 32 }
            })
        }
        listOf("bed", "generator", "medical_station", "workbench", "storage", "bookshelf", "radio").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id should use the detailed 32x32 bunker art", sprite.frames.all { frame ->
                frame.size == 32 && frame.all { it.length == 32 }
            })
        }
    }


    @Test
    fun inventoryIconsHaveUniquePixelArtFrames() {
        val ids = listOf("item_wood", "item_metal", "item_fabric", "item_food", "item_water", "item_medical_kit", "item_bandage")
        ids.forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id must be a 16x16 icon", sprite.frames.single().size == 16 &&
                sprite.frames.single().all { it.length == 16 })
            assertTrue("$id must define a palette", sprite.palette.isNotEmpty())
        }
        assertTrue("Inventory icons should be visually distinct",
            ids.map { PixelSpriteAtlas.sprite(it)?.frames?.single() }.distinct().size == ids.size)
    }

    @Test
    fun infectedArchetypesHaveDistinctTwoFramePixelSilhouettes() {
        val ids = listOf("infected_shambler", "infected_runner", "infected_brute")
        ids.forEach { id ->
            val frames = requireNotNull(PixelSpriteAtlas.sprite(id)).frames
            assertTrue("$id should have two animation frames", frames.size == 2)
            assertTrue("$id frames should be 12x24", frames.all { frame ->
                frame.size == 24 && frame.all { it.length == 12 }
            })
        }
        assertTrue("infected silhouettes should not all be identical",
            ids.map { PixelSpriteAtlas.sprite(it)?.frames?.first() }.distinct().size == ids.size)
    }
}
