package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.json.JSONObject
import com.granit.survival.save.SaveCodec
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
    fun serializedSaveSnapshotRoundTripsThroughCurrentCodec() {
        val original = GameEngine { 808L }
        original.inventory.add(ItemType.WOOD, 23)
        original.inventory.add(ItemType.METAL, 5)
        assertTrue(original.bunkerStorage.deposit(original.inventory, ItemType.WOOD, 7))
        assertTrue(original.bunkerStorage.deposit(original.inventory, ItemType.METAL, 2))
        original.changeZone(WorldZone.FOREST)
        original.player.x = 11f
        original.player.y = 12f
        original.sleepUntilMorning()
        val expected = original.createSaveSnapshot()

        val encoded = SaveCodec.encode(expected.toJson(), 123456L)
        val envelope = SaveCodec.decode(encoded)
        val restored = EngineSaveSnapshot.fromJson(envelope.getJSONObject("state"))

        assertEquals(expected.player, restored.player)
        assertEquals(expected.inventory, restored.inventory)
        assertEquals(expected.bunkerStorage, restored.bunkerStorage)
        assertEquals(expected.needs, restored.needs)
        assertEquals(expected.clock, restored.clock)
        assertEquals(expected.zone, restored.zone)
        assertEquals(expected.zoneResources, restored.zoneResources)
        assertEquals(expected.wakeCheckpoint, restored.wakeCheckpoint)
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
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[1, 8])
        assertTrue(engine.checkpointSystem.hasCheckpoint())
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(4, engine.inventory.count(ItemType.WOOD))
    }

    @Test
    fun explicitStorageTransfersPreserveExactSlotOrderAcrossSaveRestore() {
        val original = GameEngine { 4242L }
        original.inventory.add(ItemType.WOOD, 25)
        original.inventory.add(ItemType.METAL, 5)

        assertTrue(original.bunkerStorage.depositAt(original.inventory, 1, 3))
        assertTrue(original.bunkerStorage.depositAt(original.inventory, 2, 2))

        val expectedInventory = original.inventory.snapshot()
        val expectedStorage = original.bunkerStorage.snapshot()
        assertEquals(ItemStack(ItemType.WOOD, 20), expectedInventory[0])
        assertEquals(ItemStack(ItemType.WOOD, 2), expectedInventory[1])
        assertEquals(ItemStack(ItemType.METAL, 3), expectedInventory[2])
        assertEquals(ItemStack(ItemType.WOOD, 3), expectedStorage[0])
        assertEquals(ItemStack(ItemType.METAL, 2), expectedStorage[1])

        val encoded = SaveCodec.encode(original.createSaveSnapshot().toJson(), 777L)
        val decoded = SaveCodec.decode(encoded)
        val snapshot = EngineSaveSnapshot.fromJson(decoded.getJSONObject("state"))
        val restored = GameEngine { 9999L }
        restored.restoreSaveSnapshot(snapshot)

        assertEquals(expectedInventory, restored.inventory.snapshot())
        assertEquals(expectedStorage, restored.bunkerStorage.snapshot())
        assertEquals(expectedInventory, snapshot.inventory)
        assertEquals(expectedStorage, snapshot.bunkerStorage)
    }

}
