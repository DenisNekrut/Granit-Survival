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

    /** Low energy slows movement gradually without making the player immobile. */
    val movementSpeedMultiplier: Float
        get() = 0.55f + 0.45f * (energy / 100f).coerceIn(0f, 1f)

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

    fun applyDamage(amount: Float) {
        require(amount >= 0f)
        health = (health - amount).coerceAtLeast(0f)
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


/** Reversible character statistics. The wake checkpoint restores these counters on death. */
data class CharacterStatisticsSnapshot(
    val daysSurvived: Int = 1,
    val expeditionMinutes: Long = 0L,
    val distanceTravelled: Float = 0f,
    val resourcesCollected: Int = 0,
    val itemsCrafted: Int = 0,
    val booksRead: Int = 0,
    val visitedZones: Set<WorldZone> = setOf(WorldZone.BUNKER)
)

class CharacterStatistics {
    var daysSurvived: Int = 1
        private set
    var expeditionMinutes: Long = 0L
        private set
    var distanceTravelled: Float = 0f
        private set
    var resourcesCollected: Int = 0
        private set
    var itemsCrafted: Int = 0
        private set
    var booksRead: Int = 0
        private set
    private val visited = linkedSetOf(WorldZone.BUNKER)

    val visitedZones: Set<WorldZone> get() = visited.toSet()

    fun visitZone(zone: WorldZone) {
        visited += zone
    }

    fun recordMinutes(minutes: Int, zone: WorldZone, currentDay: Int) {
        if (minutes <= 0) return
        daysSurvived = maxOf(daysSurvived, currentDay)
        if (zone != WorldZone.BUNKER) expeditionMinutes += minutes.toLong()
    }

    fun recordDistance(distance: Float) {
        if (distance.isFinite() && distance > 0f) distanceTravelled += distance
    }

    fun recordResourceCollected(amount: Int = 1) {
        resourcesCollected += amount.coerceAtLeast(0)
    }

    fun recordCrafted(amount: Int = 1) {
        itemsCrafted += amount.coerceAtLeast(0)
    }

    fun recordBookRead() {
        booksRead++
    }

    fun snapshot() = CharacterStatisticsSnapshot(
        daysSurvived = daysSurvived,
        expeditionMinutes = expeditionMinutes,
        distanceTravelled = distanceTravelled,
        resourcesCollected = resourcesCollected,
        itemsCrafted = itemsCrafted,
        booksRead = booksRead,
        visitedZones = visited.toSet()
    )

    fun restore(snapshot: CharacterStatisticsSnapshot) {
        daysSurvived = snapshot.daysSurvived.coerceAtLeast(1)
        expeditionMinutes = snapshot.expeditionMinutes.coerceAtLeast(0L)
        distanceTravelled = snapshot.distanceTravelled.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        resourcesCollected = snapshot.resourcesCollected.coerceAtLeast(0)
        itemsCrafted = snapshot.itemsCrafted.coerceAtLeast(0)
        booksRead = snapshot.booksRead.coerceAtLeast(0)
        visited.clear()
        visited.addAll(snapshot.visitedZones)
        visited += WorldZone.BUNKER
    }
}
