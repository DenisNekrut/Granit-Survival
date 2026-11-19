package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointTest {
    @Test
    fun wakeCheckpointRestoresRunInventoryNeedsClockAndWorldResources() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 3)
        engine.inventory.add(ItemType.METAL, 2)
        engine.bunkerStorage.deposit(engine.inventory, ItemType.METAL, 1)
        engine.sleepUntilMorning()

        val checkpointPlayerX = engine.player.x
        val checkpointDay = engine.worldClock.day
        val checkpointNeeds = engine.needs.snapshot()

        engine.player.x += 4f
        engine.inventory.add(ItemType.WOOD, 5)
        engine.bunkerStorage.deposit(engine.inventory, ItemType.METAL, 1)
        engine.needs.advanceAwake(180)
        engine.worldClock.advanceMinutes(180)
        engine.tileMap.collectResource(ResourceLocation(7, 8, ItemType.WOOD))

        assertTrue(engine.respawnAtWakeCheckpoint())

        assertEquals(checkpointPlayerX, engine.player.x, 0.001f)
        assertEquals(3, engine.inventory.count(ItemType.WOOD))
        assertEquals(1, engine.inventory.count(ItemType.METAL))
        assertEquals(1, engine.bunkerStorage.count(ItemType.METAL))
        assertEquals(checkpointNeeds, engine.needs.snapshot())
        assertEquals(checkpointDay, engine.worldClock.day)
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[7, 8])
    }

    @Test
    fun sleepCreatesNewWakeCheckpoint() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WATER, 2)
        val slept = engine.sleepUntilMorning()
        assertTrue(slept > 0)
        assertEquals(7, engine.worldClock.hour)

        engine.inventory.remove(ItemType.WATER, 2)
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(2, engine.inventory.count(ItemType.WATER))
    }
}
