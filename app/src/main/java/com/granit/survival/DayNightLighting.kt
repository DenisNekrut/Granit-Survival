package com.granit.survival

/** Deterministic outdoor light levels derived from the in-game clock (minute 0..1439). */
internal object DayNightLighting {
    fun outdoorDarkness(minuteOfDay: Int): Float {
        val minute = minuteOfDay.coerceIn(0, WorldClock.MINUTES_PER_DAY - 1)
        return when (minute) {
            in 0..269 -> 0.62f
            in 270..329 -> 0.62f - (minute - 270) / 60f * 0.30f
            in 330..419 -> 0.32f - (minute - 330) / 90f * 0.22f
            in 420..539 -> 0.10f - (minute - 420) / 120f * 0.10f
            in 540..959 -> 0f
            in 960..1079 -> (minute - 960) / 120f * 0.12f
            in 1080..1199 -> 0.12f + (minute - 1080) / 120f * 0.25f
            in 1200..1319 -> 0.37f + (minute - 1200) / 120f * 0.25f
            else -> 0.62f
        }.coerceIn(0f, 0.62f)
    }

    fun twilightTint(minuteOfDay: Int): Float {
        val minute = minuteOfDay.coerceIn(0, WorldClock.MINUTES_PER_DAY - 1)
        return when (minute) {
            in 330..419 -> 0.10f
            in 1020..1139 -> 0.12f
            else -> 0f
        }
    }
}
