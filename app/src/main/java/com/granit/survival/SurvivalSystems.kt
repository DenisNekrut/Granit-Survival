package com.granit.survival

/** In-game clock. One real second is mapped to one in-game minute by GameEngine. */
data class WorldClockSnapshot(
    val day: Int,
    val minuteOfDay: Int,
    val totalMinutes: Long
)

class WorldClock(
    startDay: Int = 1,
    startHour: Int = 7,
    startMinute: Int = 0
) {
    var day: Int = startDay
        private set
    var minuteOfDay: Int = startHour * 60 + startMinute
        private set
    var totalMinutes: Long = 0L
        private set

    val hour: Int get() = minuteOfDay / 60
    val minute: Int get() = minuteOfDay % 60

    fun advanceMinutes(minutes: Int) {
        require(minutes >= 0) { "Minutes cannot be negative" }
        if (minutes == 0) return
        val absolute = minuteOfDay.toLong() + minutes
        val crossedDays = absolute / MINUTES_PER_DAY
        minuteOfDay = (absolute % MINUTES_PER_DAY).toInt()
        day += crossedDays.toInt()
        totalMinutes += minutes
    }

    /** Wake at 07:00 on the next morning, advancing at least one day. */
    fun sleepUntilMorning(): Int {
        val target = 7 * 60
        val duration = if (minuteOfDay < target) {
            target - minuteOfDay
        } else {
            MINUTES_PER_DAY - minuteOfDay + target
        }
        advanceMinutes(duration)
        return duration
    }

    fun snapshot() = WorldClockSnapshot(day, minuteOfDay, totalMinutes)

    fun restore(snapshot: WorldClockSnapshot) {
        require(snapshot.day >= 1)
        require(snapshot.minuteOfDay in 0 until MINUTES_PER_DAY)
        require(snapshot.totalMinutes >= 0L)
        day = snapshot.day
        minuteOfDay = snapshot.minuteOfDay
        totalMinutes = snapshot.totalMinutes
    }

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
    }
}

data class SurvivalNeedsSnapshot(
    val health: Float,
    val hunger: Float,
    val thirst: Float,
    val energy: Float
)

/** Needs are normalized to 0..100. Rates are centralized for future balancing. */
class SurvivalNeeds {
    var health: Float = 100f
        private set
    var hunger: Float = 100f
        private set
    var thirst: Float = 100f
        private set
    var energy: Float = 100f
        private set

    fun advanceAwake(minutes: Int) {
        require(minutes >= 0)
        if (minutes == 0) return
        hunger = (hunger - HUNGER_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        thirst = (thirst - THIRST_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        energy = (energy - ENERGY_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        if (hunger <= 0f || thirst <= 0f) {
            health = (health - HEALTH_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        }
    }

    fun restoreHealth(amount: Float) {
        require(amount >= 0f)
        health = (health + amount).coerceAtMost(100f)
    }

    fun consumeFood() {
        hunger = (hunger + FOOD_HUNGER_RESTORE).coerceAtMost(100f)
    }

    fun drinkWater() {
        thirst = (thirst + WATER_THIRST_RESTORE).coerceAtMost(100f)
    }

    fun sleep(minutes: Int) {
        require(minutes >= 0)
        if (minutes == 0) return
        energy = (energy + SLEEP_ENERGY_GAIN_PER_MINUTE * minutes).coerceAtMost(100f)
        hunger = (hunger - SLEEP_HUNGER_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        thirst = (thirst - SLEEP_THIRST_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        if (hunger <= 0f || thirst <= 0f) {
            health = (health - HEALTH_LOSS_PER_MINUTE * minutes).coerceAtLeast(0f)
        }
    }

    fun restore(snapshot: SurvivalNeedsSnapshot) {
        health = snapshot.health.coerceIn(0f, 100f)
        hunger = snapshot.hunger.coerceIn(0f, 100f)
        thirst = snapshot.thirst.coerceIn(0f, 100f)
        energy = snapshot.energy.coerceIn(0f, 100f)
    }

    fun snapshot() = SurvivalNeedsSnapshot(health, hunger, thirst, energy)

    companion object {
        const val HUNGER_LOSS_PER_MINUTE = 0.035f
        const val THIRST_LOSS_PER_MINUTE = 0.055f
        const val ENERGY_LOSS_PER_MINUTE = 0.045f
        const val SLEEP_ENERGY_GAIN_PER_MINUTE = 0.22f
        const val SLEEP_HUNGER_LOSS_PER_MINUTE = 0.025f
        const val SLEEP_THIRST_LOSS_PER_MINUTE = 0.04f
        const val HEALTH_LOSS_PER_MINUTE = 0.04f
        const val FOOD_HUNGER_RESTORE = 42f
        const val WATER_THIRST_RESTORE = 48f
    }
}
