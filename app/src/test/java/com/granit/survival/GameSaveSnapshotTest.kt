package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSaveSnapshotTest {
    @Test
    fun saveSnapshotRestoresEquipmentAndCharacterStatistics() {
        val original = GameEngine { 808L }
        original.changeZone(WorldZone.RUINS)
        original.statistics.recordMinutes(75, WorldZone.RUINS, 2)
        original.statistics.recordDistance(42.5f)
        original.statistics.recordResourceCollected(6)
        original.statistics.recordCrafted(2)
        original.statistics.recordBookRead()
        val expectedStatistics = original.statistics.snapshot()
        val expectedEquipment = original.equipment.snapshot()
        val snapshot = original.createSaveSnapshot()

        val restored = GameEngine { 909L }
        restored.restoreSaveSnapshot(snapshot)

        assertEquals(expectedEquipment, restored.equipment.snapshot())
        assertEquals(expectedStatistics, restored.statistics.snapshot())
        assertEquals(808L, restored.tileMap.layoutSeed)
    }

    @Test
    fun restoringSnapshotReturnsCurrentStateAndWakeCheckpoint() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.WOOD, 4)
        engine.inventory.add(ItemType.METAL, 2)
        engine.bookLibrary.markRead("old_novel")
        engine.radioSystem.markListened(1)
        engine.sleepUntilMorning()
        val saved = engine.createSaveSnapshot()

        engine.player.x = 18f
        engine.inventory.remove(ItemType.WOOD, 4)
        engine.needs.advanceAwake(240)
        engine.worldClock.advanceMinutes(240)
        engine.tileMap.collectResource(ResourceLocation(7, 8, ItemType.WOOD))

        engine.restoreSaveSnapshot(saved)

        assertEquals(saved.player.x, engine.player.x, 0.001f)
        assertEquals(4, engine.inventory.count(ItemType.WOOD))
        assertEquals(saved.clock, engine.worldClock.snapshot())
        assertEquals(saved.needs, engine.needs.snapshot())
        assertEquals(saved.bookLibrary, engine.bookLibrary.snapshot())
        assertEquals(saved.radio, engine.radioSystem.snapshot())
        assertEquals(saved.psychology, engine.psychology.snapshot())
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[7, 8])
        assertTrue(engine.checkpointSystem.hasCheckpoint())
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(4, engine.inventory.count(ItemType.WOOD))
    }
}
