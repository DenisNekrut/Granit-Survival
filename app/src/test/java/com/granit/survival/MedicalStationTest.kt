package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalStationTest {
    private fun placeAtMedicalStation(engine: GameEngine) {
        engine.player.x = 2.5f
        engine.player.y = 7.5f
    }

    @Test
    fun poweredTreatmentConsumesKitAndGameTimeAndRestoresHealth() {
        val engine = GameEngine()
        placeAtMedicalStation(engine)
        engine.needs.applyDamage(60f)
        engine.inventory.add(ItemType.MEDICAL_KIT, 1)
        val startTime = engine.worldClock.totalMinutes
        val startFuel = engine.bunkerPower.fuelMinutesRemaining

        assertTrue(engine.canUseMedicalStation())
        assertTrue(engine.treatAtMedicalStation())
        assertEquals(90f, engine.needs.health, 0.001f)
        assertEquals(0, engine.inventory.count(ItemType.MEDICAL_KIT))
        assertEquals(startTime + GameEngine.MEDICAL_STATION_TREATMENT_MINUTES.toLong(),
            engine.worldClock.totalMinutes)
        assertEquals(startFuel - GameEngine.MEDICAL_STATION_TREATMENT_MINUTES,
            engine.bunkerPower.fuelMinutesRemaining)
    }

    @Test
    fun poweredTreatmentIsUnavailableWithoutPowerAndConsumesNothing() {
        val engine = GameEngine()
        placeAtMedicalStation(engine)
        engine.needs.applyDamage(40f)
        engine.inventory.add(ItemType.MEDICAL_KIT, 1)
        engine.bunkerPower.toggleGenerator()
        val startTime = engine.worldClock.totalMinutes

        assertFalse(engine.canUseMedicalStation())
        assertFalse(engine.treatAtMedicalStation())
        assertEquals(60f, engine.needs.health, 0.001f)
        assertEquals(1, engine.inventory.count(ItemType.MEDICAL_KIT))
        assertEquals(startTime, engine.worldClock.totalMinutes)
    }

    @Test
    fun manualBandageRemainsAvailableDuringPowerOutage() {
        val engine = GameEngine()
        engine.needs.applyDamage(30f)
        engine.inventory.add(ItemType.BANDAGE, 1)
        engine.bunkerPower.toggleGenerator()
        val startTime = engine.worldClock.totalMinutes

        assertTrue(engine.useBandage())
        assertEquals(85f, engine.needs.health, 0.001f)
        assertEquals(0, engine.inventory.count(ItemType.BANDAGE))
        assertEquals(startTime + 3L, engine.worldClock.totalMinutes)
    }

    @Test
    fun failedTimedTreatmentDoesNotConsumeMedicalSupplies() {
        val engine = GameEngine()
        placeAtMedicalStation(engine)
        engine.needs.applyDamage(99f)
        engine.inventory.add(ItemType.MEDICAL_KIT, 1)
        // Preserve the item in the wake checkpoint, then force the timed action to trigger rollback.
        engine.checkpointSystem.capture(engine)
        engine.needs.advanceAwake(100000)

        assertFalse(engine.treatAtMedicalStation())
        assertEquals(1, engine.inventory.count(ItemType.MEDICAL_KIT))
        assertEquals(1f, engine.needs.health, 0.001f)
    }
}
