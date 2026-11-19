package com.granit.survival

import org.junit.Assert.assertArrayEquals
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
