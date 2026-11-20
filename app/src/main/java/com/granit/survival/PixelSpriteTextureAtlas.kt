package com.granit.survival

import android.graphics.Bitmap
import android.graphics.Color

data class BuiltPixelSpriteAtlas(
    val manifest: SpriteAtlasManifest,
    val bitmap: Bitmap
)

/** Packed position of one sprite inside the generated texture. */
data class SpriteAtlasPackEntry(
    val id: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val frames: List<SpriteAtlasFrame>
)

/**
 * Platform-independent atlas layout.
 *
 * The cell size follows the largest authored frame after the 2x raster pass,
 * so tall character frames and small environment props share one texture
 * without any frame exceeding its cell. This type intentionally avoids Android classes so
 * the packing rules can be verified by JVM unit tests.
 */
data class SpriteAtlasPackPlan(
    val cellSize: Int,
    val columns: Int,
    val rows: Int,
    val entries: List<SpriteAtlasPackEntry>
) {
    val textureWidth: Int get() = columns * cellSize
    val textureHeight: Int get() = rows * cellSize
    val frameCount: Int get() = entries.sumOf { it.frames.size }

    fun entry(id: String): SpriteAtlasPackEntry? = entries.firstOrNull { it.id == id }
}

/**
 * Packs the current authored pixel sprites into a transparent RGBA texture.
 * This keeps the game playable while the project transitions from code-defined
 * sprites to externally authored PNG atlases.
 */
object PixelSpriteTextureAtlas {
    const val COLUMNS = 16

    /**
     * Rasterize each authored logical pixel into a 2x2 block in the runtime
     * texture. World-space sprite bounds remain unchanged; nearest filtering
     * keeps the higher-density atlas crisp on high-DPI phone displays.
     */
    const val RASTER_SCALE = 2

    /**
     * Compute the texture layout for every authored sprite.
     *
     * Every frame must have identical dimensions inside its own sprite; the cell
     * size is derived from the largest frame so a sprite can never be laid out
     * outside its cell or overlap the next one.
     */
    fun plan(): SpriteAtlasPackPlan {
        val ids = PixelSpriteAtlas.allIds().sorted()
        require(ids.isNotEmpty()) { "Pixel sprite atlas is empty" }

        val bounds = ids.associateWith { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id)) { "Sprite $id is missing" }
            require(sprite.frames.isNotEmpty()) { "Sprite $id has no frames" }
            val first = sprite.frames.first()
            val frameWidth = first.maxOfOrNull { it.length } ?: 0
            val frameHeight = first.size
            require(frameWidth > 0 && frameHeight > 0) { "Sprite $id has an empty frame" }
            sprite.frames.forEachIndexed { index, rowsData ->
                require(rowsData.size == frameHeight && rowsData.all { it.length == frameWidth }) {
                    "Sprite $id frame $index has inconsistent dimensions"
                }
            }
            frameWidth to frameHeight
        }

        val cellSize = bounds.values.maxOf { (width, height) -> maxOf(width, height) } * RASTER_SCALE
        val totalFrames = ids.sumOf { requireNotNull(PixelSpriteAtlas.sprite(it)).frames.size }
        val rows = (totalFrames + COLUMNS - 1) / COLUMNS

        var frameIndex = 0
        val entries = ids.map { id ->
            val (frameWidth, frameHeight) = bounds.getValue(id)
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            val frames = List(sprite.frames.size) {
                val cellX = (frameIndex % COLUMNS) * cellSize
                val cellY = (frameIndex / COLUMNS) * cellSize
                frameIndex++
                SpriteAtlasFrame(cellX, cellY, frameWidth * RASTER_SCALE, frameHeight * RASTER_SCALE)
            }
            SpriteAtlasPackEntry(id, frameWidth * RASTER_SCALE, frameHeight * RASTER_SCALE, frames)
        }
        return SpriteAtlasPackPlan(cellSize, COLUMNS, rows, entries)
    }

    fun build(): BuiltPixelSpriteAtlas {
        val plan = plan()
        val bitmap = Bitmap.createBitmap(plan.textureWidth, plan.textureHeight, Bitmap.Config.ARGB_8888)
        val entries = plan.entries.map { packed ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(packed.id))
            packed.frames.forEachIndexed { frameIndex, frame ->
                sprite.frames[frameIndex].forEachIndexed { y, row ->
                    row.forEachIndexed { x, symbol ->
                        if (symbol != '.') {
                            val color = sprite.palette[symbol]
                                ?: error("Sprite ${packed.id} uses undefined palette symbol $symbol")
                            val argb = Color.argb(
                                255,
                                (color[0] * 255f).toInt(),
                                (color[1] * 255f).toInt(),
                                (color[2] * 255f).toInt()
                            )
                            val pixelX = frame.x + x * RASTER_SCALE
                            val pixelY = frame.y + y * RASTER_SCALE
                            for (dy in 0 until RASTER_SCALE) {
                                for (dx in 0 until RASTER_SCALE) {
                                    bitmap.setPixel(pixelX + dx, pixelY + dy, argb)
                                }
                            }
                        }
                    }
                }
            }
            SpriteAtlasEntry(
                id = packed.id,
                frameWidth = packed.frameWidth,
                frameHeight = packed.frameHeight,
                frames = packed.frames
            )
        }

        val manifest = SpriteAtlasManifest(
            version = 1,
            texture = "generated/pixel-sprite-atlas",
            textureWidth = plan.textureWidth,
            textureHeight = plan.textureHeight,
            filter = "nearest",
            sprites = entries
        )
        val errors = SpriteAtlasManifestValidator.validate(manifest)
        check(errors.isEmpty()) { "Generated sprite atlas is invalid: ${errors.joinToString()}" }
        return BuiltPixelSpriteAtlas(manifest, bitmap)
    }
}
