package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldZoneTest {
    @Test
    fun playerCanTravelFromBunkerToForestAndBack() {
        val engine = GameEngine()
        engine.player.x = 20.5f
        engine.player.y = 1.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.FOREST, engine.zone)

        engine.player.x = 20.5f
        engine.player.y = 22.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.BUNKER, engine.zone)
    }

    @Test
    fun forestConnectsToRuinsAndRoadAndBothReturnToForest() {
        val engine = GameEngine()
        engine.changeZone(WorldZone.FOREST)

        engine.player.x = 1.5f
        engine.player.y = 12.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.RUINS, engine.zone)

        engine.player.x = 38.5f
        engine.player.y = 12.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.FOREST, engine.zone)

        engine.player.x = 38.5f
        engine.player.y = 12.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.ROAD, engine.zone)

        engine.player.x = 1.5f
        engine.player.y = 12.5f
        engine.updateFrame(1f / 60f, EngineInput(interact = false))
        engine.updateFrame(1f / 60f, EngineInput(interact = true))
        assertEquals(WorldZone.FOREST, engine.zone)
    }


    @Test
    fun everyZoneSpawnCanReachAllItsExitsWithoutCrossingWalls() {
        val exits = mapOf(
            WorldZone.BUNKER to listOf(20 to 1),
            WorldZone.FOREST to listOf(20 to 22, 1 to 12, 38 to 12),
            WorldZone.RUINS to listOf(38 to 12),
            WorldZone.ROAD to listOf(1 to 12)
        )
        WorldZone.values().forEach { zone ->
            val map = TileMap(zone = zone)
            val start = kotlin.math.floor(zone.spawnX).toInt() to kotlin.math.floor(zone.spawnY).toInt()
            val visited = mutableSetOf(start)
            val queue = ArrayDeque<Pair<Int, Int>>()
            queue.add(start)
            while (queue.isNotEmpty()) {
                val (x, y) = queue.removeFirst()
                listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).forEach { next ->
                    if (next !in visited && map.isWalkable(next.first + 0.5f, next.second + 0.5f)) {
                        visited.add(next)
                        queue.add(next)
                    }
                }
            }
            exits.getValue(zone).forEach { (x, y) ->
                assertTrue("$zone exit ($x,$y) must be reachable from spawn", (x to y) in visited)
            }
        }
    }

    @Test
    fun checkpointRestoresZoneAndResources() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        engine.changeZone(WorldZone.FOREST)
        engine.tileMap.collectResource(ResourceLocation(7, 5, ItemType.WOOD))
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(WorldZone.BUNKER, engine.zone)
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[7, 8])
        engine.changeZone(WorldZone.FOREST)
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[7, 5])
    }

    @Test
    fun saveRestoreKeepsCollectedResourcesInEveryZone() {
        val original = GameEngine()
        original.changeZone(WorldZone.FOREST)
        original.tileMap.collectResource(ResourceLocation(7, 5, ItemType.WOOD))
        val snapshot = original.createSaveSnapshot()

        val restored = GameEngine()
        restored.restoreSaveSnapshot(snapshot)
        assertEquals(WorldZone.FOREST, restored.zone)
        assertEquals(TileType.FLOOR, restored.tileMap[7, 5])
        restored.changeZone(WorldZone.BUNKER)
        restored.changeZone(WorldZone.FOREST)
        assertEquals(TileType.FLOOR, restored.tileMap[7, 5])
    }

    @Test
    fun saveSnapshotIncludesCurrentAndWakeZones() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        engine.changeZone(WorldZone.FOREST)
        val snapshot = engine.createSaveSnapshot()
        assertEquals(WorldZone.FOREST, snapshot.zone)
        assertEquals(WorldZone.BUNKER, snapshot.wakeCheckpoint?.zone)
        assertTrue(snapshot.zoneResources.isNotEmpty())
        assertTrue(snapshot.wakeCheckpoint?.zoneResources?.isNotEmpty() == true)
    }
}
