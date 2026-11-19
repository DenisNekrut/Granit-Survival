package com.granit.survival

import org.junit.Assert.assertTrue
import org.junit.Test

class PixelSpriteAtlasTest {
    @Test
    fun allSpritesHaveConsistentFramesAndDefinedPaletteSymbols() {
        assertTrue(PixelSpriteAtlas.validate().joinToString(), PixelSpriteAtlas.validate().isEmpty())
        assertTrue(PixelSpriteAtlas.allIds().containsAll(
            setOf("player", "player_back", "player_side_left", "player_side_right", "exit_hatch", "ladder_hatch", "vault_door", "control_console", "dining_table", "generator", "medical_station", "locker", "bookshelf", "tree", "bush", "rubble", "streetlamp", "crate", "storage", "radio", "bunker_panel", "road_barrier", "barrel", "wreck", "debris", "wood", "metal", "fabric", "food", "water", "bandage", "bed", "workbench", "floor_grate")
        ))
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
            assertTrue("$id frames should be 16x24", frames.all { frame ->
                frame.size == 24 && frame.all { it.length == 16 }
            })
        }
    }

    @Test
    fun environmentSpritesFitTheGeneratedAtlasCell() {
        listOf("tree", "rubble", "road_barrier", "bunker_panel", "bush", "debris", "streetlamp", "floor_grate", "crate", "barrel", "wreck", "exit_hatch", "ladder_hatch", "vault_door", "control_console", "dining_table", "generator", "medical_station", "workbench", "locker", "bookshelf").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id must fit the 16x16 atlas cell", sprite.frames.all { frame ->
                frame.size <= 16 && frame.all { it.length <= 16 }
            })
        }
    }
}
