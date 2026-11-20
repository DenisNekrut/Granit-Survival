package com.granit.survival

/**
 * Canonical world-art palette. Values are authored as HEX so material colours
 * remain consistent across sprite atlases, zone floors, weather grading and lights.
 */
internal object GranitWorldPalette {
    data class Ramp(val shadow: FloatArray, val mid: FloatArray, val highlight: FloatArray)

    const val VOID_BLACK = 0x101820
    const val DEEP_GRAPHITE = 0x1B2930
    const val COLD_CHARCOAL = 0x293B43
    const val STEEL_BLUE = 0x496575
    const val MIST_BLUE = 0x7896A0
    const val CONCRETE = 0x78817E
    const val PALE_CONCRETE = 0xA9AAA0
    const val OXIDIZED_TEAL = 0x287F78
    const val TEAL_HIGHLIGHT = 0x55B7A3
    const val PINE_SHADOW = 0x183D32
    const val PINE_GREEN = 0x286447
    const val LEAF_GREEN = 0x4B8A4B
    const val MOSS = 0x789447
    const val TOXIC_LIME = 0xB4C94B
    const val SOIL_SHADOW = 0x49372F
    const val EARTH_BROWN = 0x76513B
    const val DUST_OCHRE = 0xB58A4A
    const val RUST_SHADOW = 0x63352D
    const val RUST = 0xA94F35
    const val BRICK = 0xB85D43
    const val AMBER = 0xE3A448
    const val WARM_LIGHT = 0xFFD27A
    const val WARNING_RED = 0xD65345
    const val SNOW_ASH = 0xD2D1C4

    private fun channels(hex: Int): FloatArray = floatArrayOf(
        ((hex shr 16) and 0xFF) / 255f,
        ((hex shr 8) and 0xFF) / 255f,
        (hex and 0xFF) / 255f
    )

    private fun ramp(shadow: Int, mid: Int, highlight: Int) =
        Ramp(channels(shadow), channels(mid), channels(highlight))

    val slate = ramp(VOID_BLACK, STEEL_BLUE, PALE_CONCRETE)
    val rust = ramp(RUST_SHADOW, RUST, AMBER)
    val ochre = ramp(SOIL_SHADOW, DUST_OCHRE, WARM_LIGHT)
    val moss = ramp(PINE_SHADOW, LEAF_GREEN, TOXIC_LIME)
    val oxidized = ramp(DEEP_GRAPHITE, OXIDIZED_TEAL, TEAL_HIGHLIGHT)
    val steelBlue = ramp(VOID_BLACK, STEEL_BLUE, MIST_BLUE)

    /** Authored semantic ramps; unlike a global hue shift, each material keeps its identity. */
    val materialRamps: Map<String, Ramp> = mapOf(
        "concrete" to ramp(COLD_CHARCOAL, CONCRETE, PALE_CONCRETE),
        "steel" to ramp(VOID_BLACK, STEEL_BLUE, MIST_BLUE),
        "rust" to ramp(RUST_SHADOW, RUST, BRICK),
        "pine" to ramp(PINE_SHADOW, PINE_GREEN, LEAF_GREEN),
        "soil" to ramp(SOIL_SHADOW, EARTH_BROWN, DUST_OCHRE),
        "brick" to ramp(RUST_SHADOW, BRICK, AMBER),
        "water" to ramp(0x102B3A, 0x287A91, 0x79C4D1),
        "moss" to ramp(PINE_SHADOW, MOSS, TOXIC_LIME),
        "wood" to ramp(0x39291F, 0x805638, 0xC18A4D),
        "fabric" to ramp(0x263B38, 0x52766A, 0x9BB99A),
        "skin" to ramp(0x684638, 0xB77C58, 0xE2B184),
        "hazard" to ramp(0x4B3820, 0xB58A2E, 0xE6D15A),
        "medical" to ramp(0x6B3030, 0xC65349, 0xF0A08A)
    )

    /** Distinct ground identities for the four authored zones. */
    fun zoneFloor(zone: WorldZone): FloatArray = when (zone) {
        WorldZone.BUNKER -> channels(0x293B43)
        WorldZone.FOREST -> channels(0x355B38)
        WorldZone.RUINS -> channels(0x79513D)
        WorldZone.ROAD -> channels(0x405D70)
        WorldZone.COMMUNICATION_NODE -> channels(0x293B43)
    }

    /** Accent colours used by zone props, landmarks and lighting cues. */
    fun zoneAccent(zone: WorldZone): FloatArray = when (zone) {
        WorldZone.BUNKER -> channels(OXIDIZED_TEAL)
        WorldZone.FOREST -> channels(LEAF_GREEN)
        WorldZone.RUINS -> channels(BRICK)
        WorldZone.ROAD -> channels(MIST_BLUE)
        WorldZone.COMMUNICATION_NODE -> channels(OXIDIZED_TEAL)
    }

    /**
     * Weather colour grades are intentionally visible, not a barely perceptible
     * overlay. Keep alpha below 0.25 so silhouettes and authored material ramps
     * remain readable on the 128x128 tile grid.
     */
    fun weatherOverlay(weather: WeatherType): FloatArray = when (weather) {
        WeatherType.CLEAR -> floatArrayOf(0f, 0f, 0f, 0f)
        WeatherType.FOG -> floatArrayOf(0.66f, 0.76f, 0.77f, 0.21f)
        WeatherType.RAIN -> floatArrayOf(0.12f, 0.24f, 0.34f, 0.23f)
        WeatherType.COLD -> floatArrayOf(0.48f, 0.68f, 0.78f, 0.20f)
        WeatherType.OVERCAST -> floatArrayOf(0.42f, 0.49f, 0.51f, 0.17f)
    }

    /** Return fresh arrays so callers cannot mutate the shared palette. */
    fun spriteRamp(name: String): Ramp {
        val source = when (name) {
            "slate" -> slate
            "rust" -> rust
            "ochre" -> ochre
            "moss" -> moss
            "oxidized" -> oxidized
            "steelBlue" -> steelBlue
            else -> error("Unknown sprite ramp: $name")
        }
        return Ramp(source.shadow.copyOf(), source.mid.copyOf(), source.highlight.copyOf())
    }

    fun materialRamp(name: String): Ramp {
        val source = materialRamps[name] ?: error("Unknown material ramp: $name")
        return Ramp(source.shadow.copyOf(), source.mid.copyOf(), source.highlight.copyOf())
    }

    fun hexColor(hex: Int): FloatArray = channels(hex)
}
