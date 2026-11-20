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

    @Test
    fun cacheLootTierAndContentsAreStableForTheSameId() {
        val first = WorldContainer("ruins_12_8", WorldZone.RUINS, 12, 8, "ТАЙНИК")
        val second = WorldContainer("ruins_12_8", WorldZone.RUINS, 12, 8, "ТАЙНИК")
        first.seedInitialContents()
        second.seedInitialContents()

        assertEquals(first.lootTier, second.lootTier)
        assertEquals(first.snapshot(), second.snapshot())
        assertTrue(first.displayTitle.contains(first.lootTier.label))
    }

    @Test
    fun differentCacheIdsCanProduceDifferentLootSpecializations() {
        val tiers = (0 until 20).map { index ->
            WorldContainer("road_${index}_4", WorldZone.ROAD, index, 4, "ТАЙНИК").lootTier
        }.toSet()

        assertTrue("Loot cache catalog should expose multiple specializations", tiers.size >= 3)
    }

    @Test
    fun medicalAndProvisionCachesContainTheirSignatureSupplies() {
        val medical = (0 until 100).map { index ->
            WorldContainer("medical_$index", WorldZone.RUINS, index, 4, "ТАЙНИК").also {
                it.seedInitialContents()
            }
        }.first { it.lootTier == WorldLootTier.MEDICAL }
        val provisions = (0 until 100).map { index ->
            WorldContainer("provisions_$index", WorldZone.ROAD, index, 4, "ТАЙНИК").also {
                it.seedInitialContents()
            }
        }.first { it.lootTier == WorldLootTier.PROVISIONS }

        assertTrue(medical.count(ItemType.BANDAGE) >= 2)
        assertTrue(medical.count(ItemType.MEDICAL_KIT) >= 1)
        assertTrue(provisions.count(ItemType.CANNED_FOOD) >= 3)
        assertTrue(provisions.count(ItemType.WATER) >= 3)
    }

    @Test
    fun authoredLandmarksExposeSpecializedLootCaches() {
        val cases = listOf(
            Triple(WorldZone.FOREST, "forest_camp", WorldLootTier.PROVISIONS),
            Triple(WorldZone.FOREST, "forest_radio_cache", WorldLootTier.MATERIALS),
            Triple(WorldZone.RUINS, "ruins_shelter", WorldLootTier.MEDICAL),
            Triple(WorldZone.RUINS, "ruins_generator_room", WorldLootTier.MATERIALS),
            Triple(WorldZone.ROAD, "road_vehicle", WorldLootTier.RARE)
        )

        cases.forEach { (zone, poiId, expectedTier) ->
            val poi = requireNotNull(PointOfInterestCatalog.byId(poiId))
            val engine = GameEngine()
            engine.changeZone(zone)
            engine.player.x = poi.x
            engine.player.y = poi.y

            val cache = requireNotNull(engine.nearestWorldContainer())
            assertEquals("poi_$poiId", cache.id)
            assertEquals(expectedTier, cache.lootTier)
            assertTrue(cache.snapshot().any { it != null })
        }
    }

    @Test
    fun landmarkCacheContentsSurviveSaveRestore() {
        val poi = requireNotNull(PointOfInterestCatalog.byId("ruins_shelter"))
        val original = GameEngine()
        original.changeZone(WorldZone.RUINS)
        original.player.x = poi.x
        original.player.y = poi.y
        val expected = requireNotNull(original.nearestWorldContainer()).snapshot()

        val restored = GameEngine()
        restored.restoreSaveSnapshot(original.createSaveSnapshot())
        restored.changeZone(WorldZone.RUINS, regenerate = false)
        restored.player.x = poi.x
        restored.player.y = poi.y

        val actual = requireNotNull(restored.nearestWorldContainer())
        assertEquals("poi_ruins_shelter", actual.id)
        assertEquals(WorldLootTier.MEDICAL, actual.lootTier)
        assertEquals(expected, actual.snapshot())
    }
}
