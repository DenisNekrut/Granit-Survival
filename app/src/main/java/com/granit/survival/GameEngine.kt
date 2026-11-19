package com.granit.survival

import kotlin.math.sqrt
import kotlin.random.Random

/** Platform-independent simulation state for Granit Survival. */
data class EngineInput(val moveX: Float = 0f, val moveY: Float = 0f, val interact: Boolean = false)

enum class PlayerFacing { DOWN, UP, LEFT, RIGHT }

data class PlayerState(
    var x: Float = 9.5f,
    var y: Float = 7.0f,
    var speed: Float = 4.2f,
    var facing: PlayerFacing = PlayerFacing.DOWN
)

class GameEngine(private val mapSeedGenerator: () -> Long = { Random.nextLong() }) {
    companion object {
        const val WORLD_WIDTH = 40f
        const val WORLD_HEIGHT = 24f
        const val MEDICAL_KIT_HEALING = 35f
        const val BANDAGE_HEALING = 15f
        const val RESOURCE_GATHER_MINUTES = 2
        const val ZONE_TRAVEL_MINUTES = 8
        private const val FIXED_STEP_SECONDS = 1f / 60f
        private const val MAX_FRAME_SECONDS = 0.25f
        private const val GAME_MINUTE_REAL_SECONDS = 1f
    }

    val player = PlayerState()
    val equipment = PlayerEquipment()
    private val zoneMaps = mutableMapOf<WorldZone, TileMap>()
    var tileMap = TileMap().also { zoneMaps[WorldZone.BUNKER] = it }
        private set
    val zone: WorldZone get() = tileMap.zone
    val camera = Camera2D()
    val inventory = Inventory()
    val bunkerStorage = BunkerStorage()
    val needs = SurvivalNeeds()
    val psychology = PsychologicalState()
    val bookLibrary = BookLibrary()
    val radioSystem = BunkerRadioSystem()
    val worldClock = WorldClock()
    val weather: WeatherType get() = WeatherSystem.forDay(worldClock.day)
    val weatherMovementMultiplier: Float get() =
        if (zone == WorldZone.BUNKER) 1f else weather.outdoorMovementMultiplier
    val checkpointSystem = CheckpointSystem()

    private var accumulator = 0f
    private var gameMinuteAccumulator = 0f
    var simulationTimeSeconds: Double = 0.0
        private set
    var interactionCount: Int = 0
        private set
    var collectedResourceCount: Int = 0
        private set
    var lastCollectedItem: ItemType? = null
        private set
    var isMoving: Boolean = false
        private set
    private var wasInteracting = false
    private val discoveredPoiIds = linkedSetOf<String>()
    var lastDiscoveryTitle: String? = null
        private set
    var lastDiscoveryHudLabel: String? = null
        private set

    fun isPoiDiscovered(id: String): Boolean = id in discoveredPoiIds
    fun discoveredPointsOfInterest(): Set<String> = discoveredPoiIds.toSet()
    fun discoveredPointDetails(): List<PointOfInterest> =
        PointOfInterestCatalog.all().filter { it.id in discoveredPoiIds }

    fun restoreDiscoveredPointsOfInterest(ids: Set<String>) {
        discoveredPoiIds.clear()
        discoveredPoiIds.addAll(ids.filter { PointOfInterestCatalog.byId(it) != null })
        lastDiscoveryTitle = null
        lastDiscoveryHudLabel = null
    }

    fun scanNearbyPointsOfInterest(): List<PointOfInterest> {
        val found = PointOfInterestCatalog.inZone(zone).filter { poi ->
            val dx = player.x - poi.x
            val dy = player.y - poi.y
            dx * dx + dy * dy <= poi.discoveryRadius * poi.discoveryRadius &&
                discoveredPoiIds.add(poi.id)
        }
        found.lastOrNull()?.let {
            lastDiscoveryTitle = it.title
            lastDiscoveryHudLabel = it.hudLabel
        }
        return found
    }

    init {
        // The initial safe state acts as the first wake-up checkpoint.
        checkpointSystem.capture(this)
    }

    /** Clear latched touch state when the render surface is paused or recreated. */
    fun releaseInputState() {
        wasInteracting = false
        isMoving = false
    }

    fun updateFrame(frameDeltaSeconds: Float, input: EngineInput): Int {
        accumulator += frameDeltaSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        var steps = 0
        while (accumulator >= FIXED_STEP_SECONDS) {
            fixedUpdate(FIXED_STEP_SECONDS, input)
            accumulator -= FIXED_STEP_SECONDS
            steps++
        }
        return steps
    }

    /** Sleep until 07:00, restore energy, begin the next wake cycle and save a checkpoint. */
    fun sleepUntilMorning(): Int {
        val sleptMinutes = worldClock.sleepUntilMorning()
        needs.sleep(sleptMinutes)
        psychology.sleep(sleptMinutes)
        gameMinuteAccumulator = 0f
        if (needs.health <= 0f) {
            checkpointSystem.restore(this)
        } else {
            checkpointSystem.capture(this)
        }
        return sleptMinutes
    }

    /** Sleep is only available beside a bed in the bunker. */
    fun canSleep(): Boolean = zone == WorldZone.BUNKER && tileMap.isNearTile(TileType.BED, player.x, player.y, 1.5f)

    fun canAccessStorage(): Boolean = zone == WorldZone.BUNKER && tileMap.isNearTile(TileType.STORAGE, player.x, player.y, 1.5f)

    fun canUseWorkbench(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.WORKBENCH, player.x, player.y, 1.5f)

    fun canAccessBookshelf(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.BOOKSHELF, player.x, player.y, 1.5f)

    fun canUseRadio(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.RADIO, player.x, player.y, 1.5f) && radioSystem.canListen(worldClock.day)

    fun listenToRadio(): RadioEvent? {
        if (!canUseRadio()) return null
        val listeningDay = worldClock.day
        val event = radioSystem.eventForDay(listeningDay) ?: return null
        if (!advanceGameMinutes(event.broadcastMinutes)) return null
        radioSystem.markListened(listeningDay)
        psychology.applyReading(event.moraleGain, event.stressRelief)
        return if (event.id == "weather") {
            event.copy(description = "Прогноз на завтра: " + WeatherSystem.forDay(listeningDay + 1).forecastText)
        } else event
    }

    fun readBook(bookId: String): Boolean {
        if (!canAccessBookshelf()) return false
        val book = BookCatalog.byId(bookId) ?: return false
        if (!bookLibrary.markRead(book.id)) return false
        if (!advanceGameMinutes(book.readMinutes)) return false
        psychology.applyReading(book.moraleGain, book.stressRelief)
        return true
    }

    fun canCraft(recipeId: String): Boolean {
        val recipe = CraftingRecipe.byId(recipeId) ?: return false
        return recipe.isUnlocked(bookLibrary) && recipe.canCraft(inventory)
    }

    fun craft(recipeId: String): Boolean {
        if (!canUseWorkbench()) return false
        val recipe = CraftingRecipe.byId(recipeId) ?: return false
        if (!recipe.isUnlocked(bookLibrary) || !recipe.canCraft(inventory)) return false
        recipe.ingredients.forEach { (type, amount) -> inventory.remove(type, amount) }
        check(inventory.add(recipe.output, recipe.outputAmount) == 0) {
            "Crafting output did not fit after ingredient validation"
        }
        return advanceGameMinutes(recipe.craftMinutes)
    }

    fun useBandage(): Boolean {
        if (needs.health >= 100f || !inventory.remove(ItemType.BANDAGE, 1)) return false
        needs.restoreHealth(BANDAGE_HEALING)
        return advanceGameMinutes(3)
    }

    fun useMedicalKit(): Boolean {
        if (needs.health >= 100f || !inventory.remove(ItemType.MEDICAL_KIT, 1)) return false
        needs.restoreHealth(MEDICAL_KIT_HEALING)
        return advanceGameMinutes(5)
    }

    /**
     * Enter a zone. Resource maps marked procedural receive a fresh seed on every
     * new entry; fixed maps (bunker and first forest area) are reused unchanged.
     * Save/checkpoint restoration passes regenerate=false to rebuild the exact map.
     */
    fun changeZone(target: WorldZone, regenerate: Boolean = true) {
        val previousZone = zone
        if (target != previousZone) psychology.onZoneChange(previousZone, target)
        val targetMap = if (regenerate && target.isProcedurallyGenerated) {
            TileMap(zone = target, seed = mapSeedGenerator()).also { zoneMaps[target] = it }
        } else {
            zoneMaps.getOrPut(target) { TileMap(zone = target) }
        }
        tileMap = targetMap
        equipment.onZoneChanged(previousZone, target)
        lastDiscoveryTitle = null
        lastDiscoveryHudLabel = null
        player.x = target.spawnX
        player.y = target.spawnY
        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        wasInteracting = false
        isMoving = false
    }

    /** Snapshot only maps that actually exist; an unvisited expedition remains ungenerated. */
    fun zoneResourceSnapshot(): List<ZoneResourceSnapshot> = zoneMaps.map { (target, map) ->
        ZoneResourceSnapshot(target, map.resourceSnapshot(), map.layoutSeed)
    }

    fun restoreZoneResources(snapshots: List<ZoneResourceSnapshot>) {
        zoneMaps.clear()
        if (snapshots.isEmpty()) {
            zoneMaps[WorldZone.BUNKER] = TileMap(zone = WorldZone.BUNKER)
            return
        }
        snapshots.forEach { snapshot ->
            val map = TileMap(zone = snapshot.zone, seed = snapshot.seed)
            map.restoreResources(snapshot.resources)
            zoneMaps[snapshot.zone] = map
        }
        zoneMaps.putIfAbsent(WorldZone.BUNKER, TileMap(zone = WorldZone.BUNKER))
    }

    fun transferToStorage(type: ItemType, amount: Int = inventory.count(type)): Boolean =
        canAccessStorage() && bunkerStorage.deposit(inventory, type, amount)

    fun transferFromStorage(type: ItemType, amount: Int = bunkerStorage.count(type)): Boolean =
        canAccessStorage() && bunkerStorage.withdraw(inventory, type, amount)

    fun consumeFood(): Boolean {
        if (!inventory.remove(ItemType.CANNED_FOOD, 1)) return false
        needs.consumeFood()
        return advanceGameMinutes(5)
    }

    fun drinkWater(): Boolean {
        if (!inventory.remove(ItemType.WATER, 1)) return false
        needs.drinkWater()
        return advanceGameMinutes(5)
    }

    /** Apply the cost of a discrete action using the same clock/needs rules as real time. */
    private fun advanceGameMinutes(minutes: Int): Boolean {
        if (minutes <= 0) return true
        worldClock.advanceMinutes(minutes)
        needs.advanceAwake(minutes)
        psychology.advanceAwake(minutes, isMoving, zone)
        if (needs.health <= 0f) {
            checkpointSystem.restore(this)
            gameMinuteAccumulator = 0f
            return false
        }
        return true
    }

    private fun travelToZone(target: WorldZone) {
        if (target == zone) return
        changeZone(target)
        advanceGameMinutes(ZONE_TRAVEL_MINUTES)
    }

    /** Restore the complete wake-up state after a failed expedition. */
    fun respawnAtWakeCheckpoint(): Boolean = checkpointSystem.restore(this)

    fun createSaveSnapshot(): EngineSaveSnapshot = EngineSaveSnapshot(
        player = PlayerSnapshot(player.x, player.y, player.speed),
        inventory = inventory.snapshot(),
        bunkerStorage = bunkerStorage.snapshot(),
        needs = needs.snapshot(),
        psychology = psychology.snapshot(),
        clock = worldClock.snapshot(),
        remainingResources = tileMap.resourceSnapshot(),
        zone = zone,
        zoneResources = zoneResourceSnapshot(),
        bookLibrary = bookLibrary.snapshot(),
        radio = radioSystem.snapshot(),
        discoveredPoiIds = discoveredPoiIds.toSet(),
        equipment = equipment.snapshot(),
        wakeCheckpoint = checkpointSystem.snapshot()
    )

    fun restoreSaveSnapshot(snapshot: EngineSaveSnapshot) {
        restoreZoneResources(snapshot.zoneResources)
        changeZone(snapshot.zone, regenerate = false)
        equipment.restore(snapshot.equipment)
        restoreDiscoveredPointsOfInterest(snapshot.discoveredPoiIds)
        if (snapshot.zoneResources.isEmpty()) tileMap.restoreResources(snapshot.remainingResources)
        player.x = snapshot.player.x
        player.y = snapshot.player.y
        player.speed = snapshot.player.speed
        inventory.restore(snapshot.inventory)
        bunkerStorage.restore(snapshot.bunkerStorage)
        needs.restore(snapshot.needs)
        psychology.restore(snapshot.psychology)
        bookLibrary.restore(snapshot.bookLibrary)
        radioSystem.restore(snapshot.radio)
        worldClock.restore(snapshot.clock)
        tileMap.restoreResources(snapshot.remainingResources)
        checkpointSystem.restoreSnapshot(snapshot.wakeCheckpoint)
        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        accumulator = 0f
        gameMinuteAccumulator = 0f
        wasInteracting = false
        isMoving = false
        player.facing = PlayerFacing.DOWN
    }

    private fun fixedUpdate(delta: Float, input: EngineInput) {
        val length = sqrt(input.moveX * input.moveX + input.moveY * input.moveY)
        val scale = if (length > 1f) 1f / length else 1f
        val effectiveSpeed = player.speed * needs.movementSpeedMultiplier *
            psychology.movementSpeedMultiplier * weatherMovementMultiplier
        val stepX = input.moveX * scale * effectiveSpeed * delta
        val stepY = input.moveY * scale * effectiveSpeed * delta
        isMoving = kotlin.math.abs(stepX) > 0.0001f || kotlin.math.abs(stepY) > 0.0001f
        if (isMoving) {
            player.facing = if (kotlin.math.abs(input.moveX) > kotlin.math.abs(input.moveY)) {
                if (input.moveX < 0f) PlayerFacing.LEFT else PlayerFacing.RIGHT
            } else {
                if (input.moveY < 0f) PlayerFacing.UP else PlayerFacing.DOWN
            }
        }

        val nextX = (player.x + stepX).coerceIn(0.65f, WORLD_WIDTH - 0.65f)
        if (tileMap.isWalkable(nextX, player.y)) player.x = nextX

        val nextY = (player.y + stepY).coerceIn(0.8f, WORLD_HEIGHT - 0.8f)
        if (tileMap.isWalkable(player.x, nextY)) player.y = nextY

        scanNearbyPointsOfInterest()

        if (input.interact && !wasInteracting) interact()
        wasInteracting = input.interact

        gameMinuteAccumulator += delta
        while (gameMinuteAccumulator >= GAME_MINUTE_REAL_SECONDS) {
            worldClock.advanceMinutes(1)
            needs.advanceAwake(1)
            psychology.advanceAwake(1, isMoving, zone)
            gameMinuteAccumulator -= GAME_MINUTE_REAL_SECONDS
            if (needs.health <= 0f) {
                checkpointSystem.restore(this)
                gameMinuteAccumulator = 0f
                break
            }
        }

        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        simulationTimeSeconds += delta.toDouble()
    }

    private fun interact() {
        val resource = tileMap.nearestResource(player.x, player.y, 1.35f)
        if (resource != null) {
            // Remove the world node only after the inventory accepted the item.
            if (inventory.add(resource.type, 1) == 0 && tileMap.collectResource(resource)) {
                collectedResourceCount++
                lastCollectedItem = resource.type
                interactionCount++
                advanceGameMinutes(RESOURCE_GATHER_MINUTES)
            }
            return
        }

        when (tileMap.nearestInteractable(player.x, player.y, 1.35f)) {
            TileType.STORAGE -> {
                ItemType.values().forEach { type ->
                    val amount = inventory.count(type)
                    if (amount > 0) bunkerStorage.deposit(inventory, type, amount)
                }
                interactionCount++
            }
            TileType.EXIT_TO_BUNKER -> { travelToZone(WorldZone.BUNKER); interactionCount++ }
            TileType.EXIT_TO_FOREST -> { travelToZone(WorldZone.FOREST); interactionCount++ }
            TileType.EXIT_TO_RUINS -> { travelToZone(WorldZone.RUINS); interactionCount++ }
            TileType.EXIT_TO_ROAD -> { travelToZone(WorldZone.ROAD); interactionCount++ }
            TileType.EXIT -> {
                travelToZone(if (zone == WorldZone.BUNKER) WorldZone.FOREST else WorldZone.BUNKER)
                interactionCount++
            }
            TileType.BED, TileType.WORKBENCH, TileType.BOOKSHELF, TileType.RADIO -> interactionCount++
            else -> Unit
        }
    }
}
