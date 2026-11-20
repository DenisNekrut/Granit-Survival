package com.granit.survival

/** Sprite bounds expressed in the same world-tile units used by TileMap and Camera2D. */
internal data class SpriteWorldFootprint(val width: Float, val height: Float)

internal object SpriteWorldMetrics {
    fun footprint(spriteId: String, pixelSize: Float): SpriteWorldFootprint {
        val sprite = requireNotNull(PixelSpriteAtlas.sprite(spriteId)) { "Missing sprite $spriteId" }
        val frame = requireNotNull(sprite.frames.firstOrNull()) { "Sprite $spriteId has no frames" }
        val sourceWidth = frame.maxOfOrNull { it.length } ?: 0
        val sourceHeight = frame.size
        val density = PixelSpriteAtlas.worldDensityScale(spriteId)
        val worldScale = GranitArtPalette.WORLD_SPRITE_SCALE
        return SpriteWorldFootprint(
            width = sourceWidth * pixelSize * density * worldScale,
            height = sourceHeight * pixelSize * density * worldScale
        )
    }
}
