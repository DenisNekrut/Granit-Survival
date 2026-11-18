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
        private const val FIXED_STEP_SECONDS = 1f / 60f
        private const val MAX_FRAME_SECONDS = 0.25f
        private const val GAME_MINUTE_REAL_SECONDS = 1f
    }

    val player = PlayerState()
    val tileMap = TileMap()
    val camera = Camera2D()
    val inventory = Inventory()
    val bunkerStorage = BunkerStorage()
    val needs = SurvivalNeeds()
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
        gameMinuteAccumulator = 0f
        checkpointSystem.capture(this)
        return sleptMinutes
    }

    /** Restore the complete wake-up state after a failed expedition. */
    fun respawnAtWakeCheckpoint(): Boolean = checkpointSystem.restore(this)

    private fun fixedUpdate(delta: Float, input: EngineInput) {
        val length = sqrt(input.moveX * input.moveX + input.moveY * input.moveY)
        val scale = if (length > 1f) 1f / length else 1f
        val stepX = input.moveX * scale * player.speed * delta
        val stepY = input.moveY * scale * player.speed * delta

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
            }
            return
        }

        if (tileMap.nearestInteractable(player.x, player.y, 1.35f) != null) {
            interactionCount++
        }
    }
}
