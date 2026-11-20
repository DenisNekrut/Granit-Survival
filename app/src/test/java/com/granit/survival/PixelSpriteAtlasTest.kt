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
    fun wallSpriteFootprintsFitSingleOccupiedMapCells() {
        val wallSprites = listOf(
            "bunker_panel", "bunker_panel_rusted",
            "tree", "tree_dark", "tree_dead",
            "rubble", "rubble_brick", "debris",
            "road_barrier", "road_barrier_faded"
        )
        wallSprites.forEach { id ->
            val footprint = SpriteWorldMetrics.footprint(id, 0.052f)
            assertTrue("$id wall width must fit one map cell", footprint.width <= 1.001f)
            assertTrue("$id wall height must fit one map cell", footprint.height <= 1.001f)
        }
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
    fun doubledWorldSpriteResolutionDoesNotDoubleItsLogicalFootprint() {
        val highResolutionIds = listOf(
            "player", "player_back", "player_side_left", "player_side_right",
            "tree", "bush", "rubble", "streetlamp", "road_sign", "wreck",
            "crate", "barrel", "road_barrier", "bunker_panel", "debris", "floor_grate",
            "grass_patch", "fallen_log", "metal_scrap", "puddle",
            "bed", "generator", "medical_station", "workbench", "storage", "bookshelf", "radio"
        )
        highResolutionIds.forEach { id ->
            assertTrue("$id must compensate for 2x source density",
                PixelSpriteAtlas.worldDensityScale(id) == 0.5f)
        }
        listOf("exit_hatch", "ladder_hatch", "infected_shambler").forEach { id ->
            assertTrue("$id keeps its original world pixel scale",
                PixelSpriteAtlas.worldDensityScale(id) == 1f)
        }
        val formerFootprint = 16f * 0.0625f
        val detailedFootprint = 32f * 0.0625f * PixelSpriteAtlas.worldDensityScale("tree")
        assertTrue("Higher resolution must preserve the original tile footprint",
            kotlin.math.abs(formerFootprint - detailedFootprint) < 0.0001f)
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
