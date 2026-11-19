package com.granit.survival

/** Bunker framing tuned so the camera must track the player on wide phone screens. */
internal object BunkerCameraProfile {
    private const val MAX_VIEWPORT_WIDTH = 12.5f
    private const val MAX_VIEWPORT_HEIGHT = 7.2f

    fun viewportHeight(screenWidth: Int, screenHeight: Int): Float {
        val width = screenWidth.coerceAtLeast(1).toFloat()
        val height = screenHeight.coerceAtLeast(1).toFloat()
        return minOf(MAX_VIEWPORT_HEIGHT, MAX_VIEWPORT_WIDTH * height / width)
            .coerceAtLeast(1f)
    }
}
