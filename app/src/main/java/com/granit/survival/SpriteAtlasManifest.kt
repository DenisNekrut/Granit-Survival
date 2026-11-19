package com.granit.survival

/**
 * Platform-independent description of a texture atlas.
 *
 * Coordinates are expressed in source-image pixels with a top-left origin.
 * GPU-specific UV conversion is deliberately kept in the renderer layer.
 */
data class SpriteAtlasManifest(
    val version: Int,
    val texture: String,
    val textureWidth: Int,
    val textureHeight: Int,
    val filter: String,
    val sprites: List<SpriteAtlasEntry>
)

data class SpriteAtlasEntry(
    val id: String,
    val frameWidth: Int,
    val frameHeight: Int,
    val frames: List<SpriteAtlasFrame>,
    val pivotX: Float = 0.5f,
    val pivotY: Float = 1.0f
)

data class SpriteAtlasFrame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class SpriteAtlasUvRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

object SpriteAtlasManifestValidator {
    private val allowedFilters = setOf("nearest", "linear")

    fun validate(manifest: SpriteAtlasManifest): List<String> {
        val errors = mutableListOf<String>()
        if (manifest.version != 1) errors += "Unsupported manifest version: ${manifest.version}"
        if (manifest.texture.isBlank()) errors += "Texture path is empty"
        if (manifest.texture.startsWith("/") || manifest.texture.contains('\\') || manifest.texture.split('/').any { it == ".." }) {
            errors += "Texture path must stay inside the assets directory"
        }
        if (manifest.textureWidth <= 0 || manifest.textureHeight <= 0) errors += "Texture dimensions must be positive"
        if (manifest.filter !in allowedFilters) errors += "Unsupported texture filter: ${manifest.filter}"

        val duplicateIds = manifest.sprites.groupBy { it.id }.filterValues { it.size > 1 }.keys
        duplicateIds.forEach { errors += "Duplicate sprite id: $it" }

        manifest.sprites.forEach { sprite ->
            if (sprite.id.isBlank()) errors += "Sprite id is empty"
            if (sprite.frameWidth <= 0 || sprite.frameHeight <= 0) errors += "${sprite.id} frame dimensions must be positive"
            if (sprite.frames.isEmpty()) errors += "${sprite.id} has no frames"
            if (!sprite.pivotX.isFinite() || sprite.pivotX !in 0f..1f ||
                !sprite.pivotY.isFinite() || sprite.pivotY !in 0f..1f) {
                errors += "${sprite.id} pivot must be within 0..1"
            }
            sprite.frames.forEachIndexed { index, frame ->
                if (frame.x < 0 || frame.y < 0 || frame.width <= 0 || frame.height <= 0) {
                    errors += "${sprite.id} frame $index has invalid coordinates or size"
                } else if (frame.width != sprite.frameWidth || frame.height != sprite.frameHeight) {
                    errors += "${sprite.id} frame $index does not match the declared frame size"
                } else if (frame.x.toLong() + frame.width > manifest.textureWidth ||
                    frame.y.toLong() + frame.height > manifest.textureHeight) {
                    errors += "${sprite.id} frame $index exceeds texture bounds"
                }
            }
        }
        return errors
    }
}

fun SpriteAtlasManifest.uvRect(frame: SpriteAtlasFrame): SpriteAtlasUvRect {
    require(frame.x >= 0 && frame.y >= 0 && frame.width > 0 && frame.height > 0)
    require(frame.x.toLong() + frame.width <= textureWidth &&
        frame.y.toLong() + frame.height <= textureHeight)
    return SpriteAtlasUvRect(
        left = frame.x.toFloat() / textureWidth,
        top = frame.y.toFloat() / textureHeight,
        right = (frame.x + frame.width).toFloat() / textureWidth,
        bottom = (frame.y + frame.height).toFloat() / textureHeight
    )
}
