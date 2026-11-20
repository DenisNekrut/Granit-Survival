package com.granit.survival

/** Deterministic outdoor point-light and shadow rules shared by renderer and tests. */
internal object OutdoorLighting {
    data class Profile(val radius: Float, val intensity: Float)
    data class ShadowProfile(
        val offsetX: Float,
        val lengthScale: Float,
        val opacityScale: Float
    )

    fun streetlampProfile(minuteOfDay: Int, weather: WeatherType): Profile {
        val darkness = DayNightLighting.outdoorDarkness(minuteOfDay)
        val nightFactor = ((darkness - 0.10f) / 0.52f).coerceIn(0f, 1f)
        val weatherFactor = when (weather) {
            WeatherType.CLEAR -> 1.00f
            WeatherType.FOG -> 0.82f
            WeatherType.RAIN -> 0.78f
            WeatherType.COLD -> 0.92f
            WeatherType.OVERCAST -> 0.86f
        }
        return Profile(
            radius = 2.4f + 1.1f * nightFactor,
            intensity = (nightFactor * weatherFactor).coerceIn(0f, 1f)
        )
    }

    /**
     * A compact directional shadow profile: morning light comes from the east,
     * midday shadows shorten, and evening shadows extend in the opposite direction.
     * At night the directional component is deliberately subdued.
     */
    fun shadowProfile(minuteOfDay: Int): ShadowProfile {
        val minute = minuteOfDay.coerceIn(0, WorldClock.MINUTES_PER_DAY - 1)
        return ShadowProfile(
            offsetX = interpolate(minute, listOf(0 to 0.03f, 360 to -0.18f, 720 to 0f, 1080 to 0.18f, 1439 to 0.03f)),
            lengthScale = interpolate(minute, listOf(0 to 0.90f, 360 to 1.18f, 720 to 0.72f, 1080 to 1.18f, 1439 to 0.90f)),
            opacityScale = interpolate(minute, listOf(0 to 0.48f, 360 to 0.92f, 720 to 0.82f, 1080 to 0.92f, 1439 to 0.48f))
        )
    }

    private fun interpolate(minute: Int, keys: List<Pair<Int, Float>>): Float {
        val upperIndex = keys.indexOfFirst { it.first >= minute }.let {
            if (it < 0) keys.lastIndex else it
        }
        if (upperIndex == 0) return keys.first().second
        val (startMinute, startValue) = keys[upperIndex - 1]
        val (endMinute, endValue) = keys[upperIndex]
        val linear = ((minute - startMinute).toFloat() / (endMinute - startMinute).coerceAtLeast(1))
            .coerceIn(0f, 1f)
        val eased = linear * linear * (3f - 2f * linear)
        return startValue + (endValue - startValue) * eased
    }

    fun isLightSource(spriteId: String?): Boolean =
        spriteId != null && spriteId.startsWith("streetlamp") && !spriteId.endsWith("_broken")

    /** Resolve static streetlamp positions once when a zone map enters the renderer. */
    fun streetlampPositions(map: TileMap): List<Pair<Float, Float>> {
        val result = ArrayList<Pair<Float, Float>>()
        map.forEachTile { x, y, type ->
            if (type != TileType.FLOOR) return@forEachTile
            if (isLightSource(map.environmentPropAt(x, y))) {
                result += (x + 0.5f) to (y + 0.72f)
            }
        }
        return result
    }
}
