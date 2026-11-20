package com.granit.survival

import kotlin.math.abs
import kotlin.random.Random

enum class TileType {
    FLOOR, WALL, BED, WORKBENCH, STORAGE, BOOKSHELF, RADIO, EXIT,
    EXIT_TO_BUNKER, EXIT_TO_FOREST, EXIT_TO_RUINS, EXIT_TO_ROAD,
    DINING_TABLE, GENERATOR, MEDICAL_STATION,
    RESOURCE_WOOD, RESOURCE_METAL, RESOURCE_FABRIC, RESOURCE_FOOD, RESOURCE_WATER
}

data class ResourceLocation(val x: Int, val y: Int, val type: ItemType)

class TileMap(
    val width: Int = 40,
    val height: Int = 24,
    val zone: WorldZone = WorldZone.BUNKER,
    val seed: Long? = null
) {
    val layoutSeed: Long? = if (zone.isProcedurallyGenerated) seed else null
    private val tiles = Array(height) { Array(width) { TileType.FLOOR } }
    private val zoneLandmarks = PointOfInterestCatalog.inZone(zone)
    private val protectedExitCoordinates: List<Pair<Int, Int>> = when (zone) {
        WorldZone.BUNKER -> listOf(width / 2 to 1)
        WorldZone.FOREST -> listOf(width / 2 to height - 2, 1 to height / 2, width - 2 to height / 2)
        WorldZone.RUINS -> listOf(width - 2 to height / 2)
        WorldZone.ROAD -> listOf(1 to height / 2)
    }

    init {
        for (x in 0 until width) { set(x, 0, TileType.WALL); set(x, height - 1, TileType.WALL) }
        for (y in 0 until height) { set(0, y, TileType.WALL); set(width - 1, y, TileType.WALL) }
        when (zone) {
            WorldZone.BUNKER -> buildBunker()
            WorldZone.FOREST -> buildForest()
            WorldZone.RUINS -> buildRuins()
            WorldZone.ROAD -> buildRoad()
        }
        if (zone != WorldZone.BUNKER) buildRoomPartitions()
        if (seed != null && zone.isProcedurallyGenerated) buildProceduralLayout(seed)
    }

    private fun buildBunker() {
        // Six compact rooms (three by two) connected through one-tile doorways.
        for (y in 1 until height - 1) {
            if (y != 2 && y != 7) {
                set(5, y, TileType.WALL)
                set(11, y, TileType.WALL)
            }
        }
        for (x in 1 until width - 1) {
            if (x != 2 && x != 8 && x != 14) set(x, 4, TileType.WALL)
        }

        // Furniture is anchored to room perimeter walls; keep the bedroom exit lane open.
        set(2, 1, TileType.BED)
        set(7, 3, TileType.DINING_TABLE)
        set(9, 3, TileType.BOOKSHELF)
        set(13, 3, TileType.GENERATOR)
        set(14, 3, TileType.RADIO)
        set(2, 8, TileType.MEDICAL_STATION)
        set(8, 8, TileType.WORKBENCH)

        // The shared storage is in the lower-right room, not in the bedroom.
        // A short wall segment anchors it while preserving a clear approach tile.
        set(15, 6, TileType.WALL)
        set(14, 6, TileType.STORAGE)

        // Bunker hatch; the arrival tile just inside is kept clear.
        set(width / 2, 1, TileType.EXIT_TO_FOREST)

        // A small starter supply cache remains accessible inside the bunker.
        set(1, 8, TileType.RESOURCE_WOOD)
        set(4, 7, TileType.RESOURCE_METAL)
        set(7, 8, TileType.RESOURCE_FABRIC)
        set(10, 7, TileType.RESOURCE_FOOD)
        set(14, 8, TileType.RESOURCE_WATER)
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

    /** Add room partitions and door gaps around the permanent central expedition corridor. */
    private fun buildRoomPartitions() {
        listOf(13, 26).forEach { x ->
            for (y in 1 until height - 1) {
                if (y !in setOf(4, height / 2, height - 5) && get(x, y) == TileType.FLOOR) set(x, y, TileType.WALL)
            }
        }
        listOf(7, 16).forEach { y ->
            for (x in 1 until width - 1) {
                if (x !in setOf(6, 13, 20, 26, 33) && get(x, y) == TileType.FLOOR) set(x, y, TileType.WALL)
            }
        }
    }

    /**
     * Builds a deterministic expedition layout from a seed. A clear central corridor
     * always connects the spawn to the exit; obstacles and resources vary around it.
     */
    private fun buildProceduralLayout(seed: Long) {
        for (y in 1 until height - 1) for (x in 1 until width - 1) set(x, y, TileType.FLOOR)
        buildRoomPartitions()

        val exitX = if (zone == WorldZone.RUINS) width - 2 else 1
        set(exitX, height / 2, TileType.EXIT_TO_FOREST)

        val random = Random(seed)
        val spawnX = zone.spawnX.toInt()
        val spawnY = zone.spawnY.toInt()
        val reserved = (height / 2 - 2)..(height / 2 + 2)

        val reservedLandmarks = PointOfInterestCatalog.inZone(zone)
            .map { kotlin.math.floor(it.x).toInt() to kotlin.math.floor(it.y).toInt() }
            .toSet()

        fun validPlacement(x: Int, y: Int): Boolean {
            if (x <= 1 || x >= width - 2 || y <= 1 || y >= height - 2) return false
            if (reservedLandmarks.any { (lx, ly) -> abs(x - lx) <= 2 && abs(y - ly) <= 2 }) return false
            if (y in reserved) return false
            if (abs(x - spawnX) <= 3 && abs(y - spawnY) <= 3) return false
            if (x == exitX && abs(y - height / 2) <= 2) return false
            if (get(x, y) != TileType.FLOOR) return false
            // A sprite is wider than one logical cell; leave a complete empty cell around it.
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                if (get(x + dx, y + dy) != TileType.FLOOR) return false
            }
            return true
        }

        // Fewer, separated cover pieces replace the old tightly packed scatter.
        repeat(12) {
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
        while (placed < 14 && attempts < 360) {
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

    /**
     * Resolve the visible environment prop from the map itself. Rendering and collision
     * use this same placement result, so a large sprite cannot be drawn over a walkway.
     */
    fun environmentPropAt(x: Int, y: Int): String? {
        if (get(x, y) != TileType.FLOOR) return null
        if (x <= 1 || x >= width - 2 || y <= 1 || y >= height - 2) return null
        for (offsetY in -1..1) for (offsetX in -1..1) {
            if (offsetX == 0 && offsetY == 0) continue
            if (get(x + offsetX, y + offsetY) != TileType.FLOOR) return null
        }
        if (zone == WorldZone.BUNKER) {
            // Keep the upper service corridor and the bunker hatch approach clear.
            if (y <= 2) return null
        } else {
            if (kotlin.math.abs(y - height / 2) <= 2) return null
            if (zone == WorldZone.FOREST && kotlin.math.abs(x - width / 2) <= 2) return null
            if (kotlin.math.abs(x + 0.5f - zone.spawnX) < 2.0f &&
                kotlin.math.abs(y + 0.5f - zone.spawnY) < 2.0f) return null
            if (protectedExitCoordinates.any { (exitX, exitY) ->
                kotlin.math.abs(x - exitX) < 2 && kotlin.math.abs(y - exitY) < 2
            }) return null
            if (zoneLandmarks.any { poi ->
                val dx = x + 0.5f - poi.x
                val dy = y + 0.5f - poi.y
                dx * dx + dy * dy < 2.25f * 2.25f
            }) return null
        }
        return ZonePropSelector.spriteFor(zone, x, y)
    }

    private fun overlapsBlockingProp(worldX: Float, worldY: Float, radius: Float): Boolean {
        val playerLeft = worldX - radius
        val playerRight = worldX + radius
        val playerTop = worldY - radius
        val playerBottom = worldY + radius
        val cellX = kotlin.math.floor(worldX).toInt()
        val cellY = kotlin.math.floor(worldY).toInt()
        for (propY in cellY - 1..cellY + 1) for (propX in cellX - 1..cellX + 1) {
            val spriteId = environmentPropAt(propX, propY) ?: continue
            if (!ZonePropSelector.blocksMovement(spriteId)) continue
            val footprint = SpriteWorldMetrics.footprint(spriteId, 1f)
            val propLeft = propX + 0.5f - footprint.width * 0.5f
            val propRight = propLeft + footprint.width
            // Tall props are depth-sorted by their ground contact point. Only the
            // lower base strip blocks movement; the upper canopy/body remains passable
            // so the player can walk behind it instead of hitting an invisible wall.
            val collisionDepth = minOf(footprint.height, 0.28f)
            val propBottom = propY.toFloat() + footprint.height
            val propTop = propBottom - collisionDepth
            if (playerLeft < propRight && playerRight > propLeft &&
                playerTop < propBottom && playerBottom > propTop) return true
        }
        return false
    }

    fun isWalkable(worldX: Float, worldY: Float, radius: Float = 0.22f): Boolean {
        val points = listOf(
            worldX - radius to worldY - radius,
            worldX + radius to worldY - radius,
            worldX - radius to worldY + radius,
            worldX + radius to worldY + radius,
            worldX to worldY
        )
        val tilesClear = points.all { (x, y) ->
            val type = get(kotlin.math.floor(x).toInt(), kotlin.math.floor(y).toInt())
            type !in setOf(
                TileType.WALL, TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF,
                TileType.RADIO, TileType.DINING_TABLE, TileType.GENERATOR, TileType.MEDICAL_STATION
            )
        }
        return tilesClear && !overlapsBlockingProp(worldX, worldY, radius)
    }

    /** Return the first walkable tile on a shortest four-way path to a destination. */
    fun nextPathStep(startX: Float, startY: Float, targetX: Float, targetY: Float): Pair<Float, Float>? {
        val sx = kotlin.math.floor(startX).toInt().coerceIn(0, width - 1)
        val sy = kotlin.math.floor(startY).toInt().coerceIn(0, height - 1)
        val tx = kotlin.math.floor(targetX).toInt().coerceIn(0, width - 1)
        val ty = kotlin.math.floor(targetY).toInt().coerceIn(0, height - 1)
        if (sx == tx && sy == ty) return targetX to targetY
        if (!isWalkable(tx + 0.5f, ty + 0.5f)) return null
        val parentX = Array(height) { IntArray(width) { -1 } }
        val parentY = Array(height) { IntArray(width) { -1 } }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(sx to sy)
        parentX[sy][sx] = sx
        parentY[sy][sx] = sy
        val directions = arrayOf(0 to -1, 1 to 0, 0 to 1, -1 to 0)
        while (queue.isNotEmpty()) {
            val (x, y) = queue.removeFirst()
            for ((dx, dy) in directions) {
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until width || ny !in 0 until height ||
                    parentX[ny][nx] != -1 || !isWalkable(nx + 0.5f, ny + 0.5f)) continue
                parentX[ny][nx] = x
                parentY[ny][nx] = y
                if (nx == tx && ny == ty) {
                    var stepX = nx
                    var stepY = ny
                    while (parentX[stepY][stepX] != sx || parentY[stepY][stepX] != sy) {
                        val px = parentX[stepY][stepX]
                        val py = parentY[stepY][stepX]
                        stepX = px
                        stepY = py
                    }
                    return stepX + 0.5f to stepY + 0.5f
                }
                queue.addLast(nx to ny)
            }
        }
        return null
    }

    /** Grid line-of-sight check; solid bunker/ruin walls hide anything behind them. */
    fun hasLineOfSight(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean {
        var x = kotlin.math.floor(fromX).toInt()
        var y = kotlin.math.floor(fromY).toInt()
        val endX = kotlin.math.floor(toX).toInt()
        val endY = kotlin.math.floor(toY).toInt()
        val dx = kotlin.math.abs(endX - x)
        val dy = kotlin.math.abs(endY - y)
        val stepX = if (x < endX) 1 else -1
        val stepY = if (y < endY) 1 else -1
        var error = dx - dy
        while (x != endX || y != endY) {
            val twiceError = error * 2
            if (twiceError > -dy) { error -= dy; x += stepX }
            if (twiceError < dx) { error += dx; y += stepY }
            if ((x != endX || y != endY) && blocksVision(get(x, y))) return false
        }
        return true
    }

    private fun blocksVision(type: TileType): Boolean = when (type) {
        TileType.WALL, TileType.BED, TileType.WORKBENCH, TileType.STORAGE,
        TileType.BOOKSHELF, TileType.RADIO, TileType.DINING_TABLE,
        TileType.GENERATOR, TileType.MEDICAL_STATION -> true
        else -> false
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

    /** Locate a transition tile and return a safe walkable cell immediately inside it. */
    fun spawnBesideExit(exitType: TileType): Pair<Float, Float>? {
        var exit: Pair<Int, Int>? = null
        forEachTile { x, y, type ->
            if (exit == null && type == exitType) exit = x to y
        }
        val (exitX, exitY) = exit ?: return null
        val inwardOffsets = when {
            exitX <= 1 -> listOf(1 to 0, 0 to -1, 0 to 1)
            exitX >= width - 2 -> listOf(-1 to 0, 0 to -1, 0 to 1)
            exitY <= 1 -> listOf(0 to 1, -1 to 0, 1 to 0)
            exitY >= height - 2 -> listOf(0 to -1, -1 to 0, 1 to 0)
            else -> listOf(0 to 1, 0 to -1, -1 to 0, 1 to 0)
        }
        return inwardOffsets.firstNotNullOfOrNull { (dx, dy) ->
            val x = exitX + dx
            val y = exitY + dy
            if (isWalkable(x + 0.5f, y + 0.5f)) x + 0.5f to (y + 0.5f) else null
        }
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
                TileType.BED, TileType.WORKBENCH, TileType.STORAGE, TileType.BOOKSHELF, TileType.RADIO,
                TileType.DINING_TABLE, TileType.GENERATOR, TileType.MEDICAL_STATION, TileType.EXIT,
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


/** Creates the correctly sized map for each zone. The bunker footprint is intentionally compact. */
internal fun createZoneMap(zone: WorldZone, seed: Long? = null): TileMap =
    if (zone == WorldZone.BUNKER) TileMap(width = 17, height = 10, zone = zone, seed = seed)
    else TileMap(zone = zone, seed = seed)
