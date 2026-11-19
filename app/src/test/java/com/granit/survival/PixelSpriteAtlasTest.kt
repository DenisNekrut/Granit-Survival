package com.granit.survival

import org.junit.Assert.assertTrue
import org.junit.Test

class PixelSpriteAtlasTest {
    @Test
    fun allSpritesHaveConsistentFramesAndDefinedPaletteSymbols() {
        assertTrue(PixelSpriteAtlas.validate().joinToString(), PixelSpriteAtlas.validate().isEmpty())
        assertTrue(PixelSpriteAtlas.allIds().containsAll(
            setOf("player", "player_back", "player_side", "wood", "metal", "fabric", "food", "water", "bandage", "bed", "workbench", "storage", "bookshelf", "radio", "tree", "rubble", "road_barrier", "bunker_panel", "bush", "debris", "streetlamp", "floor_grate", "crate", "barrel", "wreck")
        ))
    }

    @Test
    fun playerHasFourDirectionalWalkFrames() {
        listOf("player", "player_back", "player_side").forEach { id ->
            assertTrue("$id should provide a four-frame walk cycle",
                requireNotNull(PixelSpriteAtlas.sprite(id)).frames.size == 4)
        }
    }

    @Test
    fun environmentSpritesFitTheGeneratedAtlasCell() {
        listOf("tree", "rubble", "road_barrier", "bunker_panel", "bush", "debris", "streetlamp", "floor_grate", "crate", "barrel", "wreck").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id must fit the 16x16 atlas cell", sprite.frames.all { frame ->
                frame.size <= 16 && frame.all { it.length <= 16 }
            })
        }
    }
}
