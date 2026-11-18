package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Test

class GameSettingsTest {
    @Test
    fun normalizedSettingsClampValuesToSupportedRanges() {
        val settings = GameSettings(
            musicVolume = 1.8f,
            effectsVolume = -0.2f,
            joystickSensitivity = 3f,
            joystickScale = 0.1f,
            controlsOpacity = 2f
        ).normalized()

        assertEquals(1f, settings.musicVolume, 0.001f)
        assertEquals(0f, settings.effectsVolume, 0.001f)
        assertEquals(2f, settings.joystickSensitivity, 0.001f)
        assertEquals(0.8f, settings.joystickScale, 0.001f)
        assertEquals(1f, settings.controlsOpacity, 0.001f)
    }

    @Test
    fun defaultsAreSafeForFirstLaunch() {
        val settings = GameSettings().normalized()
        assertEquals(0.7f, settings.musicVolume, 0.001f)
        assertEquals(GraphicsQuality.MEDIUM, settings.graphicsQuality)
        assertEquals(true, settings.vibrationEnabled)
    }
}
