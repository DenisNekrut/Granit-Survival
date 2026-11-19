package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Test

class Camera2DTest {
    @Test
    fun viewportCanFollowActualDisplayAspectRatio() {
        val camera = Camera2D()
        camera.setViewport(26f, 12f)

        assertEquals(26f, camera.viewportWidth, 0.001f)
        assertEquals(12f, camera.viewportHeight, 0.001f)
        assertEquals(13f, camera.worldToViewX(camera.x), 0.001f)
        assertEquals(6f, camera.worldToViewY(camera.y), 0.001f)
    }

    @Test
    fun viewportCanZoomWithoutChangingWorldCoordinates() {
        val camera = Camera2D()
        camera.setViewport(30f, 15f)

        assertEquals(30f, camera.viewportWidth, 0.001f)
        assertEquals(15f, camera.viewportHeight, 0.001f)
        assertEquals(15f, camera.worldToViewX(camera.x), 0.001f)
        assertEquals(7.5f, camera.worldToViewY(camera.y), 0.001f)
    }
}
