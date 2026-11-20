package com.granit.survival

/**
 * Material-aware recolouring for the built-in pixel atlas.
 * Sprite silhouettes and source frame geometry are never modified.
 */
internal object GranitSpritePalette {
    private val slate = GranitWorldPalette.spriteRamp("slate")
    private val rust = GranitWorldPalette.spriteRamp("rust")
    private val ochre = GranitWorldPalette.spriteRamp("ochre")
    private val moss = GranitWorldPalette.spriteRamp("moss")
    private val oxidized = GranitWorldPalette.spriteRamp("oxidized")
    private val steelBlue = GranitWorldPalette.spriteRamp("steelBlue")

    fun remap(sprite: PixelSprite): PixelSprite = remap(sprite, "")

    fun remap(sprite: PixelSprite, spriteId: String): PixelSprite {
        val material = materialForSprite(spriteId)
        return PixelSprite(
            frames = sprite.frames,
            palette = sprite.palette.mapValues { (_, color) ->
                if (material == null) remapColor(color) else remapColor(color, material)
            }
        )
    }

    /** Material routing is explicit so one hue cannot accidentally turn every object into foliage or rust. */
    internal fun materialForSprite(spriteId: String): String? = when {
        spriteId.startsWith("tree") || spriteId.startsWith("bush") ||
            spriteId.startsWith("grass_patch") -> "pine"
        spriteId.startsWith("fallen_log") || spriteId.startsWith("crate") ||
            spriteId in setOf("wood", "item_wood", "bed", "bookshelf", "dining_table") -> "wood"
        spriteId.startsWith("metal_scrap") || spriteId.startsWith("floor_grate") ||
            spriteId.startsWith("road_barrier") || spriteId.startsWith("streetlamp") ||
            spriteId.startsWith("road_sign") || spriteId.startsWith("wreck") ||
            spriteId.startsWith("bunker_panel") ||
            spriteId in setOf("metal", "item_metal", "radio", "generator", "storage", "locker",
                "workbench", "control_console", "vault_door", "ceiling_lamp") -> "steel"
        spriteId.startsWith("rubble") || spriteId.startsWith("debris") ||
            spriteId.startsWith("barrel") -> "brick"
        spriteId.startsWith("puddle") || spriteId in setOf("water", "item_water") -> "water"
        spriteId in setOf("fabric", "item_fabric", "bandage", "item_bandage") -> "fabric"
        spriteId == "medical_station" || spriteId == "item_medical_kit" -> "medical"
        spriteId.startsWith("infected_") -> "skin"
        spriteId in setOf("food", "item_food") -> "hazard"
        else -> null
    }

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
        return mapLuminance(source, ramp)
    }

    fun remapColor(source: FloatArray, material: String): FloatArray {
        require(source.size >= 3) { "Sprite palette colours must contain RGB channels" }
        return mapLuminance(source, GranitWorldPalette.materialRamp(material))
    }

    private fun mapLuminance(source: FloatArray, ramp: GranitWorldPalette.Ramp): FloatArray {
        val red = source[0].coerceIn(0f, 1f)
        val green = source[1].coerceIn(0f, 1f)
        val blue = source[2].coerceIn(0f, 1f)
        val luminance = (red * 0.2126f + green * 0.7152f + blue * 0.0722f).coerceIn(0f, 1f)
        val low = if (luminance < 0.48f) ramp.shadow else ramp.mid
        val high = if (luminance < 0.48f) ramp.mid else ramp.highlight
        val amount = if (luminance < 0.48f) luminance / 0.48f else (luminance - 0.48f) / 0.52f
        return FloatArray(3) { channel ->
            (low[channel] + (high[channel] - low[channel]) * amount).coerceIn(0f, 1f)
        }
    }
}
