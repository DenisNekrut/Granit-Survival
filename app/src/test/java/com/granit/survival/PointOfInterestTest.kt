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
    fun chosenRadioTheoryProducesDistinctRelayObservationAndPersists() {
        val engine = GameEngine()
        engine.storyProgress.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)
        assertTrue(engine.storyProgress.chooseFirstTheory(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE))
        engine.changeZone(WorldZone.FOREST)
        val relay = PointOfInterestCatalog.byId("forest_radio_cache")!!
        engine.player.x = relay.x
        engine.player.y = relay.y

        assertTrue(engine.scanNearbyPointsOfInterest().contains(relay))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED))

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))
    }

    @Test
    fun chosenEquipmentTheoryProducesDistinctRelayObservationAndPersists() {
        val engine = GameEngine()
        engine.storyProgress.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)
        assertTrue(engine.storyProgress.chooseFirstTheory(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST))
        engine.changeZone(WorldZone.FOREST)
        val relay = PointOfInterestCatalog.byId("forest_radio_cache")!!
        engine.player.x = relay.x
        engine.player.y = relay.y

        assertTrue(engine.scanNearbyPointsOfInterest().contains(relay))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED))
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED))
    }

    @Test
    fun theoryChosenAfterRelayDiscoveryStillProducesBranchObservation() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.FOREST)
        val relay = PointOfInterestCatalog.byId("forest_radio_cache")!!
        engine.player.x = relay.x
        engine.player.y = relay.y
        engine.scanNearbyPointsOfInterest()
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))

        engine.storyProgress.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)
        assertTrue(engine.storyProgress.chooseFirstTheory(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE))
        engine.scanNearbyPointsOfInterest()

        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))
    }

    @Test
    fun relayDiscoveryDoesNotCreateBranchObservationWithoutChosenTheory() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.FOREST)
        val relay = PointOfInterestCatalog.byId("forest_radio_cache")!!
        engine.player.x = relay.x
        engine.player.y = relay.y

        engine.scanNearbyPointsOfInterest()
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND))
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED))
    }

    @Test
    fun forestRelayCannotBeInspectedBeforeDiscoveryAndInspectionPersists() {
        val engine = GameEngine()
        assertFalse(engine.inspectDiscoveredForestRelay())

        engine.restoreDiscoveredPointsOfInterest(setOf("forest_radio_cache"))
        assertTrue(engine.inspectDiscoveredForestRelay())
        assertFalse(engine.inspectDiscoveredForestRelay())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.FOREST_RELAY_INSPECTED))
    }

    @Test
    fun relayRepairRequiresInspectionAndMaterialsAndPersists() {
        val engine = GameEngine()
        engine.restoreDiscoveredPointsOfInterest(setOf("forest_radio_cache"))
        assertFalse(engine.canRepairDiscoveredForestRelay())
        assertFalse(engine.repairDiscoveredForestRelay())

        assertTrue(engine.inspectDiscoveredForestRelay())
        engine.inventory.add(ItemType.METAL, GameEngine.FOREST_RELAY_REPAIR_METAL)
        engine.inventory.add(ItemType.FABRIC, GameEngine.FOREST_RELAY_REPAIR_FABRIC)
        assertTrue(engine.canRepairDiscoveredForestRelay())
        assertTrue(engine.repairDiscoveredForestRelay())
        assertFalse(engine.canRepairDiscoveredForestRelay())
        assertEquals(0, engine.inventory.count(ItemType.METAL))
        assertEquals(0, engine.inventory.count(ItemType.FABRIC))

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.FOREST_RELAY_REPAIRED))
        assertFalse(restored.canRepairDiscoveredForestRelay())
    }

    @Test
    fun repairedRelayUnlocksWatchPostSignalCheckAndPersistsOnce() {
        val engine = GameEngine()
        engine.restoreDiscoveredPointsOfInterest(setOf("forest_radio_cache"))
        assertFalse(engine.inspectForestWatchPostSignal())

        assertTrue(engine.inspectDiscoveredForestRelay())
        engine.inventory.add(ItemType.METAL, GameEngine.FOREST_RELAY_REPAIR_METAL)
        engine.inventory.add(ItemType.FABRIC, GameEngine.FOREST_RELAY_REPAIR_FABRIC)
        assertTrue(engine.repairDiscoveredForestRelay())
        assertFalse(engine.inspectForestWatchPostSignal())

        engine.restoreDiscoveredPointsOfInterest(setOf("forest_radio_cache", "forest_watch_post"))
        assertTrue(engine.inspectForestWatchPostSignal())
        assertFalse(engine.inspectForestWatchPostSignal())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED))
    }

    @Test
    fun watchPostSignalLeadsToRoadMapClueAndPersistsOnce() {
        val engine = GameEngine()
        assertFalse(engine.inspectRoadSignalMap())

        engine.storyProgress.recordEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)
        assertFalse(engine.inspectRoadSignalMap())
        engine.restoreDiscoveredPointsOfInterest(setOf("road_vehicle"))
        assertTrue(engine.inspectRoadSignalMap())
        assertFalse(engine.inspectRoadSignalMap())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND))
    }

    @Test
    fun ruinsSignalMarkerIsHiddenUntilMapClueAndThenPersists() {
        val engine = GameEngine()
        val marker = PointOfInterestCatalog.byId("ruins_checkpoint")!!
        engine.changeZone(WorldZone.RUINS)
        engine.player.x = marker.x
        engine.player.y = marker.y

        assertFalse(engine.scanNearbyPointsOfInterest().contains(marker))
        assertFalse(engine.isPoiDiscovered(marker.id))
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED))

        engine.storyProgress.recordEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)
        assertTrue(engine.scanNearbyPointsOfInterest().contains(marker))
        assertTrue(engine.isPoiDiscovered(marker.id))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED))
        assertTrue(engine.scanNearbyPointsOfInterest().isEmpty())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.isPoiDiscovered(marker.id))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED))
    }

    @Test
    fun ruinsSignalMarkerInspectionRequiresDiscoveryAndPersistsOnce() {
        val engine = GameEngine()
        assertFalse(engine.inspectRuinsSignalMarker())

        engine.storyProgress.recordEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)
        val marker = PointOfInterestCatalog.byId("ruins_checkpoint")!!
        engine.changeZone(WorldZone.RUINS)
        engine.player.x = marker.x
        engine.player.y = marker.y
        assertTrue(engine.scanNearbyPointsOfInterest().contains(marker))

        assertTrue(engine.inspectRuinsSignalMarker())
        assertFalse(engine.inspectRuinsSignalMarker())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED))
    }

    @Test
    fun outerRouteUnlocksOnlyAfterMarkerInspectionAndPersistsOnDiscovery() {
        val engine = GameEngine()
        val marker = PointOfInterestCatalog.byId("ruins_checkpoint")!!
        val outerRoute = PointOfInterestCatalog.byId("ruins_outer_route")!!
        engine.storyProgress.recordEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)
        engine.changeZone(WorldZone.RUINS)
        engine.player.x = marker.x
        engine.player.y = marker.y

        val firstDiscovery = engine.scanNearbyPointsOfInterest()
        assertTrue(firstDiscovery.contains(marker))
        assertFalse(firstDiscovery.contains(outerRoute))
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND))

        assertTrue(engine.inspectRuinsSignalMarker())
        assertFalse(engine.inspectRuinsOuterRoute())
        engine.player.x = outerRoute.x
        engine.player.y = outerRoute.y
        assertTrue(engine.scanNearbyPointsOfInterest().contains(outerRoute))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND))
        assertTrue(engine.inspectRuinsOuterRoute())
        assertFalse(engine.inspectRuinsOuterRoute())
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED))

        val serviceAccess = PointOfInterestCatalog.byId("ruins_service_access")!!
        assertFalse(engine.inspectRuinsServiceAccess())
        assertFalse(engine.scanNearbyPointsOfInterest().contains(serviceAccess))
        engine.player.x = serviceAccess.x
        engine.player.y = serviceAccess.y
        assertTrue(engine.scanNearbyPointsOfInterest().contains(serviceAccess))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND))
        assertTrue(engine.inspectRuinsServiceAccess())
        assertFalse(engine.inspectRuinsServiceAccess())
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED))

        val junction = PointOfInterestCatalog.byId("ruins_power_junction")!!
        assertFalse(engine.scanNearbyPointsOfInterest().contains(junction))
        engine.player.x = junction.x
        engine.player.y = junction.y
        assertTrue(engine.scanNearbyPointsOfInterest().contains(junction))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_FOUND))
        assertTrue(engine.inspectRuinsPowerJunction())
        assertFalse(engine.inspectRuinsPowerJunction())
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_INSPECTED))

        assertFalse(engine.canRestoreRuinsServicePower())
        assertFalse(engine.restoreRuinsServicePower())
        engine.inventory.add(ItemType.METAL, GameEngine.RUINS_SERVICE_POWER_REPAIR_METAL)
        engine.inventory.add(ItemType.FABRIC, GameEngine.RUINS_SERVICE_POWER_REPAIR_FABRIC)
        assertTrue(engine.canRestoreRuinsServicePower())
        assertTrue(engine.restoreRuinsServicePower())
        assertFalse(engine.canRestoreRuinsServicePower())
        assertEquals(0, engine.inventory.count(ItemType.METAL))
        assertEquals(0, engine.inventory.count(ItemType.FABRIC))
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_POWER_RESTORED))

        assertTrue(engine.openRuinsServiceAccess())
        assertFalse(engine.openRuinsServiceAccess())
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_OPENED))

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.isPoiDiscovered(outerRoute.id))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED))
        assertTrue(restored.isPoiDiscovered(junction.id))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_FOUND))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_INSPECTED))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_POWER_RESTORED))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_OPENED))
    }

    @Test
    fun discoveriesSurviveSaveSnapshotRestore() {
        val engine = GameEngine()
        val poi = PointOfInterestCatalog.inZone(WorldZone.RUINS).first { it.requiredStoryEventId == null }
        engine.changeZone(WorldZone.RUINS)
        engine.player.x = poi.x
        engine.player.y = poi.y
        engine.scanNearbyPointsOfInterest()
        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())

        assertTrue(restored.isPoiDiscovered(poi.id))
        assertEquals(setOf(poi.id), restored.discoveredPointsOfInterest())
    }
    @Test
    fun communicationNodeSignalSequenceRequiresConsoleAndLogAndPersistsOnce() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.COMMUNICATION_NODE)
        engine.storyProgress.recordEvent(StoryEventIds.COMMUNICATION_NODE_ENTERED)
        val console = PointOfInterestCatalog.byId("communication_node_console")!!
        engine.player.x = console.x
        engine.player.y = console.y

        assertFalse(engine.extractCommunicationNodeSignalSequence())
        assertTrue(engine.scanNearbyPointsOfInterest().any { it.id == console.id })
        assertFalse(engine.extractCommunicationNodeSignalSequence())
        assertTrue(engine.inspectCommunicationNodeConsole())
        assertFalse(engine.extractCommunicationNodeSignalSequence())
        assertTrue(engine.readCommunicationNodeShiftLog())
        assertTrue(engine.extractCommunicationNodeSignalSequence())
        assertFalse(engine.extractCommunicationNodeSignalSequence())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED))
        assertTrue(restored.storyProgress.observations().any {
            it.eventId == StoryEventIds.COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED
        })
    }

    @Test
    fun communicationNodeSignalCrossCheckRequiresBothRecordsAndPersistsOnce() {
        val engine = GameEngine()
        assertFalse(engine.crossCheckCommunicationNodeSignal())
        engine.storyProgress.recordEvent(StoryEventIds.COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED)
        assertFalse(engine.crossCheckCommunicationNodeSignal())
        engine.storyProgress.recordEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)
        assertTrue(engine.crossCheckCommunicationNodeSignal())
        assertFalse(engine.crossCheckCommunicationNodeSignal())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED))
        assertTrue(restored.storyProgress.observations().any {
            it.eventId == StoryEventIds.COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED
        })
    }

    @Test
    fun communicationNodeRouteRecordRequiresSignalCrossCheckAndPersistsOnce() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.COMMUNICATION_NODE)
        assertFalse(engine.inspectCommunicationNodeRouteRecord())
        engine.storyProgress.recordEvent(StoryEventIds.COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED)
        assertTrue(engine.inspectCommunicationNodeRouteRecord())
        assertFalse(engine.inspectCommunicationNodeRouteRecord())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND))
        assertTrue(restored.storyProgress.observations().any {
            it.eventId == StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND
        })
    }

    @Test
    fun line08MarkerIsHiddenUntilRouteRecordAndInspectionPersists() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.ROAD, regenerate = false)
        val marker = PointOfInterestCatalog.byId("road_line_08_marker")!!
        engine.player.x = marker.x
        engine.player.y = marker.y

        assertTrue(engine.scanNearbyPointsOfInterest().none { it.id == marker.id })
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_MARKER_FOUND))
        assertFalse(engine.inspectRoadLine08Marker())

        engine.storyProgress.recordEvent(StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND)
        assertTrue(engine.scanNearbyPointsOfInterest().any { it.id == marker.id })
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_MARKER_FOUND))
        assertTrue(engine.inspectRoadLine08Marker())
        assertFalse(engine.inspectRoadLine08Marker())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_MARKER_FOUND))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_MARKER_INSPECTED))
        assertTrue(restored.storyProgress.observations().any {
            it.eventId == StoryEventIds.ROAD_LINE_08_MARKER_INSPECTED
        })
    }

    @Test
    fun line08BranchRequiresInspectedMarkerAndPersistsOnce() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.ROAD, regenerate = false)
        val marker = PointOfInterestCatalog.byId("road_line_08_marker")!!
        val branch = PointOfInterestCatalog.byId("road_line_08_branch")!!
        engine.player.x = branch.x
        engine.player.y = branch.y

        engine.storyProgress.recordEvent(StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND)
        assertTrue(engine.scanNearbyPointsOfInterest().any { it.id == marker.id })
        assertTrue(engine.scanNearbyPointsOfInterest().none { it.id == branch.id })
        assertFalse(engine.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_BRANCH_FOUND))
        assertFalse(engine.inspectRoadLine08Branch())

        engine.storyProgress.recordEvent(StoryEventIds.ROAD_LINE_08_MARKER_FOUND)
        engine.storyProgress.recordEvent(StoryEventIds.ROAD_LINE_08_MARKER_INSPECTED)
        assertTrue(engine.scanNearbyPointsOfInterest().any { it.id == branch.id })
        assertTrue(engine.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_BRANCH_FOUND))
        assertTrue(engine.inspectRoadLine08Branch())
        assertFalse(engine.inspectRoadLine08Branch())

        val restored = GameEngine()
        restored.restoreSaveSnapshot(engine.createSaveSnapshot())
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_BRANCH_FOUND))
        assertTrue(restored.storyProgress.hasEvent(StoryEventIds.ROAD_LINE_08_BRANCH_INSPECTED))
        assertTrue(restored.storyProgress.observations().any {
            it.eventId == StoryEventIds.ROAD_LINE_08_BRANCH_INSPECTED
        })
    }

}