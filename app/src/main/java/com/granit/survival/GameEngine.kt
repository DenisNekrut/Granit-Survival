package com.granit.survival

import kotlin.math.sqrt

/**
 * Platform-independent simulation state for Granit Survival.
 * The render thread owns updates; UI input is sampled once per frame.
 */
data class EngineInput(val moveX: Float = 0f, val moveY: Float = 0f)

data class PlayerState(
    var x: Float = 9.5f,
    var y: Float = 7.0f,
    var speed: Float = 4.2f
)

class GameEngine {
    companion object {
        const val WORLD_WIDTH = 20f
        const val WORLD_HEIGHT = 12f
        private const val FIXED_STEP_SECONDS = 1f / 60f
        private const val MAX_FRAME_SECONDS = 0.25f
    }

    val player = PlayerState()
    private var accumulator = 0f
    var simulationTimeSeconds: Double = 0.0
        private set

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
        player.x = (player.x + input.moveX * scale * player.speed * delta)
            .coerceIn(0.65f, WORLD_WIDTH - 0.65f)
        player.y = (player.y + input.moveY * scale * player.speed * delta)
            .coerceIn(0.8f, WORLD_HEIGHT - 0.8f)
        simulationTimeSeconds += delta.toDouble()
    }
}
