package com.granit.survival

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.fail
import org.junit.Test
import java.nio.FloatBuffer

class SpriteQuadGeometryTest {
    @Test fun writesTwoTrianglesWithTopLeftUvMapping() {
        val positions = FloatBuffer.allocate(12)
        val uvs = FloatBuffer.allocate(12)
        SpriteQuadGeometry.append(
            positions, uvs, -1f, -0.5f, 1f, 0.5f,
            SpriteAtlasUvRect(0.25f, 0.125f, 0.5f, 0.375f)
        )
        assertArrayEquals(
            floatArrayOf(-1f,-0.5f, -1f,0.5f, 1f,-0.5f, 1f,-0.5f, -1f,0.5f, 1f,0.5f),
            positions.array(), 0.0001f
        )
        assertArrayEquals(
            floatArrayOf(0.25f,0.125f, 0.25f,0.375f, 0.5f,0.125f,
                0.5f,0.125f, 0.25f,0.375f, 0.5f,0.375f),
            uvs.array(), 0.0001f
        )
    }

    @Test fun acceptsTopToBottomScreenCoordinatesConvertedToDescendingNdcY() {
        val positions = FloatBuffer.allocate(12)
        val uvs = FloatBuffer.allocate(12)
        // In OpenGL NDC, the visual top is +1 and the visual bottom is -1.
        SpriteQuadGeometry.append(
            positions, uvs, -1f, 1f, 1f, -1f,
            SpriteAtlasUvRect(0f, 0f, 1f, 1f)
        )
        assertArrayEquals(
            floatArrayOf(-1f,1f, -1f,-1f, 1f,1f, 1f,1f, -1f,-1f, 1f,-1f),
            positions.array(), 0.0001f
        )
    }

    @Test fun rejectsInvertedHorizontalBounds() {
        val positions = FloatBuffer.allocate(12)
        val uvs = FloatBuffer.allocate(12)
        try {
            SpriteQuadGeometry.append(
                positions, uvs, 1f, -1f, -1f, 1f,
                SpriteAtlasUvRect(0f, 0f, 1f, 1f)
            )
            fail("Expected inverted horizontal bounds to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: left must not exceed right.
        }
    }

    @Test fun horizontalFlipSwapsOnlyHorizontalUvCoordinates() {
        val positions = FloatBuffer.allocate(12)
        val uvs = FloatBuffer.allocate(12)
        SpriteQuadGeometry.append(
            positions, uvs, 0f, 0f, 1f, 1f,
            SpriteAtlasUvRect(0.2f, 0.3f, 0.6f, 0.9f),
            flipX = true
        )
        assertArrayEquals(
            floatArrayOf(0.6f,0.3f, 0.6f,0.9f, 0.2f,0.3f,
                0.2f,0.3f, 0.6f,0.9f, 0.2f,0.9f),
            uvs.array(), 0.0001f
        )
    }
}


class SpriteAtlasManifestValidationTest {
    private fun validManifest(
        texture: String = "sprites/world.png",
        sprites: List<SpriteAtlasEntry> = listOf(
            SpriteAtlasEntry("player", 16, 16, listOf(SpriteAtlasFrame(0, 0, 16, 16)))
        ),
        pivotX: Float = 0.5f,
        filter: String = "nearest"
    ) = SpriteAtlasManifest(
        version = 1, texture = texture, textureWidth = 64, textureHeight = 64,
        filter = filter, sprites = sprites.map { it.copy(pivotX = pivotX) }
    )

    @Test fun acceptsValidManifest() {
        org.junit.Assert.assertTrue(SpriteAtlasManifestValidator.validate(validManifest()).isEmpty())
    }

    @Test fun rejectsUnsafeTexturePaths() {
        listOf("../outside.png", "/outside.png", "sprites\\..\\outside.png").forEach { path ->
            val errors = SpriteAtlasManifestValidator.validate(validManifest(texture = path))
            org.junit.Assert.assertTrue(errors.any { it.contains("inside the assets") })
        }
    }

    @Test fun rejectsDuplicateSpriteIds() {
        val entry = SpriteAtlasEntry("tree", 8, 8, listOf(SpriteAtlasFrame(0, 0, 8, 8)))
        val errors = SpriteAtlasManifestValidator.validate(validManifest(sprites = listOf(entry, entry)))
        org.junit.Assert.assertTrue(errors.any { it.contains("Duplicate sprite id: tree") })
    }

    @Test fun rejectsOutOfBoundsAndMismatchedFrames() {
        val outside = SpriteAtlasEntry("outside", 16, 16, listOf(SpriteAtlasFrame(56, 56, 16, 16)))
        val mismatch = SpriteAtlasEntry("mismatch", 16, 16, listOf(SpriteAtlasFrame(0, 0, 8, 8)))
        val errors = SpriteAtlasManifestValidator.validate(validManifest(sprites = listOf(outside, mismatch)))
        org.junit.Assert.assertTrue(errors.any { it.contains("exceeds texture bounds") })
        org.junit.Assert.assertTrue(errors.any { it.contains("does not match the declared frame size") })
    }

    @Test fun rejectsInvalidPivotAndSamplingFilter() {
        val pivotErrors = SpriteAtlasManifestValidator.validate(validManifest(pivotX = 1.5f))
        val filterErrors = SpriteAtlasManifestValidator.validate(validManifest(filter = "mipmap"))
        org.junit.Assert.assertTrue(pivotErrors.any { it.contains("pivot must be within") })
        org.junit.Assert.assertTrue(filterErrors.any { it.contains("Unsupported texture filter") })
    }

    @Test fun convertsTopLeftCoordinatesToNormalizedUvBounds() {
        val uv = validManifest().uvRect(SpriteAtlasFrame(16, 8, 16, 24))
        org.junit.Assert.assertEquals(0.25f, uv.left, 0.0001f)
        org.junit.Assert.assertEquals(0.125f, uv.top, 0.0001f)
        org.junit.Assert.assertEquals(0.5f, uv.right, 0.0001f)
        org.junit.Assert.assertEquals(0.5f, uv.bottom, 0.0001f)
    }
}
