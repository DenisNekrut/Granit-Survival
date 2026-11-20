package com.granit.survival

/**
 * World-art grade for the new cold, weathered pixel-art direction.
 * Keep it shared by the runtime fallback renderer and generated texture atlas.
 */
internal object GranitArtPalette {
    const val WORLD_SPRITE_SCALE = 1.18f

    fun grade(red: Float, green: Float, blue: Float): FloatArray {
        fun contrast(value: Float): Float = ((value - 0.5f) * 1.10f + 0.5f).coerceIn(0f, 1f)
        val luminance = red * 0.2126f + green * 0.7152f + blue * 0.0722f
        val shadow = if (luminance < 0.42f) 1f else 0f
        val highlight = if (luminance > 0.70f) 1f else 0f
        return floatArrayOf(
            (contrast(red) - 0.010f * shadow + 0.012f * highlight).coerceIn(0f, 1f),
            (contrast(green) + 0.006f * shadow + 0.006f * highlight).coerceIn(0f, 1f),
            (contrast(blue) + 0.018f * shadow - 0.004f * highlight).coerceIn(0f, 1f)
        )
    }
}
