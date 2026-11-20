package com.granit.survival

enum class WeatherType(
    val shortCode: String,
    val forecastText: String,
    val outdoorMovementMultiplier: Float
) {
    CLEAR("CLR", "ясно, видимость хорошая", 1.00f),
    FOG("FOG", "туман, видимость ограничена", 0.94f),
    RAIN("RAIN", "дождь, поверхность скользкая", 0.92f),
    COLD("COLD", "похолодание, двигаться на открытом воздухе тяжелее", 0.95f),
    OVERCAST("OVC", "облачно, существенных изменений не ожидается", 0.98f)
}

/** Weather is derived from the in-game day, so checkpoint rollback restores it automatically. */
object WeatherSystem {
    private val cycle = WeatherType.values()

    fun forDay(day: Int): WeatherType {
        require(day >= 1)
        return cycle[Math.floorMod(day - 1, cycle.size)]
    }
}


/**
 * Stable world-space positions for weather particles.
 * The same particle index and simulation time always produce the same point,
 * so rendering never depends on frame-rate or random-number state.
 */
internal object WeatherParticleField {
    fun position(
        index: Int,
        timeSeconds: Double,
        mapWidth: Int,
        mapHeight: Int,
        fallSpeed: Float,
        horizontalDrift: Float
    ): Pair<Float, Float> {
        require(index >= 0) { "Particle index must be non-negative" }
        require(mapWidth > 0 && mapHeight > 0) { "Weather field dimensions must be positive" }
        val baseX = Math.floorMod(index * 37, mapWidth) +
            (Math.floorMod(index * 17, 100) / 100f)
        val baseY = Math.floorMod(index * 61, mapHeight) +
            (Math.floorMod(index * 29, 100) / 100f)
        val elapsed = timeSeconds.coerceAtLeast(0.0).toFloat()
        val x = (baseX + elapsed * horizontalDrift).mod(mapWidth.toFloat())
        val y = (baseY + elapsed * fallSpeed).mod(mapHeight.toFloat())
        return x to y
    }
}
