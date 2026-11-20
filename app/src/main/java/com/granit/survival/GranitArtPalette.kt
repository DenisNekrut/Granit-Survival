package com.granit.survival

/**
 * Saturated post-apocalyptic pixel-art grade: vivid materials, restrained shadows.
 * Keep it shared by the runtime fallback renderer and generated texture atlas.
 */
internal object GranitArtPalette {
    const val WORLD_SPRITE_SCALE = 1.18f

    fun grade(red: Float, green: Float, blue: Float): FloatArray {
        fun contrast(value: Float): Float = ((value - 0.5f) * 1.08f + 0.5f).coerceIn(0f, 1f)
        val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        val shadow = if (luminance < 0.38f) 1f else 0f
        val highlight = if (luminance > 0.68f) 1f else 0f
        fun vivid(value: Float, tint: Float): Float =
            (luminance + (value - luminance) * 1.22f + tint).coerceIn(0f, 1f)
        return floatArrayOf(
            contrast(vivid(red, 0.012f * highlight - 0.006f * shadow)),
            contrast(vivid(green, 0.008f * shadow + 0.004f * highlight)),
            contrast(vivid(blue, 0.018f * shadow - 0.006f * highlight))
        )
    }
}
