package com.granit.survival

/** Deterministic outdoor point-light rules shared by the renderer and unit tests. */
internal object OutdoorLighting {
    data class Profile(val radius: Float, val intensity: Float)

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
