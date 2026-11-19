package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldZoneTest {
    @Test
    fun bunkerUsesCompactDimensionsAndCenteredExit() {
        val engine = GameEngine()
        assertEquals(17, engine.tileMap.width)
        assertEquals(10, engine.tileMap.height)
        assertEquals(TileType.EXIT_TO_FOREST, engine.tileMap[8, 1])
        assertEquals(8.5f, engine.tileMap.spawnBesideExit(TileType.EXIT_TO_FOREST)!!.first, 0.01f)
    }

    @Test
    fun bunkerHasSixRoomsWithReachableEquipmentStations() {
        val engine = GameEngine()
        val types = mutableSetOf<TileType>()
        engine.tileMap.forEachTile { _, _, type -> types += type }

        assertTrue(TileType.BED in types)
        assertTrue(TileType.DINING_TABLE in types)
        assertTrue(TileType.GENERATOR in types)
        assertTrue(TileType.MEDICAL_STATION in types)
        assertTrue(TileType.WORKBENCH in types)

        engine.player.x = 7.5f
        engine.player.y = 2.5f
        assertTrue(engine.canAccessDiningTable())

        engine.player.x = 13.5f
        engine.player.y = 2.5f
        assertTrue(engine.canAccessGenerator())

        engine.player.x = 2.5f
        engine.player.y = 7.5f
        assertTrue(engine.canAccessMedicalStation())

        engine.player.x = 8.5f
        engine.player.y = 7.5f
        assertTrue(engine.canUseWorkbench())
    }

    @Test
    fun everyZoneTransitionPlacesPlayerBesideTheMatchingDestinationHatch() {
        val engine = GameEngine()

        fun interactAt(x: Float, y: Float) {
            engine.updateFrame(1f / 60f, EngineInput(interact = false))
            engine.player.x = x
            engine.player.y = y
            engine.updateFrame(1f / 60f, EngineInput(interact = true))
        }

        interactAt(8.5f, 2.5f)
        assertEquals(WorldZone.FOREST, engine.zone)
        assertEquals(20.5f, engine.player.x, 0.01f)
        assertEquals(21.5f, engine.player.y, 0.01f)

        interactAt(20.5f, 22.5f)
        assertEquals(WorldZone.BUNKER, engine.zone)
        assertEquals(8.5f, engine.player.x, 0.01f)
        assertEquals(2.5f, engine.player.y, 0.01f)

        engine.changeZone(WorldZone.FOREST)
        interactAt(1.5f, 12.5f)
        assertEquals(WorldZone.RUINS, engine.zone)
        assertEquals(37.5f, engine.player.x, 0.01f)
        assertEquals(12.5f, engine.player.y, 0.01f)

        interactAt(38.5f, 12.5f)
        assertEquals(WorldZone.FOREST, engine.zone)
        assertEquals(2.5f, engine.player.x, 0.01f)
        assertEquals(12.5f, engine.player.y, 0.01f)

        interactAt(38.5f, 12.5f)
        assertEquals(WorldZone.ROAD, engine.zone)
        assertEquals(2.5f, engine.player.x, 0.01f)
        assertEquals(12.5f, engine.player.y, 0.01f)

        interactAt(1.5f, 12.5f)
        assertEquals(WorldZone.FOREST, engine.zone)
        assertEquals(37.5f, engine.player.x, 0.01f)
        assertEquals(12.5f, engine.player.y, 0.01f)
    }

    @Test
    fun playerCanTravelFromBunkerToForestAndBack() {
        val engine = GameEngine()
        engine.player.x = 8.5f
        engine.player.y = 2.5f
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
            WorldZone.BUNKER to listOf(8 to 1),
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
    fun outdoorZonesHaveConnectedRoomPartitionsAndClearDoorways() {
        listOf(WorldZone.FOREST, WorldZone.RUINS, WorldZone.ROAD).forEach { zone ->
            val map = TileMap(zone = zone)
            assertEquals(TileType.WALL, map[13, 3])
            assertEquals(TileType.FLOOR, map[13, 12])
            assertEquals(TileType.WALL, map[26, 18])
            assertEquals(TileType.FLOOR, map[20, 7])
        }
    }

    @Test
    fun fixedBunkerAndFirstForestMapIgnoreProceduralSeeds() {
        listOf(WorldZone.BUNKER, WorldZone.FOREST).forEach { zone ->
            val first = TileMap(zone = zone, seed = 111L)
            val second = TileMap(zone = zone, seed = 999L)
            assertEquals(first.resourceSnapshot(), second.resourceSnapshot())
            assertEquals(null, first.layoutSeed)
            assertEquals(null, second.layoutSeed)
        }
    }

    @Test
    fun proceduralMapsAreDeterministicForASeedAndDifferentAcrossSeeds() {
        listOf(WorldZone.RUINS, WorldZone.ROAD).forEach { zone ->
            val first = TileMap(zone = zone, seed = 12345L)
            val same = TileMap(zone = zone, seed = 12345L)
            val different = TileMap(zone = zone, seed = 54321L)
            assertEquals(first.resourceSnapshot(), same.resourceSnapshot())
            assertEquals(first.layoutSeed, same.layoutSeed)
            assertTrue(first.resourceSnapshot() != different.resourceSnapshot())
            assertTrue(first.isWalkable(zone.spawnX, zone.spawnY))
        }
    }

    @Test
    fun proceduralMapSpawnAndExitRemainConnectedForManySeeds() {
        listOf(WorldZone.RUINS, WorldZone.ROAD).forEach { zone ->
            (1L..25L).forEach { seed ->
                val map = TileMap(zone = zone, seed = seed)
                val start = zone.spawnX.toInt() to zone.spawnY.toInt()
                val exit = (if (zone == WorldZone.RUINS) map.width - 2 else 1) to map.height / 2
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
                assertTrue("$zone seed=$seed must preserve route to exit", exit in visited)
            }
        }
    }

    @Test
    fun weaponIsDrawnAutomaticallyOutsideAndHolsteredInBunker() {
        val engine = GameEngine()
        assertEquals(false, engine.equipment.weaponDrawn)
        assertEquals(WeaponType.PULSE_CARBINE, engine.equipment.weapon)

        engine.changeZone(WorldZone.FOREST)
        assertEquals(true, engine.equipment.weaponDrawn)
        engine.changeZone(WorldZone.RUINS)
        assertEquals(true, engine.equipment.weaponDrawn)
        engine.changeZone(WorldZone.BUNKER)
        assertEquals(false, engine.equipment.weaponDrawn)
        engine.changeZone(WorldZone.ROAD)
        assertEquals(true, engine.equipment.weaponDrawn)
    }

    @Test
    fun proceduralZoneRegeneratesOnEveryEntryButForestRemainsFixed() {
        val seeds = listOf(101L, 202L)
        var nextSeed = 0
        val engine = GameEngine { seeds[nextSeed++] }

        engine.changeZone(WorldZone.FOREST)
        val firstForestMap = engine.tileMap
        engine.changeZone(WorldZone.RUINS)
        assertEquals(101L, engine.tileMap.layoutSeed)
        engine.changeZone(WorldZone.FOREST)
        assertTrue(engine.tileMap === firstForestMap)
        engine.changeZone(WorldZone.RUINS)
        assertEquals(202L, engine.tileMap.layoutSeed)
    }

    @Test
    fun saveRestoreReconstructsCurrentProceduralMapFromItsSeed() {
        val original = GameEngine { 501L }
        original.changeZone(WorldZone.RUINS)
        val savedSeed = original.tileMap.layoutSeed
        val savedResources = original.tileMap.resourceSnapshot()
        val snapshot = original.createSaveSnapshot()

        val restored = GameEngine { 777L }
        restored.restoreSaveSnapshot(snapshot)
        assertEquals(savedSeed, restored.tileMap.layoutSeed)
        assertEquals(savedResources, restored.tileMap.resourceSnapshot())

        restored.changeZone(WorldZone.FOREST)
        restored.changeZone(WorldZone.RUINS)
        assertEquals(777L, restored.tileMap.layoutSeed)
    }

    @Test
    fun wakeCheckpointRollbackDoesNotKeepAnExpeditionGeneratedAfterWake() {
        val seeds = listOf(301L, 302L)
        var nextSeed = 0
        val engine = GameEngine { seeds[nextSeed++] }
        engine.sleepUntilMorning()
        engine.changeZone(WorldZone.RUINS)
        assertEquals(301L, engine.tileMap.layoutSeed)

        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(WorldZone.BUNKER, engine.zone)
        engine.changeZone(WorldZone.RUINS)
        assertEquals(302L, engine.tileMap.layoutSeed)
    }

    @Test
    fun checkpointRestoresZoneAndResources() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        engine.changeZone(WorldZone.FOREST)
        engine.tileMap.collectResource(ResourceLocation(7, 5, ItemType.WOOD))
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertEquals(WorldZone.BUNKER, engine.zone)
        assertEquals(TileType.RESOURCE_WOOD, engine.tileMap[1, 8])
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
