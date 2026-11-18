package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurvivalSystemsTest {
    @Test
    fun clockAdvancesAcrossMidnight() {
        val clock = WorldClock(startHour = 23, startMinute = 50)
        clock.advanceMinutes(20)
        assertEquals(2, clock.day)
        assertEquals(0, clock.hour)
        assertEquals(10, clock.minute)
        assertEquals(20L, clock.totalMinutes)
    }

    @Test
    fun sleepingWakesAtSevenAndRestoresEnergy() {
        val clock = WorldClock(startHour = 22)
        val needs = SurvivalNeeds()
        needs.advanceAwake(120)
        val energyBeforeSleep = needs.energy

        val sleptMinutes = clock.sleepUntilMorning()
        needs.sleep(sleptMinutes)

        assertEquals(540, sleptMinutes)
        assertEquals(7, clock.hour)
        assertEquals(2, clock.day)
        assertTrue(needs.energy > energyBeforeSleep)
        assertEquals(100f, needs.energy, 0.001f)
    }

    @Test
    fun foodAndWaterRestoreTheirNeeds() {
        val needs = SurvivalNeeds()
        needs.advanceAwake(500)
        val hungerBefore = needs.hunger
        val thirstBefore = needs.thirst

        needs.consumeFood()
        needs.drinkWater()

        assertTrue(needs.hunger > hungerBefore)
        assertTrue(needs.thirst > thirstBefore)
        assertTrue(needs.hunger <= 100f)
        assertTrue(needs.thirst <= 100f)
    }

    @Test
    fun needsNeverFallBelowZero() {
        val needs = SurvivalNeeds()
        needs.advanceAwake(10_000)
        assertEquals(0f, needs.hunger, 0.001f)
        assertEquals(0f, needs.thirst, 0.001f)
        assertEquals(0f, needs.energy, 0.001f)
        assertEquals(0f, needs.health, 0.001f)
    }
}
