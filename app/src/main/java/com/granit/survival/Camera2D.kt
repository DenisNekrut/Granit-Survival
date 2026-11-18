package com.granit.survival

/** Camera with a fixed virtual viewport and smooth follow target. */
class Camera2D(
    val viewportWidth: Float = 20f,
    val viewportHeight: Float = 12f
) {
    var x: Float = viewportWidth * 0.5f
        private set
    var y: Float = viewportHeight * 0.5f
        private set

    fun follow(targetX: Float, targetY: Float, worldWidth: Float, worldHeight: Float) {
        val halfWidth = viewportWidth * 0.5f
        val halfHeight = viewportHeight * 0.5f
        val desiredX = targetX.coerceIn(halfWidth, (worldWidth - halfWidth).coerceAtLeast(halfWidth))
        val desiredY = targetY.coerceIn(halfHeight, (worldHeight - halfHeight).coerceAtLeast(halfHeight))
        // Smooth camera movement, independent of frame rate.
        x += (desiredX - x) * 0.18f
        y += (desiredY - y) * 0.18f
    }

    fun worldToViewX(worldX: Float): Float = worldX - (x - viewportWidth * 0.5f)
    fun worldToViewY(worldY: Float): Float = worldY - (y - viewportHeight * 0.5f)

    fun screenToWorldX(screenX: Float, scale: Float, offsetX: Float): Float =
        screenX / scale + x - viewportWidth * 0.5f - offsetX / scale

    fun screenToWorldY(screenY: Float, scale: Float, offsetY: Float): Float =
        screenY / scale + y - viewportHeight * 0.5f - offsetY / scale
}
