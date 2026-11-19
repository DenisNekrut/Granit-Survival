package com.granit.survival

/** Reversible bunker power state. Fuel is measured in remaining powered minutes. */
data class BunkerPowerSnapshot(
    val generatorRunning: Boolean = true,
    val fuelMinutesRemaining: Int = BunkerPowerSystem.DEFAULT_FUEL_MINUTES
)

class BunkerPowerSystem(
    generatorRunning: Boolean = true,
    fuelMinutesRemaining: Int = DEFAULT_FUEL_MINUTES
) {
    var generatorRunning: Boolean = generatorRunning
        private set
    var fuelMinutesRemaining: Int = fuelMinutesRemaining.coerceIn(0, MAX_FUEL_MINUTES)
        private set

    val hasPower: Boolean
        get() = generatorRunning && fuelMinutesRemaining > 0

    val emergencyLighting: Boolean
        get() = !hasPower

    val fuelHoursRemaining: Int
        get() = (fuelMinutesRemaining + 59) / 60

    fun toggleGenerator(): Boolean {
        if (generatorRunning) {
            generatorRunning = false
            return true
        }
        if (fuelMinutesRemaining <= 0) return false
        generatorRunning = true
        return true
    }

    fun addFuel(minutes: Int): Boolean {
        if (minutes <= 0 || fuelMinutesRemaining >= MAX_FUEL_MINUTES) return false
        fuelMinutesRemaining = (fuelMinutesRemaining + minutes).coerceAtMost(MAX_FUEL_MINUTES)
        return true
    }

    fun advanceMinutes(minutes: Int) {
        require(minutes >= 0)
        if (minutes == 0 || !generatorRunning || fuelMinutesRemaining == 0) return
        fuelMinutesRemaining = (fuelMinutesRemaining - minutes).coerceAtLeast(0)
    }

    fun snapshot() = BunkerPowerSnapshot(generatorRunning, fuelMinutesRemaining)

    fun restore(snapshot: BunkerPowerSnapshot) {
        fuelMinutesRemaining = snapshot.fuelMinutesRemaining.coerceIn(0, MAX_FUEL_MINUTES)
        generatorRunning = snapshot.generatorRunning
    }

    companion object {
        const val DEFAULT_FUEL_MINUTES = 720
        const val WOOD_FUEL_MINUTES = 240
        const val MAX_FUEL_MINUTES = 2880
    }
}
