package com.granit.survival

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Backpack UI separated from Activity navigation and game lifecycle.
 * The Activity supplies factories and actions so the screen cannot own the game loop.
 */
internal class InventoryScreen(
    private val activity: Activity,
    private val createPanel: (String, String) -> LinearLayout,
    private val label: (String, Float, Int, Boolean) -> TextView,
    private val menuButton: (String, Boolean, () -> Unit) -> Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val close: () -> Unit,
    private val refresh: () -> Unit,
    private val consumeFood: () -> Boolean,
    private val drinkWater: () -> Boolean,
    private val useBandage: () -> Boolean,
    private val useMedicalKit: () -> Boolean,
    private val dp: (Int) -> Int
) {
    fun show(engine: GameEngine) {
        val panel = createPanel("РЮКЗАК", "Личные вещи персонажа · 4 × 4 ячейки")
        for (index in 0 until engine.inventory.columns * engine.inventory.rows) {
            val stack = engine.inventory.slot(index)
            if (stack == null) {
                panel.addView(label("Ячейка ${index + 1} · пусто", 12f, Color.rgb(126, 139, 132), false))
                continue
            }
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(3), dp(6), dp(3))
            }
            row.addView(label(stack.type.displayName + " × " + stack.quantity, 13f, Color.WHITE, false).apply {
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, -2, 1f))
            val useAction: (() -> Unit)? = when (stack.type) {
                ItemType.CANNED_FOOD -> ({ if (consumeFood()) refresh() })
                ItemType.WATER -> ({ if (drinkWater()) refresh() })
                ItemType.BANDAGE -> ({ if (useBandage()) refresh() })
                ItemType.MEDICAL_KIT -> ({ if (useMedicalKit()) refresh() })
                else -> null
            }
            if (useAction != null) {
                row.addView(menuButton("ИСПОЛЬЗОВАТЬ", when (stack.type) {
                    ItemType.BANDAGE, ItemType.MEDICAL_KIT -> engine.needs.health < 100f
                    else -> true
                }) { useAction.invoke() }, LinearLayout.LayoutParams(dp(142), -2))
            } else {
                row.addView(label("ПАССИВНЫЙ ПРЕДМЕТ", 10f, Color.rgb(145, 160, 149), false))
            }
            panel.addView(row)
        }
        panel.addView(menuButton("ЗАКРЫТЬ РЮКЗАК", true) { close() })
        showOverlay(panel)
    }
}
