package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

/** Save-slot UI and confirmation flows, independent from Activity lifecycle handling. */
internal class SaveSlotsScreen(
    private val activity: Activity,
    private val saves: GameSaveRepository,
    private val createPanel: (String, String) -> LinearLayout,
    private val label: (String, Float, Int, Boolean) -> android.widget.TextView,
    private val menuButton: (String, Boolean, () -> Unit) -> android.widget.Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val weightedParams: () -> LinearLayout.LayoutParams,
    private val dp: (Int) -> Int,
    private val loadSlot: (Int) -> Boolean,
    private val saveSlot: (Int) -> Unit,
    private val onLoaded: () -> Unit,
    private val onSaved: () -> Unit,
    private val onBack: () -> Unit
) {
    fun show(allowSave: Boolean) {
        val panel = createPanel("СОХРАНЕНИЯ", "Три независимых слота на устройстве")
        saves.listSlots().forEach { slot ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.rgb(34, 47, 43))
                    cornerRadius = dp(9).toFloat()
                    setStroke(dp(1), Color.rgb(77, 100, 84))
                }
            }
            val info = when {
                slot.corrupted -> "СЛОТ ${slot.slot}  ·  ФАЙЛ ПОВРЕЖДЁН"
                !slot.exists -> "СЛОТ ${slot.slot}  ·  ПУСТО"
                else -> {
                    val time = "%02d:%02d".format(slot.hour ?: 0, slot.minute ?: 0)
                    val date = slot.savedAtMillis?.let {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
                    } ?: ""
                    "СЛОТ ${slot.slot}  ·  ДЕНЬ ${slot.day ?: 1}, $time\n$date"
                }
            }
            row.addView(label(info, 14f, Color.WHITE, false))
            val actions = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(menuButton("ЗАГРУЗИТЬ", slot.exists && !slot.corrupted) {
                val loaded = runCatching { loadSlot(slot.slot) }.getOrDefault(false)
                if (loaded) onLoaded()
                else Toast.makeText(activity, "Не удалось загрузить слот", Toast.LENGTH_LONG).show()
            }, weightedParams())
            actions.addView(menuButton("СОХРАНИТЬ", allowSave) {
                AlertDialog.Builder(activity)
                    .setTitle("Перезаписать слот ${slot.slot}?")
                    .setMessage("Старое сохранение в этом слоте будет заменено.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("СОХРАНИТЬ") { _, _ ->
                        runCatching { saveSlot(slot.slot) }
                            .onSuccess {
                                Toast.makeText(activity, "Игра сохранена", Toast.LENGTH_SHORT).show()
                                onSaved()
                            }
                            .onFailure {
                                Toast.makeText(activity, "Ошибка сохранения: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }.show()
            }, weightedParams())
            actions.addView(menuButton("УДАЛИТЬ", slot.exists) {
                AlertDialog.Builder(activity)
                    .setTitle("Удалить слот ${slot.slot}?")
                    .setMessage("Это действие нельзя отменить.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("УДАЛИТЬ") { _, _ ->
                        saves.delete(slot.slot)
                        show(allowSave)
                    }.show()
            }, weightedParams())
            row.addView(actions)
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        panel.addView(menuButton("НАЗАД", true) { onBack() })
        showOverlay(panel)
    }
}
