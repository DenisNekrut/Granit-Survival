package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointOfInterestTest {
    @Test
    fun nearbyLandmarkIsDiscoveredOnlyOnce() {
        val engine = GameEngine()
        val poi = PointOfInterestCatalog.inZone(WorldZone.FOREST).first()
        engine.changeZone(WorldZone.FOREST)
        engine.player.x = poi.x
        engine.player.y = poi.y

        assertEquals(listOf(poi), engine.scanNearbyPointsOfInterest())
        assertTrue(engine.isPoiDiscovered(poi.id))
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
