package com.granit.survival

import kotlin.math.sqrt

/** Platform-independent simulation state for Granit Survival. */
data class EngineInput(val moveX: Float = 0f, val moveY: Float = 0f, val interact: Boolean = false)

data class PlayerState(
    var x: Float = 9.5f,
    var y: Float = 7.0f,
    var speed: Float = 4.2f
)

class GameEngine {
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
    private val zoneMaps = mutableMapOf<WorldZone, TileMap>()
    var tileMap = TileMap().also { zoneMaps[WorldZone.BUNKER] = it }
        private set
    val zone: WorldZone get() = tileMap.zone
    val camera = Camera2D()
    val inventory = Inventory()
    val bunkerStorage = BunkerStorage()
    val needs = SurvivalNeeds()
    val psychology = PsychologicalState()
    val worldClock = WorldClock()
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

    init {
        // The initial safe state acts as the first wake-up checkpoint.
        checkpointSystem.capture(this)
    }

    fun updateFrame(frameDeltaSeconds: Float, input: EngineInput) {
        accumulator += frameDeltaSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        while (accumulator >= FIXED_STEP_SECONDS) {
            fixedUpdate(FIXED_STEP_SECONDS, input)
            accumulator -= FIXED_STEP_SECONDS
        }
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

    fun craft(recipeId: String): Boolean {
        if (!canUseWorkbench()) return false
        val recipe = CraftingRecipe.byId(recipeId) ?: return false
        if (!recipe.canCraft(inventory)) return false
        recipe.ingredients.forEach { (type, amount) -> inventory.remove(type, amount) }
        check(inventory.add(recipe.output, recipe.outputAmount) == 0) {
            "Crafting output did not fit after ingredient validation"
        }
        advanceGameMinutes(recipe.craftMinutes)
        return true
    }

    fun useBandage(): Boolean {
        if (!inventory.remove(ItemType.BANDAGE, 1) || needs.health >= 100f) return false
        needs.restoreHealth(BANDAGE_HEALING)
        advanceGameMinutes(3)
        return true
    }

    fun useMedicalKit(): Boolean {
        if (!inventory.remove(ItemType.MEDICAL_KIT, 1)) return false
        needs.restoreHealth(MEDICAL_KIT_HEALING)
        advanceGameMinutes(5)
        return true
    }

    fun changeZone(target: WorldZone) {
        if (target != zone) psychology.onZoneChange(zone, target)
        tileMap = zoneMaps.getOrPut(target) { TileMap(zone = target) }
        player.x = target.spawnX
        player.y = target.spawnY
        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        wasInteracting = false
        isMoving = false
    }

    fun zoneResourceSnapshot(): List<ZoneResourceSnapshot> = WorldZone.values().map { target ->
        ZoneResourceSnapshot(target, zoneMaps.getOrPut(target) { TileMap(zone = target) }.resourceSnapshot())
    }

    fun restoreZoneResources(snapshots: List<ZoneResourceSnapshot>) {
        if (snapshots.isEmpty()) {
            zoneMaps.clear()
            WorldZone.values().forEach { target -> zoneMaps[target] = TileMap(zone = target) }
            return
        }
        snapshots.forEach { snapshot ->
            zoneMaps.getOrPut(snapshot.zone) { TileMap(zone = snapshot.zone) }
                .restoreResources(snapshot.resources)
        }
    }

    fun transferToStorage(type: ItemType, amount: Int = inventory.count(type)): Boolean =
        canAccessStorage() && bunkerStorage.deposit(inventory, type, amount)

    fun transferFromStorage(type: ItemType, amount: Int = bunkerStorage.count(type)): Boolean =
        canAccessStorage() && bunkerStorage.withdraw(inventory, type, amount)

    fun consumeFood(): Boolean {
        if (!inventory.remove(ItemType.CANNED_FOOD, 1)) return false
        needs.consumeFood()
        advanceGameMinutes(5)
        return true
    }

    fun drinkWater(): Boolean {
        if (!inventory.remove(ItemType.WATER, 1)) return false
        needs.drinkWater()
        advanceGameMinutes(5)
        return true
    }

    /** Apply the cost of a discrete action using the same clock/needs rules as real time. */
    private fun advanceGameMinutes(minutes: Int) {
        if (minutes <= 0) return
        worldClock.advanceMinutes(minutes)
        needs.advanceAwake(minutes)
        if (needs.health <= 0f) {
            checkpointSystem.restore(this)
            gameMinuteAccumulator = 0f
        }
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
        wakeCheckpoint = checkpointSystem.snapshot()
    )

    fun restoreSaveSnapshot(snapshot: EngineSaveSnapshot) {
        restoreZoneResources(snapshot.zoneResources)
        changeZone(snapshot.zone)
        if (snapshot.zoneResources.isEmpty()) tileMap.restoreResources(snapshot.remainingResources)
        player.x = snapshot.player.x
        player.y = snapshot.player.y
        player.speed = snapshot.player.speed
        inventory.restore(snapshot.inventory)
        bunkerStorage.restore(snapshot.bunkerStorage)
        needs.restore(snapshot.needs)
        psychology.restore(snapshot.psychology)
        worldClock.restore(snapshot.clock)
        tileMap.restoreResources(snapshot.remainingResources)
        checkpointSystem.restoreSnapshot(snapshot.wakeCheckpoint)
        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        accumulator = 0f
        gameMinuteAccumulator = 0f
        wasInteracting = false
        isMoving = false
    }

    private fun fixedUpdate(delta: Float, input: EngineInput) {
        val length = sqrt(input.moveX * input.moveX + input.moveY * input.moveY)
        val scale = if (length > 1f) 1f / length else 1f
        val effectiveSpeed = player.speed * needs.movementSpeedMultiplier
        val stepX = input.moveX * scale * effectiveSpeed * delta
        val stepY = input.moveY * scale * effectiveSpeed * delta
        isMoving = kotlin.math.abs(stepX) > 0.0001f || kotlin.math.abs(stepY) > 0.0001f

        val nextX = (player.x + stepX).coerceIn(0.65f, WORLD_WIDTH - 0.65f)
        if (tileMap.isWalkable(nextX, player.y)) player.x = nextX

        val nextY = (player.y + stepY).coerceIn(0.8f, WORLD_HEIGHT - 0.8f)
        if (tileMap.isWalkable(player.x, nextY)) player.y = nextY

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
            TileType.BED, TileType.WORKBENCH -> interactionCount++
            else -> Unit
        }
    }
}
