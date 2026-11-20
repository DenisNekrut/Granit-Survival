package com.granit.survival

/**
 * Replacement material palette for every in-game sprite.
 * Original silhouettes remain unchanged while source colours are remapped into
 * cold steel, oxidized teal, moss/olive, muted rust and amber highlight families.
 */
internal object GranitSpritePalette {
    private data class Ramp(val shadow: FloatArray, val mid: FloatArray, val highlight: FloatArray)

    private val slate = GranitWorldPalette.spriteRamp("slate")
    private val rust = GranitWorldPalette.spriteRamp("rust")
    private val ochre = GranitWorldPalette.spriteRamp("ochre")
    private val moss = GranitWorldPalette.spriteRamp("moss")
    private val oxidized = GranitWorldPalette.spriteRamp("oxidized")
    private val steelBlue = GranitWorldPalette.spriteRamp("steelBlue")

    fun remap(sprite: PixelSprite): PixelSprite = PixelSprite(
        frames = sprite.frames,
        palette = sprite.palette.mapValues { (_, color) -> remapColor(color) }
    )

    fun remapColor(source: FloatArray): FloatArray {
        require(source.size >= 3) { "Sprite palette colours must contain RGB channels" }
        val red = source[0].coerceIn(0f, 1f)
        val green = source[1].coerceIn(0f, 1f)
        val blue = source[2].coerceIn(0f, 1f)
        val maximum = maxOf(red, green, blue)
        val minimum = minOf(red, green, blue)
        val chroma = maximum - minimum
        val hue = when {
            chroma < 0.0001f -> 0f
            maximum == red -> (60f * ((green - blue) / chroma) + 360f) % 360f
            maximum == green -> 60f * ((blue - red) / chroma + 2f)
            else -> 60f * ((red - green) / chroma + 4f)
        }
        val ramp = when {
            chroma < 0.075f -> slate
            hue < 18f || hue >= 345f -> rust
            hue < 55f -> ochre
            hue < 160f -> moss
            hue < 205f -> oxidized
            hue < 255f -> steelBlue
            else -> rust
        }
        val luminance = (red * 0.2126f + green * 0.7152f + blue * 0.0722f).coerceIn(0f, 1f)
        val low = if (luminance < 0.48f) ramp.shadow else ramp.mid
        val high = if (luminance < 0.48f) ramp.mid else ramp.highlight
        val amount = if (luminance < 0.48f) luminance / 0.48f else (luminance - 0.48f) / 0.52f
        return FloatArray(3) { channel ->
            (low[channel] + (high[channel] - low[channel]) * amount).coerceIn(0f, 1f)
        }
    }
}
