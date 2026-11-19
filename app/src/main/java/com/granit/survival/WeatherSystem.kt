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
