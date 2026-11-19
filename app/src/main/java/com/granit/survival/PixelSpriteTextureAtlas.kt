package com.granit.survival

import android.graphics.Bitmap
import android.graphics.Color

data class BuiltPixelSpriteAtlas(
    val manifest: SpriteAtlasManifest,
    val bitmap: Bitmap
)

/**
 * Packs the current authored pixel sprites into a transparent RGBA texture.
 * This keeps the game playable while the project transitions from code-defined
 * sprites to externally authored PNG atlases.
 */
object PixelSpriteTextureAtlas {
    private const val CELL_SIZE = 16
    private const val COLUMNS = 16

    fun build(): BuiltPixelSpriteAtlas {
        val ids = PixelSpriteAtlas.allIds().sorted()
        val framesTotal = ids.sumOf { PixelSpriteAtlas.sprite(it)?.frames?.size ?: 0 }
        require(framesTotal > 0) { "Pixel sprite atlas is empty" }
        val rows = (framesTotal + COLUMNS - 1) / COLUMNS
        val width = COLUMNS * CELL_SIZE
        val height = rows * CELL_SIZE
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val entries = mutableListOf<SpriteAtlasEntry>()
        var frameIndex = 0

        ids.forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            val first = sprite.frames.first()
            val frameWidth = first.maxOfOrNull { it.length } ?: 0
            val frameHeight = first.size
            require(frameWidth in 1..CELL_SIZE && frameHeight in 1..CELL_SIZE) {
                "Sprite $id exceeds atlas cell size"
            }
            val frames = sprite.frames.mapIndexed { index, rowsData ->
                require(rowsData.size == frameHeight && rowsData.all { it.length == frameWidth }) {
                    "Sprite $id frame $index has inconsistent dimensions"
                }
                val cellX = (frameIndex % COLUMNS) * CELL_SIZE
                val cellY = (frameIndex / COLUMNS) * CELL_SIZE
                rowsData.forEachIndexed { y, row ->
                    row.forEachIndexed { x, symbol ->
                        if (symbol != '.') {
                            val color = sprite.palette[symbol]
                                ?: error("Sprite $id uses undefined palette symbol $symbol")
                            val argb = Color.argb(
                                255,
                                (color[0].coerceIn(0f, 1f) * 255f).toInt(),
                                (color[1].coerceIn(0f, 1f) * 255f).toInt(),
                                (color[2].coerceIn(0f, 1f) * 255f).toInt()
                            )
                            bitmap.setPixel(cellX + x, cellY + y, argb)
                        }
                    }
                }
                frameIndex++
                SpriteAtlasFrame(cellX, cellY, frameWidth, frameHeight)
            }
            entries += SpriteAtlasEntry(
                id = id,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                frames = frames
            )
        }

        val manifest = SpriteAtlasManifest(
            version = 1,
            texture = "generated/pixel-sprite-atlas",
            textureWidth = width,
            textureHeight = height,
            filter = "nearest",
            sprites = entries
        )
        val errors = SpriteAtlasManifestValidator.validate(manifest)
        check(errors.isEmpty()) { "Generated sprite atlas is invalid: ${errors.joinToString()}" }
        return BuiltPixelSpriteAtlas(manifest, bitmap)
    }
}
