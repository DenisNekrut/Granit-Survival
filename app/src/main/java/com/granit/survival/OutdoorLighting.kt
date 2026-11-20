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
}
