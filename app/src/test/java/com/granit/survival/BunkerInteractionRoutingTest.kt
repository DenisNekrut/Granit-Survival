package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerInteractionRoutingTest {
    @Test
    fun everyBunkerStationResolvesToItsOwnContextAction() {
        val engine = GameEngine()
        val stations = listOf(
            Triple(TileType.BED, 2.5f, 2.5f),
            Triple(TileType.STORAGE, 13.5f, 6.5f),
            Triple(TileType.WORKBENCH, 8.5f, 7.5f),
            Triple(TileType.BOOKSHELF, 9.5f, 3.5f),
            Triple(TileType.RADIO, 14.5f, 3.5f),
            Triple(TileType.DINING_TABLE, 7.5f, 3.5f),
            Triple(TileType.GENERATOR, 13.5f, 3.5f),
            Triple(TileType.MEDICAL_STATION, 2.5f, 7.5f)
        )

        stations.forEach { (expected, x, y) ->
            engine.player.x = x
            engine.player.y = y
            assertEquals("Wrong context action at $expected", expected,
                engine.tileMap.nearestInteractable(x, y, 1.35f))
        }
    }

    @Test
    fun unpoweredWorkbenchStillOpensButCannotCraft() {
        val engine = GameEngine()
        engine.player.x = 8.5f
        engine.player.y = 7.5f
        engine.bunkerPower.toggleGenerator()

        assertTrue(engine.canAccessWorkbench())
        assertFalse(engine.canUseWorkbench())
        assertFalse(engine.craft("bandage"))
        assertEquals(TileType.WORKBENCH, engine.tileMap.nearestInteractable(engine.player.x, engine.player.y, 1.35f))
    }

    @Test
    fun eachBunkerDoorwayRemainsAnInteractableExit() {
        val engine = GameEngine()
        engine.player.x = 8.5f
        engine.player.y = 2.5f
        assertEquals(TileType.EXIT_TO_FOREST,
            engine.tileMap.nearestInteractable(engine.player.x, engine.player.y, 1.35f))
    }
}
