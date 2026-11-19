package com.granit.survival

/** Deterministic outdoor light levels derived from the in-game clock (minute 0..1439). */
internal object DayNightLighting {
    private val darknessKeys = listOf(
        0 to 0.62f,
        270 to 0.62f,
        330 to 0.32f,
        420 to 0.10f,
        540 to 0f,
        960 to 0f,
        1080 to 0.12f,
        1200 to 0.37f,
        1320 to 0.62f,
        1439 to 0.62f
    )

    private val twilightKeys = listOf(
        0 to 0f,
        330 to 0f,
        375 to 0.10f,
        420 to 0f,
        990 to 0f,
        1080 to 0.12f,
        1140 to 0f,
        1439 to 0f
    )

    fun outdoorDarkness(minuteOfDay: Int): Float =
        interpolate(minuteOfDay, darknessKeys).coerceIn(0f, 0.62f)

    fun twilightTint(minuteOfDay: Int): Float =
        interpolate(minuteOfDay, twilightKeys).coerceIn(0f, 0.12f)

    /** Smoothstep interpolation avoids visible brightness jumps at phase boundaries. */
    private fun interpolate(minuteOfDay: Int, keys: List<Pair<Int, Float>>): Float {
        val minute = minuteOfDay.coerceIn(0, WorldClock.MINUTES_PER_DAY - 1)
        val upperIndex = keys.indexOfFirst { it.first >= minute }.let {
            if (it < 0) keys.lastIndex else it
        }
        if (upperIndex == 0) return keys.first().second
        val (startMinute, startValue) = keys[upperIndex - 1]
        val (endMinute, endValue) = keys[upperIndex]
        val span = (endMinute - startMinute).coerceAtLeast(1)
        val linear = ((minute - startMinute).toFloat() / span).coerceIn(0f, 1f)
        val eased = linear * linear * (3f - 2f * linear)
        return startValue + (endValue - startValue) * eased
    }
}
