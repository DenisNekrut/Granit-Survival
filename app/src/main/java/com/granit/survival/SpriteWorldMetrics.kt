package com.granit.survival

/** A map cell is always a 128x128 logical-pixel tile and exactly one world unit. */
internal data class SpriteWorldFootprint(val width: Float, val height: Float)

internal object SpriteWorldMetrics {
    const val TILE_PIXELS = 128
    const val TILE_WORLD_SIZE = 1f

    private fun sourceDimensions(spriteId: String): Pair<Int, Int> {
        val sprite = requireNotNull(PixelSpriteAtlas.sprite(spriteId)) { "Missing sprite $spriteId" }
        val frame = requireNotNull(sprite.frames.firstOrNull()) { "Sprite $spriteId has no frames" }
        val width = frame.maxOfOrNull { it.length } ?: 0
        val height = frame.size
        require(width > 0 && height > 0) { "Sprite $spriteId has an empty frame" }
        return width to height
    }

    /**
     * Converts authored source pixels to world units by fitting the longest side
     * into the requested fraction of one 128x128 map tile. Source resolution,
     * 2x atlas rasterization and device DPI never alter the logical footprint.
     */
    fun pixelWorldSize(spriteId: String, tileExtent: Float = 1f): Float {
        require(tileExtent > 0f) { "Tile extent must be positive" }
        val (width, height) = sourceDimensions(spriteId)
        val longestSide = maxOf(width, height)
        val sourcePixelsPerTile = TILE_PIXELS.toFloat() / longestSide
        return (TILE_WORLD_SIZE / TILE_PIXELS) * sourcePixelsPerTile * tileExtent
    }

    fun footprint(spriteId: String, tileExtent: Float = 1f): SpriteWorldFootprint {
        val (width, height) = sourceDimensions(spriteId)
        val pixelSize = pixelWorldSize(spriteId, tileExtent)
        return SpriteWorldFootprint(width * pixelSize, height * pixelSize)
    }
}
