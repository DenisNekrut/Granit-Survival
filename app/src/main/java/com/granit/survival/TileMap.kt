package com.granit.survival

enum class TileType {
    FLOOR,
    WALL,
    BED,
    WORKBENCH,
    STORAGE,
    EXIT
}

/** Small grid world. Coordinates are measured in world tiles. */
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
                TileType.FLOOR, TileType.EXIT -> true
                else -> false
            }
        }
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
