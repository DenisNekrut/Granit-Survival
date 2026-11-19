package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun invalidViewportDimensionsAreClampedToSafeMinimum() {
        val camera = Camera2D()
        camera.setViewport(0f, -5f)
        assertEquals(1f, camera.viewportWidth, 0.001f)
        assertEquals(1f, camera.viewportHeight, 0.001f)
    }

    @Test
    fun bunkerViewportIsNarrowEnoughToFollowAcrossLandscapeAspects() {
        val height = BunkerCameraProfile.viewportHeight(2400, 1080)
        val width = 2400f / (1080f / height)
        assertTrue(width < 17f)
        val camera = Camera2D(width, height)
        camera.snapTo(8.5f, 2.5f, 17f, 10f)
        val beforeX = camera.x
        camera.follow(13.5f, 7.5f, 17f, 10f, 0.1f)
        assertTrue(camera.x > beforeX)
        assertTrue(camera.y > 2.5f)
    }

    @Test
    fun cameraSmoothlyMovesTowardPlayer() {
        val camera = Camera2D(10f, 6f)
        camera.snapTo(5f, 3f, 100f, 60f)

        camera.follow(50f, 30f, 100f, 60f, 1f / 60f)

        assertTrue(camera.x > 5f)
        assertTrue(camera.y > 3f)
        assertTrue(camera.x < 50f)
        assertTrue(camera.y < 30f)
    }

    @Test
    fun cameraStopsAtWorldEdges() {
        val camera = Camera2D(10f, 6f)
        camera.snapTo(50f, 30f, 100f, 60f)

        // Follow is deliberately smoothed, so allow it to settle at the clamped edge.
        repeat(100) {
            camera.follow(0f, 0f, 100f, 60f, 1f / 60f)
        }

        assertEquals(3.5f, camera.x, 0.001f)
        assertEquals(1.5f, camera.y, 0.001f)
    }

    @Test
    fun cameraShowsNeutralMarginOutsideMapInsteadOfPuttingMapEdgeOnScreen() {
        val camera = Camera2D(10f, 6f)
        camera.snapTo(50f, 30f, 100f, 60f)

        repeat(100) {
            camera.follow(0f, 0f, 100f, 60f, 1f / 60f)
        }

        assertEquals(-Camera2D.WORLD_EDGE_PADDING,
            camera.x - camera.viewportWidth * 0.5f, 0.001f)
        assertEquals(-Camera2D.WORLD_EDGE_PADDING,
            camera.y - camera.viewportHeight * 0.5f, 0.001f)
    }

    @Test
    fun cameraCentersWorldThatIsSmallerThanViewport() {
        val camera = Camera2D(20f, 12f)

        camera.follow(2f, 1f, 17f, 10f, 0.25f)

        assertEquals(8.5f, camera.x, 0.001f)
        assertEquals(5f, camera.y, 0.001f)
    }

    @Test
    fun snapToImmediatelyResetsCameraAfterZoneChange() {
        val camera = Camera2D(10f, 6f)
        camera.snapTo(50f, 30f, 100f, 60f)

        camera.snapTo(8f, 4f, 17f, 10f)

        assertEquals(8f, camera.x, 0.001f)
        assertEquals(4f, camera.y, 0.001f)
    }

    @Test
    fun followSmoothingIsIndependentOfUpdateFrequency() {
        val thirtyFps = Camera2D(10f, 6f)
        val sixtyFps = Camera2D(10f, 6f)
        thirtyFps.snapTo(5f, 3f, 100f, 60f)
        sixtyFps.snapTo(5f, 3f, 100f, 60f)

        repeat(2) { thirtyFps.follow(50f, 30f, 100f, 60f, 1f / 30f) }
        repeat(4) { sixtyFps.follow(50f, 30f, 100f, 60f, 1f / 60f) }

        assertEquals(thirtyFps.x, sixtyFps.x, 0.001f)
        assertEquals(thirtyFps.y, sixtyFps.y, 0.001f)
    }
}
