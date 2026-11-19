package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerLayoutTest {
    @Test
    fun bunkerFootprintIsHalfThePreviousDimensionsAndOutdoorZonesKeepTheirSize() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        val forest = createZoneMap(WorldZone.FOREST)
        assertEquals(17, bunker.width)
        assertEquals(10, bunker.height)
        assertEquals(40, forest.width)
        assertEquals(24, forest.height)
    }

    @Test
    fun allSixRoomsStationsAndExitFitInsideBunker() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        val required = listOf(TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF,
            TileType.RADIO, TileType.DINING_TABLE, TileType.GENERATOR, TileType.MEDICAL_STATION,
            TileType.EXIT_TO_FOREST)
        required.forEach { target ->
            var found = false
            bunker.forEachTile { x, y, type ->
                if (type == target) {
                    found = true
                    assertTrue("$target must remain available", x in 1 until bunker.width - 1)
                    assertTrue("$target must remain available", y in 1 until bunker.height - 1)
                }
            }
            assertTrue("$target must remain available", found)
        }
        assertNotNull(bunker.spawnBesideExit(TileType.EXIT_TO_FOREST))
    }

    @Test
    fun sixRoomsAreConnectedBySingleTileCorridors() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        assertEquals(TileType.FLOOR, bunker[8, 4])
        assertEquals(TileType.FLOOR, bunker[8, 5])
        assertEquals(TileType.WALL, bunker[5, 3])
        assertEquals(TileType.WALL, bunker[11, 6])
        val centers = listOf(2.5f to 2.5f, 7.5f to 2.5f, 14.5f to 2.5f,
            2.5f to 7.5f, 9.5f to 7.5f, 14.5f to 7.5f)
        centers.forEach { (x, y) -> assertTrue(bunker.isWalkable(x, y)) }
        centers.drop(1).forEach { (x, y) -> assertNotNull(bunker.nextPathStep(2.5f, 2.5f, x, y)) }
    }

    @Test
    fun engineUsesHalfSizeBunkerAfterZoneTransitions() {
        val engine = GameEngine()
        assertEquals(17, engine.tileMap.width)
        assertEquals(10, engine.tileMap.height)
        engine.changeZone(WorldZone.FOREST)
        assertEquals(40, engine.tileMap.width)
        assertEquals(24, engine.tileMap.height)
        engine.changeZone(WorldZone.BUNKER, entryFrom = WorldZone.FOREST)
        assertEquals(17, engine.tileMap.width)
        assertEquals(10, engine.tileMap.height)
        assertTrue(engine.tileMap.isWalkable(engine.player.x, engine.player.y))
    }

    @Test
    fun idleCharacterStartsRoomToRoomWanderingAfterFifteenSeconds() {
        val engine = GameEngine()
        val startX = engine.player.x
        val startY = engine.player.y
        repeat(61) { engine.updateFrame(0.25f, EngineInput()) }
        assertTrue(kotlin.math.abs(engine.player.x - startX) + kotlin.math.abs(engine.player.y - startY) > 0.01f)
        val before = engine.player.x to engine.player.y
        repeat(4) { engine.updateFrame(0.25f, EngineInput(moveX = 1f)) }
        assertTrue(kotlin.math.abs(engine.player.x - before.first) + kotlin.math.abs(engine.player.y - before.second) > 0.01f)
    }
}
