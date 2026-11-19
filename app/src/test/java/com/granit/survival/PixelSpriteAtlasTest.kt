package com.granit.survival

import org.junit.Assert.assertTrue
import org.junit.Test

class PixelSpriteAtlasTest {
    @Test
    fun allSpritesHaveConsistentFramesAndDefinedPaletteSymbols() {
        assertTrue(PixelSpriteAtlas.validate().joinToString(), PixelSpriteAtlas.validate().isEmpty())
        assertTrue(PixelSpriteAtlas.allIds().containsAll(
            setOf("player", "player_back", "player_side", "wood", "metal", "fabric", "food", "water", "bandage", "bed", "workbench", "storage", "bookshelf", "radio", "tree", "rubble", "road_barrier", "bunker_panel")
        ))
    }

    @Test
    fun environmentSpritesFitTheGeneratedAtlasCell() {
        listOf("tree", "rubble", "road_barrier", "bunker_panel").forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            assertTrue("$id must fit the 16x16 atlas cell", sprite.frames.all { frame ->
                frame.size <= 16 && frame.all { it.length <= 16 }
            })
        }
    }
}
