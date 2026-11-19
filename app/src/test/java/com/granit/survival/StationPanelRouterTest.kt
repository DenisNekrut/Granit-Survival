package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Test

class StationPanelRouterTest {
    @Test
    fun routesEachStationToItsOwnPanel() {
        val calls = mutableListOf<TileType>()
        val stations = listOf(
            TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF,
            TileType.DINING_TABLE, TileType.MEDICAL_STATION, TileType.GENERATOR, TileType.RADIO, TileType.BED
        )
        val router = StationPanelRouter(stations.associateWith { station -> { calls += station } })

        stations.forEach { router.route(it, hasNearbyResource = false) }

        assertEquals(stations, calls)
    }

    @Test
    fun resourceCollectionTakesPriorityOverStationPanels() {
        var opened = false
        val router = StationPanelRouter(mapOf(TileType.WORKBENCH to { opened = true }))

        router.route(TileType.WORKBENCH, hasNearbyResource = true)

        assertEquals(false, opened)
    }

    @Test
    fun exitsAndUnknownTargetsDoNotOpenStationPanels() {
        var opened = false
        val router = StationPanelRouter(mapOf(TileType.WORKBENCH to { opened = true }))

        router.route(TileType.EXIT_TO_FOREST, hasNearbyResource = false)
        router.route(null, hasNearbyResource = false)

        assertEquals(false, opened)
    }
}
