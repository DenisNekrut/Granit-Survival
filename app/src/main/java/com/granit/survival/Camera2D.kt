package com.granit.survival

import kotlin.math.exp

/** Camera that smoothly tracks the player while keeping the viewport inside world bounds. */
class Camera2D(
    viewportWidth: Float = 20f,
    viewportHeight: Float = 12f
) {
    companion object {
        private const val FOLLOW_SPEED = 10f
    }

    var viewportWidth: Float = viewportWidth
        private set
    var viewportHeight: Float = viewportHeight
        private set

    fun setViewport(width: Float, height: Float) {
        viewportWidth = width.coerceAtLeast(1f)
        viewportHeight = height.coerceAtLeast(1f)
    }

    var x: Float = viewportWidth * 0.5f
        private set
    var y: Float = viewportHeight * 0.5f
        private set

    /** Smooth follow with time-based damping, so camera speed does not depend on FPS. */
    fun follow(
        targetX: Float,
        targetY: Float,
        worldWidth: Float,
        worldHeight: Float,
        deltaSeconds: Float = 1f / 60f
    ) {
        val (desiredX, desiredY) = clampedTarget(targetX, targetY, worldWidth, worldHeight)
        val delta = deltaSeconds.coerceIn(0f, 0.25f)
        val blend = (1f - exp(-FOLLOW_SPEED * delta)).coerceIn(0f, 1f)
        // A smaller world must be centered immediately; otherwise empty space is exposed
        // while the camera slowly catches up from the previous viewport position.
        x = if (worldWidth.coerceAtLeast(1f) <= viewportWidth) desiredX else x + (desiredX - x) * blend
        y = if (worldHeight.coerceAtLeast(1f) <= viewportHeight) desiredY else y + (desiredY - y) * blend
    }

    /** Snap on zone loads/respawns to avoid showing the previous zone's camera position. */
    fun snapTo(targetX: Float, targetY: Float, worldWidth: Float, worldHeight: Float) {
        val (targetCameraX, targetCameraY) = clampedTarget(targetX, targetY, worldWidth, worldHeight)
        x = targetCameraX
        y = targetCameraY
    }

    private fun clampedTarget(
        targetX: Float,
        targetY: Float,
        worldWidth: Float,
        worldHeight: Float
    ): Pair<Float, Float> {
        fun clampAxis(target: Float, worldSize: Float, viewportSize: Float): Float {
            val safeWorldSize = worldSize.coerceAtLeast(1f)
            if (safeWorldSize <= viewportSize) return safeWorldSize * 0.5f
            val halfViewport = viewportSize * 0.5f
            return target.coerceIn(halfViewport, safeWorldSize - halfViewport)
        }
        return clampAxis(targetX, worldWidth, viewportWidth) to
            clampAxis(targetY, worldHeight, viewportHeight)
    }

    fun worldToViewX(worldX: Float): Float = worldX - (x - viewportWidth * 0.5f)
    fun worldToViewY(worldY: Float): Float = worldY - (y - viewportHeight * 0.5f)

    fun screenToWorldX(screenX: Float, scale: Float, offsetX: Float): Float =
        screenX / scale + x - viewportWidth * 0.5f - offsetX / scale

    fun screenToWorldY(screenY: Float, scale: Float, offsetY: Float): Float =
        screenY / scale + y - viewportHeight * 0.5f - offsetY / scale
}
