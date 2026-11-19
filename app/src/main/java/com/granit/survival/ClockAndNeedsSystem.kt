package com.granit.survival

/**
 * Applies the shared world-clock, generator, statistics, needs and psychology rules
 * for both real-time simulation minutes and discrete player actions.
 */
internal class ClockAndNeedsSystem(
    private val clock: WorldClock,
    private val bunkerPower: BunkerPowerSystem,
    private val statistics: CharacterStatistics,
    private val needs: SurvivalNeeds,
    private val psychology: PsychologicalState
) {
    fun advanceActionMinutes(
        minutes: Int,
        zone: WorldZone,
        isMoving: Boolean,
        onDeath: () -> Unit
    ): Boolean {
        if (minutes <= 0) return true
        clock.advanceMinutes(minutes)
        bunkerPower.advanceMinutes(minutes)
        statistics.recordMinutes(minutes, zone, clock.day)
        needs.advanceAwake(minutes)
        psychology.advanceAwake(minutes, isMoving, zone)
        if (needs.health <= 0f) {
            onDeath()
            return false
        }
        return true
    }

    fun advanceRealtimeMinute(zone: WorldZone, isMoving: Boolean): Boolean {
        advanceOneMinute(zone, isMoving)
        return needs.health > 0f
    }

    private fun advanceOneMinute(zone: WorldZone, isMoving: Boolean) {
        clock.advanceMinutes(1)
        bunkerPower.advanceMinutes(1)
        statistics.recordMinutes(1, zone, clock.day)
        needs.advanceAwake(1)
        psychology.advanceAwake(1, isMoving, zone)
    }
}
