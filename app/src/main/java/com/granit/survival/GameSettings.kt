package com.granit.survival

import android.content.Context

enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

/** Persisted player preferences; UI controls will bind to this model. */
data class GameSettings(
    val musicVolume: Float = 0.7f,
    val effectsVolume: Float = 0.85f,
    val joystickSensitivity: Float = 1f,
    val joystickScale: Float = 1f,
    val controlsOpacity: Float = 0.8f,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val vibrationEnabled: Boolean = true
) {
    fun normalized() = copy(
        musicVolume = musicVolume.coerceIn(0f, 1f),
        effectsVolume = effectsVolume.coerceIn(0f, 1f),
        joystickSensitivity = joystickSensitivity.coerceIn(0.5f, 2f),
        joystickScale = joystickScale.coerceIn(0.8f, 1.4f),
        controlsOpacity = controlsOpacity.coerceIn(0.35f, 1f)
    )
}

/** Android preference adapter; game systems consume the platform-independent GameSettings model. */
class GameSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): GameSettings = GameSettings(
        musicVolume = preferences.getFloat(KEY_MUSIC, 0.7f),
        effectsVolume = preferences.getFloat(KEY_EFFECTS, 0.85f),
        joystickSensitivity = preferences.getFloat(KEY_SENSITIVITY, 1f),
        joystickScale = preferences.getFloat(KEY_JOYSTICK_SCALE, 1f),
        controlsOpacity = preferences.getFloat(KEY_CONTROLS_OPACITY, 0.8f),
        graphicsQuality = runCatching {
            GraphicsQuality.valueOf(preferences.getString(KEY_GRAPHICS, GraphicsQuality.MEDIUM.name)!!)
        }.getOrDefault(GraphicsQuality.MEDIUM),
        vibrationEnabled = preferences.getBoolean(KEY_VIBRATION, true)
    ).normalized()

    fun save(settings: GameSettings) {
        val safe = settings.normalized()
        preferences.edit()
            .putFloat(KEY_MUSIC, safe.musicVolume)
            .putFloat(KEY_EFFECTS, safe.effectsVolume)
            .putFloat(KEY_SENSITIVITY, safe.joystickSensitivity)
            .putFloat(KEY_JOYSTICK_SCALE, safe.joystickScale)
            .putFloat(KEY_CONTROLS_OPACITY, safe.controlsOpacity)
            .putString(KEY_GRAPHICS, safe.graphicsQuality.name)
            .putBoolean(KEY_VIBRATION, safe.vibrationEnabled)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "granit_game_settings"
        private const val KEY_MUSIC = "music_volume"
        private const val KEY_EFFECTS = "effects_volume"
        private const val KEY_SENSITIVITY = "joystick_sensitivity"
        private const val KEY_JOYSTICK_SCALE = "joystick_scale"
        private const val KEY_CONTROLS_OPACITY = "controls_opacity"
        private const val KEY_GRAPHICS = "graphics_quality"
        private const val KEY_VIBRATION = "vibration_enabled"
    }
}
