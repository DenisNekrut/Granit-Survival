package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointOfInterestTest {
    @Test
    fun everyLandmarkIsPlacedOnWalkableGround() {
        PointOfInterestCatalog.all().forEach { poi ->
            assertTrue("${poi.id} must be reachable on its zone map",
                TileMap(zone = poi.zone).isWalkable(poi.x, poi.y))
        }
    }

    @Test
    fun everyLandmarkRemainsWalkableAcrossProceduralSeeds() {
        listOf(WorldZone.RUINS, WorldZone.ROAD).forEach { zone ->
            (1L..100L).forEach { seed ->
                val map = TileMap(zone = zone, seed = seed)
                PointOfInterestCatalog.inZone(zone).forEach { poi ->
                    assertTrue("${poi.id} must remain reachable for seed=$seed",
                        map.isWalkable(poi.x, poi.y))
                }
            }
        }
    }

    @Test
    fun catalogContainsUniqueIdsAndMultipleLandmarksPerOutdoorZone() {
        val all = PointOfInterestCatalog.all()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        listOf(WorldZone.FOREST, WorldZone.RUINS, WorldZone.ROAD).forEach { zone ->
            assertTrue("${zone.name} should contain at least four landmarks",
                PointOfInterestCatalog.inZone(zone).size >= 4)
        }
    }

    @Test
    fun everyLandmarkUsesAnExistingPixelArtSprite() {
        PointOfInterestCatalog.all().forEach { poi ->
            assertTrue("${poi.id} sprite ${poi.spriteId} must exist",
                PixelSpriteAtlas.sprite(poi.spriteId) != null)
        }
    }

    @Test
    fun nearbyLandmarkIsDiscoveredOnlyOnce() {
        val engine = GameEngine()
        val poi = PointOfInterestCatalog.inZone(WorldZone.FOREST).first()
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = poi.x
        engine.player.y = poi.y

        assertEquals(listOf(poi), engine.scanNearbyPointsOfInterest())
        assertTrue(engine.isPoiDiscovered(poi.id))
        assertEquals(poi.hudLabel, engine.lastDiscoveryHudLabel)
        assertTrue(engine.scanNearbyPointsOfInterest().isEmpty())
    }

    @Test
    fun discoveriesAreRolledBackToWakeCheckpoint() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        val poi = PointOfInterestCatalog.inZone(WorldZone.FOREST).first()
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = poi.x
        engine.player.y = poi.y
        engine.scanNearbyPointsOfInterest()
        assertTrue(engine.isPoiDiscovered(poi.id))

        assertTrue(engine.respawnAtWakeCheckpoint())
        assertFalse(engine.isPoiDiscovered(poi.id))
        assertTrue(engine.discoveredPointsOfInterest().isEmpty())
    }

    @Test
    fun discoveryDetailsAreAvailableForJournalAndUnknownIdsAreIgnored() {
        val engine = GameEngine()
        val forestPoi = PointOfInterestCatalog.inZone(WorldZone.FOREST).first()
        val roadPoi = PointOfInterestCatalog.inZone(WorldZone.ROAD).first()
        engine.restoreDiscoveredPointsOfInterest(setOf(roadPoi.id, forestPoi.id, "unknown_id"))

        assertEquals(listOf(forestPoi, roadPoi), engine.discoveredPointDetails())
    }

    @Test
    fun discoveriesSurviveSaveSnapshotRestore() {
        val engine = GameEngine()
        val poi = PointOfInterestCatalog.inZone(WorldZone.RUINS).first()
        engine.changeZone(WorldZone.RUINS)
        engine.player.x = poi.x
        engine.player.y = poi.y
        engine.scanNearbyPointsOfInterest()
        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())

        assertTrue(restored.isPoiDiscovered(poi.id))
        assertEquals(setOf(poi.id), restored.discoveredPointsOfInterest())
    }
}
