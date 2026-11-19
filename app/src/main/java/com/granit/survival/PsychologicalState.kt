package com.granit.survival

data class PsychologicalStateSnapshot(
    val stress: Float = 10f,
    val loneliness: Float = 0f,
    val fatigue: Float = 0f,
    val morale: Float = 70f
)

/** Four independent, reversible psychological indicators on a 0..100 scale. */
class PsychologicalState {
    var stress: Float = 10f
        private set
    var loneliness: Float = 0f
        private set
    var fatigue: Float = 0f
        private set
    var morale: Float = 70f
        private set

    /** Fatigue and stress gently reduce travel speed; the penalty is capped. */
    val movementSpeedMultiplier: Float
        get() = (1f - fatigue * 0.0012f - stress * 0.0003f).coerceIn(0.82f, 1f)

    fun advanceAwake(minutes: Int, moving: Boolean, zone: WorldZone) {
        require(minutes >= 0)
        if (minutes == 0) return
        loneliness = (loneliness + LONELINESS_PER_MINUTE * minutes).coerceAtMost(100f)
        fatigue = (fatigue + FATIGUE_PER_MINUTE * minutes +
            if (moving) MOVEMENT_FATIGUE_PER_MINUTE * minutes else 0f).coerceAtMost(100f)
        val stressDelta = if (zone == WorldZone.BUNKER) {
            -BUNKER_STRESS_RECOVERY_PER_MINUTE * minutes
        } else {
            EXPEDITION_STRESS_PER_MINUTE * minutes
        }
        stress = (stress + stressDelta).coerceIn(0f, 100f)
        morale = (morale - MORALE_DRIFT_PER_MINUTE * minutes).coerceAtLeast(0f)
    }

    fun sleep(minutes: Int) {
        require(minutes >= 0)
        stress = (stress - SLEEP_STRESS_RECOVERY_PER_MINUTE * minutes).coerceAtLeast(0f)
        fatigue = (fatigue - SLEEP_FATIGUE_RECOVERY_PER_MINUTE * minutes).coerceAtLeast(0f)
        morale = (morale + SLEEP_MORALE_GAIN).coerceAtMost(100f)
    }

    fun onZoneChange(from: WorldZone, to: WorldZone) {
        if (from == to) return
        if (to == WorldZone.BUNKER) {
            stress = (stress - RETURN_STRESS_RECOVERY).coerceAtLeast(0f)
            morale = (morale + RETURN_MORALE_GAIN).coerceAtMost(100f)
        } else {
            stress = (stress + EXPEDITION_ENTRY_STRESS).coerceAtMost(100f)
        }
    }

    fun applyReading(moraleGain: Float, stressRelief: Float) {
        morale = (morale + moraleGain.coerceAtLeast(0f)).coerceAtMost(100f)
        stress = (stress - stressRelief.coerceAtLeast(0f)).coerceAtLeast(0f)
    }

    fun restore(snapshot: PsychologicalStateSnapshot) {
        stress = snapshot.stress.coerceIn(0f, 100f)
        loneliness = snapshot.loneliness.coerceIn(0f, 100f)
        fatigue = snapshot.fatigue.coerceIn(0f, 100f)
        morale = snapshot.morale.coerceIn(0f, 100f)
    }

    fun snapshot() = PsychologicalStateSnapshot(stress, loneliness, fatigue, morale)

    companion object {
        const val LONELINESS_PER_MINUTE = 0.008f
        const val FATIGUE_PER_MINUTE = 0.018f
        const val MOVEMENT_FATIGUE_PER_MINUTE = 0.012f
        const val EXPEDITION_STRESS_PER_MINUTE = 0.025f
        const val BUNKER_STRESS_RECOVERY_PER_MINUTE = 0.018f
        const val MORALE_DRIFT_PER_MINUTE = 0.002f
        const val SLEEP_STRESS_RECOVERY_PER_MINUTE = 0.08f
        const val SLEEP_FATIGUE_RECOVERY_PER_MINUTE = 0.16f
        const val SLEEP_MORALE_GAIN = 3f
        const val RETURN_STRESS_RECOVERY = 8f
        const val RETURN_MORALE_GAIN = 2f
        const val EXPEDITION_ENTRY_STRESS = 3f
    }
}
