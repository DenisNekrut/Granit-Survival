package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerLayoutTest {
    @Test
    fun bunkerUsesCompactFootprintWhileExpeditionZonesKeepTheirSize() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        val forest = createZoneMap(WorldZone.FOREST)

        assertEquals(34, bunker.width)
        assertEquals(20, bunker.height)
        assertEquals(40, forest.width)
        assertEquals(24, forest.height)
    }

    @Test
    fun allBunkerStationsAndExitFitInsideCompactMap() {
        val bunker = createZoneMap(WorldZone.BUNKER)
        val required = listOf(
            TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF,
            TileType.RADIO, TileType.DINING_TABLE, TileType.GENERATOR,
            TileType.MEDICAL_STATION, TileType.EXIT_TO_FOREST
        )
        required.forEach { target ->
            var found = false
            bunker.forEachTile { x, y, type ->
                if (type == target) {
                    found = true
                    assertTrue(x in 1 until bunker.width - 1)
                    assertTrue(y in 1 until bunker.height - 1)
                }
            }
            assertTrue("$target must remain available", found)
        }
        assertNotNull(bunker.spawnBesideExit(TileType.EXIT_TO_FOREST))
    }

    @Test
    fun gameEngineUsesCompactBunkerBoundsAfterZoneTransitions() {
        val engine = GameEngine()
        assertEquals(34, engine.tileMap.width)
        assertEquals(20, engine.tileMap.height)

        engine.changeZone(WorldZone.FOREST)
        assertEquals(40, engine.tileMap.width)
        assertEquals(24, engine.tileMap.height)

        engine.changeZone(WorldZone.BUNKER, entryFrom = WorldZone.FOREST)
        assertEquals(34, engine.tileMap.width)
        assertEquals(20, engine.tileMap.height)
        assertTrue(engine.tileMap.isWalkable(engine.player.x, engine.player.y))
    }
}
