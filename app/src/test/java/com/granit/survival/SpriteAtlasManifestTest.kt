package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpriteAtlasManifestTest {
    private fun validManifest() = SpriteAtlasManifest(
        version = 1,
        texture = "sprites/world.png",
        textureWidth = 256,
        textureHeight = 128,
        filter = "nearest",
        sprites = listOf(
            SpriteAtlasEntry(
                id = "player_idle",
                frameWidth = 16,
                frameHeight = 24,
                frames = listOf(SpriteAtlasFrame(0, 0, 16, 24), SpriteAtlasFrame(16, 0, 16, 24))
            )
        )
    )

    @Test fun validManifestPassesValidation() {
        assertTrue(SpriteAtlasManifestValidator.validate(validManifest()).isEmpty())
    }

    @Test fun frameCoordinatesConvertToNormalizedTopLeftUv() {
        val manifest = validManifest()
        val uv = manifest.uvRect(SpriteAtlasFrame(64, 32, 32, 16))
        assertEquals(0.25f, uv.left, 0.0001f)
        assertEquals(0.25f, uv.top, 0.0001f)
        assertEquals(0.375f, uv.right, 0.0001f)
        assertEquals(0.375f, uv.bottom, 0.0001f)
    }

    @Test fun outOfBoundsFrameIsRejected() {
        val manifest = validManifest().copy(
            sprites = listOf(SpriteAtlasEntry("bad", 32, 32, listOf(SpriteAtlasFrame(240, 100, 32, 32))))
        )
        assertTrue(SpriteAtlasManifestValidator.validate(manifest).any { it.contains("exceeds texture bounds") })
    }

    @Test fun unsafeTexturePathIsRejected() {
        val errors = SpriteAtlasManifestValidator.validate(validManifest().copy(texture = "../private.png"))
        assertTrue(errors.any { it.contains("inside the assets directory") })
    }

    @Test fun duplicateIdsAndInvalidPivotAreRejected() {
        val entry = SpriteAtlasEntry("same", 8, 8, listOf(SpriteAtlasFrame(0, 0, 8, 8)), pivotX = 2f)
        val manifest = validManifest().copy(sprites = listOf(entry, entry))
        val errors = SpriteAtlasManifestValidator.validate(manifest)
        assertTrue(errors.any { it.contains("Duplicate sprite id") })
        assertTrue(errors.any { it.contains("pivot") })
    }

    @Test fun emptyFrameListIsRejected() {
        val manifest = validManifest().copy(
            sprites = listOf(SpriteAtlasEntry("empty", 8, 8, emptyList()))
        )
        assertFalse(SpriteAtlasManifestValidator.validate(manifest).isEmpty())
    }
}
