package com.granit.survival

import java.nio.FloatBuffer

/** Writes one quad as two triangles into caller-owned reusable buffers. */
object SpriteQuadGeometry {
    fun append(
        positions: FloatBuffer,
        textureCoordinates: FloatBuffer,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        uv: SpriteAtlasUvRect,
        flipX: Boolean = false
    ) {
        // X grows left-to-right, but screen-space Y is converted to NDC where the\n        // top edge has a larger value than the bottom edge. Descending Y is valid.\n        require(left.isFinite() && right.isFinite() && top.isFinite() && bottom.isFinite() && right >= left) {\n            "Sprite quad horizontal bounds are inverted or non-finite"\n        }
        val uLeft = if (flipX) uv.right else uv.left
        val uRight = if (flipX) uv.left else uv.right
        positions.put(left).put(top).put(left).put(bottom).put(right).put(top)
        positions.put(right).put(top).put(left).put(bottom).put(right).put(bottom)
        textureCoordinates.put(uLeft).put(uv.top)
            .put(uLeft).put(uv.bottom)
            .put(uRight).put(uv.top)
        textureCoordinates.put(uRight).put(uv.top)
            .put(uLeft).put(uv.bottom)
            .put(uRight).put(uv.bottom)
    }
}
