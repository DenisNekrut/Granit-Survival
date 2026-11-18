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
    }

    val player = PlayerState()
    val tileMap = TileMap()
    val camera = Camera2D()
    private var accumulator = 0f
    var simulationTimeSeconds: Double = 0.0
        private set
    var interactionCount: Int = 0
        private set
    private var wasInteracting = false

    fun updateFrame(frameDeltaSeconds: Float, input: EngineInput) {
        accumulator += frameDeltaSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        while (accumulator >= FIXED_STEP_SECONDS) {
            fixedUpdate(FIXED_STEP_SECONDS, input)
            accumulator -= FIXED_STEP_SECONDS
        }
    }

    private fun fixedUpdate(delta: Float, input: EngineInput) {
        val length = sqrt(input.moveX * input.moveX + input.moveY * input.moveY)
        val scale = if (length > 1f) 1f / length else 1f
        val stepX = input.moveX * scale * player.speed * delta
        val stepY = input.moveY * scale * player.speed * delta

        val nextX = (player.x + stepX).coerceIn(0.65f, WORLD_WIDTH - 0.65f)
        if (tileMap.isWalkable(nextX, player.y)) player.x = nextX

        val nextY = (player.y + stepY).coerceIn(0.8f, WORLD_HEIGHT - 0.8f)
        if (tileMap.isWalkable(player.x, nextY)) player.y = nextY

        if (input.interact && !wasInteracting) {
            val nearby = tileMap.nearestInteractable(player.x, player.y, 1.35f)
            if (nearby != null) interactionCount++
        }
        wasInteracting = input.interact

        camera.follow(player.x, player.y, WORLD_WIDTH, WORLD_HEIGHT)
        simulationTimeSeconds += delta.toDouble()
    }
}
