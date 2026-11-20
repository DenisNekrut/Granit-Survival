package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldContainerTest {
    @Test
    fun forestSupplyCacheIsVisibleAndInteractable() {
        val map = TileMap(zone = WorldZone.FOREST)
        assertEquals(TileType.LOOT_CACHE, map[30, 5])
        assertEquals(TileType.LOOT_CACHE, map.nearestInteractable(30.5f, 5.5f, 1f))
    }

    @Test
    fun cacheLootPersistsAfterLeavingAndReturningToForest() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = 30.5f
        engine.player.y = 5.5f
        val cache = requireNotNull(engine.nearestWorldContainer())
        val before = cache.count(ItemType.WOOD)
        assertTrue(before > 0)
        assertTrue(engine.transferFromWorldContainerAt(cache.id, 0, 1))
        assertEquals(before - 1, cache.count(ItemType.WOOD))
        val backpackCount = engine.inventory.count(ItemType.WOOD)

        engine.changeZone(WorldZone.BUNKER)
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = 30.5f
        engine.player.y = 5.5f
        val returned = requireNotNull(engine.nearestWorldContainer())
        assertEquals(before - 1, returned.count(ItemType.WOOD))
        assertEquals(backpackCount, engine.inventory.count(ItemType.WOOD))
    }

    @Test
    fun cacheContentsSurviveSaveRestoreWithExactSlotLayout() {
        val original = GameEngine()
        original.changeZone(WorldZone.FOREST)
        original.player.x = 30.5f
        original.player.y = 5.5f
        val cache = requireNotNull(original.nearestWorldContainer())
        assertTrue(original.transferFromWorldContainerAt(cache.id, 0, 1))
        val expected = cache.snapshot()

        val restored = GameEngine()
        restored.restoreSaveSnapshot(original.createSaveSnapshot())
        restored.player.x = 30.5f
        restored.player.y = 5.5f
        assertEquals(expected, requireNotNull(restored.nearestWorldContainer()).snapshot())
    }

    @Test
    fun proceduralExpeditionCachesAreSeededDeterministically() {
        val first = GameEngine { 81L }
        first.changeZone(WorldZone.RUINS)
        val firstContainers = first.zoneResourceSnapshot().first { it.zone == WorldZone.RUINS }.containers
        assertTrue(firstContainers.isNotEmpty())
        assertTrue(firstContainers.all { it.contents.any { stack -> stack != null } })

        val second = GameEngine { 81L }
        second.changeZone(WorldZone.RUINS)
        val secondContainers = second.zoneResourceSnapshot().first { it.zone == WorldZone.RUINS }.containers
        assertEquals(firstContainers, secondContainers)
    }
}
