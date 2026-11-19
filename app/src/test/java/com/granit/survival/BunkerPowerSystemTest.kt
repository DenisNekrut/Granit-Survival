package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerPowerSystemTest {
    @Test
    fun generatorConsumesFuelOnlyWhileRunning() {
        val power = BunkerPowerSystem(fuelMinutesRemaining = 120)
        power.advanceMinutes(30)
        assertEquals(90, power.fuelMinutesRemaining)
        assertTrue(power.toggleGenerator())
        power.advanceMinutes(45)
        assertEquals(90, power.fuelMinutesRemaining)
        assertFalse(power.hasPower)
        assertTrue(power.emergencyLighting)
    }

    @Test
    fun emptyGeneratorCannotRestartUntilRefueled() {
        val power = BunkerPowerSystem(generatorRunning = false, fuelMinutesRemaining = 0)
        assertFalse(power.toggleGenerator())
        assertTrue(power.addFuel(60))
        assertTrue(power.toggleGenerator())
        assertTrue(power.hasPower)
    }

    @Test
    fun refuelingIsCappedAndRejectsInvalidAmounts() {
        val power = BunkerPowerSystem(generatorRunning = false, fuelMinutesRemaining = 2800)
        assertTrue(power.addFuel(500))
        assertEquals(BunkerPowerSystem.MAX_FUEL_MINUTES, power.fuelMinutesRemaining)
        assertFalse(power.addFuel(10))
        assertFalse(power.addFuel(0))
    }

    @Test
    fun engineRefuelingUsesWoodAndConsumesTwoGameMinutes() {
        val engine = GameEngine()
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        engine.inventory.add(ItemType.WOOD, 1)
        val startTime = engine.worldClock.totalMinutes
        val startFuel = engine.bunkerPower.fuelMinutesRemaining

        assertTrue(engine.refuelGenerator())
        assertEquals(0, engine.inventory.count(ItemType.WOOD))
        assertEquals(startTime + 2L, engine.worldClock.totalMinutes)
        assertEquals(startFuel + BunkerPowerSystem.WOOD_FUEL_MINUTES - 2,
            engine.bunkerPower.fuelMinutesRemaining)
    }

    @Test
    fun sleepingConsumesGeneratorFuelAndFallsBackToEmergencyLighting() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        assertEquals(0, engine.bunkerPower.fuelMinutesRemaining)
        assertTrue(engine.bunkerPower.emergencyLighting)
    }

    @Test
    fun wakeCheckpointRestoresGeneratorAndFuel() {
        val engine = GameEngine()
        engine.player.x = 13.5f
        engine.player.y = 2.5f
        assertTrue(engine.toggleGenerator())
        engine.bunkerPower.advanceMinutes(100)
        assertEquals(BunkerPowerSystem.DEFAULT_FUEL_MINUTES, engine.bunkerPower.fuelMinutesRemaining)
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertTrue(engine.bunkerPower.generatorRunning)
        assertEquals(BunkerPowerSystem.DEFAULT_FUEL_MINUTES, engine.bunkerPower.fuelMinutesRemaining)
    }

    @Test
    fun workbenchRequiresPowerButManualReadingDoesNot() {
        val engine = GameEngine()
        engine.player.x = 8.5f
        engine.player.y = 7.5f
        assertTrue(engine.canUseWorkbench())

        engine.player.x = 13.5f
        engine.player.y = 2.5f
        assertTrue(engine.toggleGenerator())
        engine.player.x = 8.5f
        engine.player.y = 7.5f
        assertFalse(engine.canUseWorkbench())
        engine.player.x = 9.5f
        engine.player.y = 2.5f
        assertTrue(engine.canAccessBookshelf())
    }

    @Test
    fun saveSnapshotRestoresGeneratorState() {
        val original = GameEngine()
        original.bunkerPower.toggleGenerator()
        original.bunkerPower.advanceMinutes(80)
        val snapshot = original.createSaveSnapshot()

        val restored = GameEngine()
        restored.restoreSaveSnapshot(snapshot)
        assertFalse(restored.bunkerPower.generatorRunning)
        assertEquals(original.bunkerPower.fuelMinutesRemaining, restored.bunkerPower.fuelMinutesRemaining)
        assertTrue(restored.bunkerPower.emergencyLighting)
    }
}
