package com.granit.survival

import android.app.Activity
import android.graphics.Color
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Settings controls and persistence, separated from MainActivity navigation. */
internal class SettingsScreen(
    private val activity: Activity,
    private val current: GameSettings,
    private val settings: GameSettingsRepository,
    private val createPanel: (String, String) -> LinearLayout,
    private val label: (String, Float, Int, Boolean) -> TextView,
    private val menuButton: (String, Boolean, () -> Unit) -> android.widget.Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val dp: (Int) -> Int,
    private val onSaved: () -> Unit,
    private val onCancel: () -> Unit
) {
    fun show() {
        val panel = createPanel("НАСТРОЙКИ", "Параметры управления и отображения")
        fun addSetting(
            name: String,
            initial: Float,
            max: Int,
            render: (Int) -> String,
            changed: (Int) -> Unit
        ) {
            panel.addView(label(name, 14f, Color.LTGRAY, false))
            val valueLabel = label(render(initial.toInt()), 12f, Color.rgb(190, 185, 160), false)
            panel.addView(valueLabel)
            panel.addView(SeekBar(activity).apply {
                this.max = max
                progress = initial.toInt().coerceIn(0, max)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        valueLabel.text = render(progress)
                        changed(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            })
        }

        var music = current.musicVolume
        var effects = current.effectsVolume
        var sensitivity = current.joystickSensitivity
        var scale = current.joystickScale
        var opacity = current.controlsOpacity
        var displayScale = current.displayScale
        addSetting("ГРОМКОСТЬ МУЗЫКИ", music * 100, 100, { "Уровень: $it%" }) { music = it / 100f }
        addSetting("ГРОМКОСТЬ ЭФФЕКТОВ", effects * 100, 100, { "Уровень: $it%" }) { effects = it / 100f }
        addSetting("ЧУВСТВИТЕЛЬНОСТЬ УПРАВЛЕНИЯ", sensitivity * 50, 100, { "Множитель: ${(it / 50f).formatOneDecimal()}x" }) { sensitivity = it / 50f }
        addSetting("РАЗМЕР ДЖОЙСТИКА", (scale - 0.8f) * 166.66f, 100, { "Размер: ${(0.8f + it / 166.66f).formatOneDecimal()}x" }) { scale = 0.8f + it / 166.66f }
        addSetting("ПРОЗРАЧНОСТЬ КНОПОК", (opacity - 0.35f) / 0.65f * 100, 100, { "Прозрачность: ${(35 + it * 0.65f).toInt()}%" }) { opacity = 0.35f + it / 100f * 0.65f }
        addSetting("МАСШТАБ ИГРОВОГО ЭКРАНА", (displayScale - 0.8f) * 250f, 100, { "Масштаб: ${80 + it / 100f * 40f}%" }) { displayScale = 0.8f + it / 250f }

        val quality = Spinner(activity).apply {
            adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, GraphicsQuality.values().map { it.name })
            setSelection(GraphicsQuality.values().indexOf(current.graphicsQuality).coerceAtLeast(0))
        }
        panel.addView(label("КАЧЕСТВО ГРАФИКИ", 14f, Color.LTGRAY, false))
        panel.addView(quality)
        val vibration = Switch(activity).apply {
            text = "ВИБРАЦИЯ"
            setTextColor(Color.WHITE)
            isChecked = current.vibrationEnabled
        }
        panel.addView(vibration)
        panel.addView(menuButton("СОХРАНИТЬ НАСТРОЙКИ", true) {
            settings.save(GameSettings(
                music, effects, sensitivity, scale, opacity,
                GraphicsQuality.values()[quality.selectedItemPosition],
                vibration.isChecked, displayScale
            ))
            Toast.makeText(activity, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            onSaved()
        })
        panel.addView(menuButton("ОТМЕНА", true) { onCancel() })
        showOverlay(panel)
    }

    private fun Float.formatOneDecimal() = String.format(java.util.Locale.ROOT, "%.1f", this)
}
