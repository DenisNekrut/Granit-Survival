package com.granit.survival

import org.junit.Assert.assertTrue
import org.junit.Test

class BunkerLightingTest {
    @Test
    fun poweredPoolsAreConcentricAndTwoDimensional() {
        val layers = BunkerLighting.poweredLayers
        assertTrue(layers.size >= 5)
        assertTrue(layers.all { it.size > 0f })
        assertTrue(layers.zipWithNext().all { (outer, inner) -> outer.size > inner.size })
        assertTrue(layers.all { it.alpha in 0f..0.15f })
    }

    @Test
    fun emergencyPoolsAlsoUseSymmetricFootprints() {
        val layers = BunkerLighting.emergencyLayers
        assertTrue(layers.isNotEmpty())
        assertTrue(layers.all { it.size > 0f && it.alpha in 0f..0.15f })
        assertTrue(layers.zipWithNext().all { (outer, inner) -> outer.size > inner.size })
    }

    @Test
    fun poweredAndEmergencyLightingHaveDifferentReach() {
        assertTrue(BunkerLighting.poweredLayers.first().size >
            BunkerLighting.emergencyLayers.first().size)
        assertTrue(BunkerLighting.poweredLayers.last().alpha > BunkerLighting.emergencyLayers.first().alpha)
    }
}
