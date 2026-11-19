package com.granit.survival

import kotlin.math.abs
import kotlin.random.Random

enum class TileType {
    FLOOR, WALL, BED, WORKBENCH, STORAGE, BOOKSHELF, RADIO, EXIT,
    EXIT_TO_BUNKER, EXIT_TO_FOREST, EXIT_TO_RUINS, EXIT_TO_ROAD,
    RESOURCE_WOOD, RESOURCE_METAL, RESOURCE_FABRIC, RESOURCE_FOOD, RESOURCE_WATER
}

data class ResourceLocation(val x: Int, val y: Int, val type: ItemType)

class TileMap(
    val width: Int = 40,
    val height: Int = 24,
    val zone: WorldZone = WorldZone.BUNKER,
    val seed: Long? = null
) {
    val layoutSeed: Long? = seed
    private val tiles = Array(height) { Array(width) { TileType.FLOOR } }

    init {
        for (x in 0 until width) { set(x, 0, TileType.WALL); set(x, height - 1, TileType.WALL) }
        for (y in 0 until height) { set(0, y, TileType.WALL); set(width - 1, y, TileType.WALL) }
        when (zone) {
            WorldZone.BUNKER -> buildBunker()
            WorldZone.FOREST -> buildForest()
            WorldZone.RUINS -> buildRuins()
            WorldZone.ROAD -> buildRoad()
        }
        if (seed != null && zone.isProcedurallyGenerated) buildProceduralLayout(seed)
    }

    private fun buildBunker() {
        fill(2, 2, 5, 1, TileType.BED)
        fill(2, 4, 3, 1, TileType.STORAGE)
        fill(16, 2, 2, 2, TileType.STORAGE)
        fill(9, 1, 2, 1, TileType.WORKBENCH)
        fill(20, 2, 1, 2, TileType.BOOKSHELF)
        set(24, 2, TileType.RADIO)
        set(width / 2, 1, TileType.EXIT_TO_FOREST)
        set(7, 8, TileType.RESOURCE_WOOD); set(12, 8, TileType.RESOURCE_METAL)
        set(10, 11, TileType.RESOURCE_FABRIC); set(14, 10, TileType.RESOURCE_FOOD)
        set(5, 10, TileType.RESOURCE_WATER)
    }

    private fun buildForest() {
        set(width / 2, height - 2, TileType.EXIT_TO_BUNKER)
        set(1, height / 2, TileType.EXIT_TO_RUINS)
        set(width - 2, height / 2, TileType.EXIT_TO_ROAD)
        fill(4, 3, 2, 3, TileType.WALL); fill(9, 7, 2, 4, TileType.WALL)
        fill(15, 2, 3, 2, TileType.WALL); fill(25, 5, 2, 4, TileType.WALL)
        fill(31, 14, 3, 3, TileType.WALL)
        set(7, 5, TileType.RESOURCE_WOOD); set(12, 4, TileType.RESOURCE_WOOD)
        set(19, 6, TileType.RESOURCE_WOOD); set(23, 15, TileType.RESOURCE_WOOD)
        set(29, 10, TileType.RESOURCE_FABRIC); set(14, 17, TileType.RESOURCE_WATER)
        set(34, 6, TileType.RESOURCE_FOOD); set(6, 17, TileType.RESOURCE_METAL)
    }

    private fun buildRuins() {
        set(width - 2, height / 2, TileType.EXIT_TO_FOREST)
        fill(7, 3, 5, 1, TileType.WALL); fill(7, 3, 1, 7, TileType.WALL)
        fill(17, 8, 7, 2, TileType.WALL); fill(28, 3, 2, 6, TileType.WALL)
        fill(11, 16, 4, 2, TileType.WALL)
        set(5, 5, TileType.RESOURCE_METAL); set(14, 5, TileType.RESOURCE_METAL)
        set(25, 13, TileType.RESOURCE_METAL); set(33, 17, TileType.RESOURCE_FABRIC)
        set(20, 4, TileType.RESOURCE_FOOD); set(5, 19, TileType.RESOURCE_WATER)
        set(31, 11, TileType.RESOURCE_WOOD)
    }

    private fun buildRoad() {
        set(1, height / 2, TileType.EXIT_TO_FOREST)
        fill(8, 5, 1, 5, TileType.WALL); fill(18, 13, 5, 1, TileType.WALL)
        fill(29, 3, 2, 5, TileType.WALL); fill(33, 17, 3, 1, TileType.WALL)
        set(6, 7, TileType.RESOURCE_METAL); set(13, 17, TileType.RESOURCE_METAL)
        set(24, 5, TileType.RESOURCE_FABRIC); set(27, 16, TileType.RESOURCE_FOOD)
        set(34, 11, TileType.RESOURCE_WATER); set(15, 4, TileType.RESOURCE_WOOD)
    }

    /**
     * Builds a deterministic expedition layout from a seed. A clear central corridor
     * always connects the spawn to the exit; obstacles and resources vary around it.
     */
    private fun buildProceduralLayout(seed: Long) {
        for (y in 1 until height - 1) for (x in 1 until width - 1) set(x, y, TileType.FLOOR)

        val exitX = if (zone == WorldZone.RUINS) width - 2 else 1
        set(exitX, height / 2, TileType.EXIT_TO_FOREST)

        val random = Random(seed)
        val spawnX = zone.spawnX.toInt()
        val spawnY = zone.spawnY.toInt()
        val reserved = height / 2 - 1..height / 2 + 1

        fun validPlacement(x: Int, y: Int): Boolean {
            if (x <= 1 || x >= width - 2 || y <= 1 || y >= height - 2) return false
            if (y in reserved) return false
            if (abs(x - spawnX) <= 3 && abs(y - spawnY) <= 3) return false
            if (x == exitX && abs(y - height / 2) <= 2) return false
            return get(x, y) == TileType.FLOOR
        }

        // Scattered cover and debris, deliberately kept away from the guaranteed route.
        repeat(18) {
            val x = random.nextInt(2, width - 2)
            val y = random.nextInt(2, height - 2)
            if (validPlacement(x, y)) set(x, y, TileType.WALL)
        }

        val resourceTiles = listOf(
            TileType.RESOURCE_WOOD,
            TileType.RESOURCE_METAL,
            TileType.RESOURCE_FABRIC,
            TileType.RESOURCE_FOOD,
            TileType.RESOURCE_WATER
        )
        var placed = 0
        var attempts = 0
        while (placed < 18 && attempts < 240) {
            attempts++
            val x = random.nextInt(2, width - 2)
            val y = random.nextInt(2, height - 2)
            if (validPlacement(x, y)) {
                set(x, y, resourceTiles[random.nextInt(resourceTiles.size)])
                placed++
            }
        }
    }

    operator fun get(x: Int, y: Int): TileType =
        if (x !in 0 until width || y !in 0 until height) TileType.WALL else tiles[y][x]

    fun isWalkable(worldX: Float, worldY: Float, radius: Float = 0.22f): Boolean {
        val points = arrayOf(worldX - radius to worldY - radius, worldX + radius to worldY - radius,
            worldX - radius to worldY + radius, worldX + radius to worldY + radius)
        return points.all { (x, y) ->
            when (get(kotlin.math.floor(x).toInt(), kotlin.math.floor(y).toInt())) {
                TileType.WALL, TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF, TileType.RADIO -> false
                else -> true
            }
        }
    }

    fun nearestResource(worldX: Float, worldY: Float, maxDistance: Float): ResourceLocation? {
        var nearest: ResourceLocation? = null
        var nearestDistanceSquared = maxDistance * maxDistance
        forEachTile { x, y, type ->
            val item = when (type) {
                TileType.RESOURCE_WOOD -> ItemType.WOOD
                TileType.RESOURCE_METAL -> ItemType.METAL
                TileType.RESOURCE_FABRIC -> ItemType.FABRIC
                TileType.RESOURCE_FOOD -> ItemType.CANNED_FOOD
                TileType.RESOURCE_WATER -> ItemType.WATER
                else -> null
            } ?: return@forEachTile
            val dx = x + 0.5f - worldX; val dy = y + 0.5f - worldY
            val distance = dx * dx + dy * dy
            if (distance < nearestDistanceSquared) { nearestDistanceSquared = distance; nearest = ResourceLocation(x, y, item) }
        }
        return nearest
    }

    fun collectResource(location: ResourceLocation): Boolean {
        val expected = when (location.type) {
            ItemType.WOOD -> TileType.RESOURCE_WOOD
            ItemType.METAL -> TileType.RESOURCE_METAL
            ItemType.FABRIC -> TileType.RESOURCE_FABRIC
            ItemType.CANNED_FOOD -> TileType.RESOURCE_FOOD
            ItemType.WATER -> TileType.RESOURCE_WATER
            else -> return false
        }
        if (get(location.x, location.y) != expected) return false
        set(location.x, location.y, TileType.FLOOR)
        return true
    }

    fun isNearTile(target: TileType, worldX: Float, worldY: Float, maxDistance: Float): Boolean {
        val limit = maxDistance * maxDistance
        var found = false
        forEachTile { x, y, type ->
            if (type == target) {
                val dx = x + 0.5f - worldX; val dy = y + 0.5f - worldY
                if (dx * dx + dy * dy <= limit) found = true
            }
        }
        return found
    }

    fun nearestInteractable(worldX: Float, worldY: Float, maxDistance: Float): TileType? {
        var nearest: TileType? = null
        var best = maxDistance * maxDistance
        forEachTile { x, y, type ->
            val valid = when (type) {
                TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF, TileType.RADIO, TileType.EXIT,
                TileType.EXIT_TO_BUNKER, TileType.EXIT_TO_FOREST, TileType.EXIT_TO_RUINS,
                TileType.EXIT_TO_ROAD -> true
                else -> false
            }
            if (valid) {
                val dx = x + 0.5f - worldX; val dy = y + 0.5f - worldY
                val distance = dx * dx + dy * dy
                if (distance < best) { best = distance; nearest = type }
            }
        }
        return nearest
    }

    fun resourceSnapshot(): List<ResourceLocation> {
        val result = mutableListOf<ResourceLocation>()
        forEachTile { x, y, type ->
            val item = when (type) {
                TileType.RESOURCE_WOOD -> ItemType.WOOD
                TileType.RESOURCE_METAL -> ItemType.METAL
                TileType.RESOURCE_FABRIC -> ItemType.FABRIC
                TileType.RESOURCE_FOOD -> ItemType.CANNED_FOOD
                TileType.RESOURCE_WATER -> ItemType.WATER
                else -> null
            }
            if (item != null) result += ResourceLocation(x, y, item)
        }
        return result
    }

    fun restoreResources(resources: List<ResourceLocation>) {
        forEachTile { x, y, type ->
            if (type in setOf(TileType.RESOURCE_WOOD, TileType.RESOURCE_METAL, TileType.RESOURCE_FABRIC,
                    TileType.RESOURCE_FOOD, TileType.RESOURCE_WATER)) set(x, y, TileType.FLOOR)
        }
        resources.forEach { resource ->
            val type = when (resource.type) {
                ItemType.WOOD -> TileType.RESOURCE_WOOD
                ItemType.METAL -> TileType.RESOURCE_METAL
                ItemType.FABRIC -> TileType.RESOURCE_FABRIC
                ItemType.CANNED_FOOD -> TileType.RESOURCE_FOOD
                ItemType.WATER -> TileType.RESOURCE_WATER
                else -> null
            }
            if (type != null) set(resource.x, resource.y, type)
        }
    }

    fun forEachTile(action: (x: Int, y: Int, type: TileType) -> Unit) {
        for (y in 0 until height) for (x in 0 until width) action(x, y, tiles[y][x])
    }

    private fun set(x: Int, y: Int, type: TileType) {
        if (x in 0 until width && y in 0 until height) tiles[y][x] = type
    }

    private fun fill(x: Int, y: Int, w: Int, h: Int, type: TileType) {
        for (tileY in y until y + h) for (tileX in x until x + w) set(tileX, tileY, type)
    }
}
