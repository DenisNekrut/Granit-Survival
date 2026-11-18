package com.granit.survival

enum class TileType {
    FLOOR,
    WALL,
    BED,
    WORKBENCH,
    STORAGE,
    EXIT,
    RESOURCE_WOOD,
    RESOURCE_METAL,
    RESOURCE_FABRIC,
    RESOURCE_FOOD,
    RESOURCE_WATER
}

data class ResourceLocation(val x: Int, val y: Int, val type: ItemType)

/** Grid world shared by rendering, collision and interaction systems. */
class TileMap(
    val width: Int = 40,
    val height: Int = 24
) {
    private val tiles = Array(height) { Array(width) { TileType.FLOOR } }

    init {
        for (x in 0 until width) {
            set(x, 0, TileType.WALL)
            set(x, height - 1, TileType.WALL)
        }
        for (y in 0 until height) {
            set(0, y, TileType.WALL)
            set(width - 1, y, TileType.WALL)
        }

        // Bunker furniture and equipment.
        fill(2, 2, 5, 1, TileType.BED)
        fill(2, 4, 3, 1, TileType.STORAGE)
        fill(16, 2, 2, 2, TileType.STORAGE)
        fill(9, 1, 2, 1, TileType.WORKBENCH)
        set(width / 2, 0, TileType.EXIT)

        // Test supplies placed within walking distance of the starting area.
        set(7, 8, TileType.RESOURCE_WOOD)
        set(12, 8, TileType.RESOURCE_METAL)
        set(10, 11, TileType.RESOURCE_FABRIC)
        set(14, 10, TileType.RESOURCE_FOOD)
        set(5, 10, TileType.RESOURCE_WATER)
    }

    operator fun get(x: Int, y: Int): TileType {
        if (x !in 0 until width || y !in 0 until height) return TileType.WALL
        return tiles[y][x]
    }

    fun isWalkable(worldX: Float, worldY: Float, radius: Float = 0.22f): Boolean {
        val points = arrayOf(
            worldX - radius to worldY - radius,
            worldX + radius to worldY - radius,
            worldX - radius to worldY + radius,
            worldX + radius to worldY + radius
        )
        return points.all { (x, y) ->
            when (get(kotlin.math.floor(x).toInt(), kotlin.math.floor(y).toInt())) {
                TileType.WALL, TileType.BED, TileType.WORKBENCH, TileType.STORAGE -> false
                else -> true
            }
        }
    }

    fun nearestResource(worldX: Float, worldY: Float, maxDistance: Float): ResourceLocation? {
        var nearest: ResourceLocation? = null
        var nearestDistanceSquared = maxDistance * maxDistance
        forEachTile { x, y, type ->
            val itemType = when (type) {
                TileType.RESOURCE_WOOD -> ItemType.WOOD
                TileType.RESOURCE_METAL -> ItemType.METAL
                TileType.RESOURCE_FABRIC -> ItemType.FABRIC
                TileType.RESOURCE_FOOD -> ItemType.CANNED_FOOD
                TileType.RESOURCE_WATER -> ItemType.WATER
                else -> null
            } ?: return@forEachTile

            val dx = x + 0.5f - worldX
            val dy = y + 0.5f - worldY
            val distanceSquared = dx * dx + dy * dy
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared
                nearest = ResourceLocation(x, y, itemType)
            }
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
        }
        if (get(location.x, location.y) != expected) return false
        set(location.x, location.y, TileType.FLOOR)
        return true
    }

    fun nearestInteractable(worldX: Float, worldY: Float, maxDistance: Float): TileType? {
        var nearest: TileType? = null
        var nearestDistanceSquared = maxDistance * maxDistance
        forEachTile { x, y, type ->
            if (type == TileType.BED || type == TileType.WORKBENCH || type == TileType.STORAGE || type == TileType.EXIT) {
                val dx = x + 0.5f - worldX
                val dy = y + 0.5f - worldY
                val distanceSquared = dx * dx + dy * dy
                if (distanceSquared < nearestDistanceSquared) {
                    nearestDistanceSquared = distanceSquared
                    nearest = type
                }
            }
        }
        return nearest
    }

    fun forEachTile(action: (x: Int, y: Int, type: TileType) -> Unit) {
        for (y in 0 until height) {
            for (x in 0 until width) action(x, y, tiles[y][x])
        }
    }

    private fun set(x: Int, y: Int, type: TileType) {
        if (x in 0 until width && y in 0 until height) tiles[y][x] = type
    }

    private fun fill(x: Int, y: Int, w: Int, h: Int, type: TileType) {
        for (tileY in y until y + h) {
            for (tileX in x until x + w) set(tileX, tileY, type)
        }
    }
}
