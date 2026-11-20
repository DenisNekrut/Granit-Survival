package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt


/** Pixel-perfect Canvas icon; each source texel is drawn without filtering. */
internal class InventoryPixelIconView(context: Context, private val spriteId: String) : View(context) {
    private val pixelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        isFilterBitmap = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val sprite = PixelSpriteAtlas.sprite(spriteId) ?: return
        val frame = sprite.frames.firstOrNull() ?: return
        val frameWidth = frame.maxOfOrNull { it.length } ?: return
        if (frameWidth == 0 || frame.isEmpty()) return
        val pixelSize = minOf(width / frameWidth, height / frame.size).toFloat()
        if (pixelSize <= 0f) return
        val left = (width - frameWidth * pixelSize) * 0.5f
        val top = (height - frame.size * pixelSize) * 0.5f
        frame.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { column, symbol ->
                if (symbol == '.') return@forEachIndexed
                val color = sprite.palette[symbol] ?: return@forEachIndexed
                pixelPaint.color = Color.rgb(
                    (color[0] * 255f).roundToInt().coerceIn(0,255),
                    (color[1] * 255f).roundToInt().coerceIn(0,255),
                    (color[2] * 255f).roundToInt().coerceIn(0,255)
                )
                canvas.drawRect(
                    left + column * pixelSize,
                    top + rowIndex * pixelSize,
                    left + (column + 1) * pixelSize,
                    top + (rowIndex + 1) * pixelSize,
                    pixelPaint
                )
            }
        }
    }
}

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
    private fun showCannotUseMessage() {
        Toast.makeText(
            activity,
            "Сейчас предмет нельзя использовать: проверь состояние персонажа",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun show(engine: GameEngine) {
        val panel = createPanel("РЮКЗАК", "Личные вещи персонажа · ${engine.inventory.columns} × ${engine.inventory.rows}")
        panel.addView(InventoryGridView(
            context = activity,
            columns = engine.inventory.columns,
            rows = engine.inventory.rows,
            cellHeightDp = 104,
            iconSizeDp = 42,
            dp = dp,
            slotAt = engine.inventory::slot
        ) { _, stack ->
            val useAction: (() -> Unit)? = when (stack.type) {
                ItemType.CANNED_FOOD -> ({
                    if (consumeFood()) refresh() else showCannotUseMessage()
                })
                ItemType.WATER -> ({
                    if (drinkWater()) refresh() else showCannotUseMessage()
                })
                ItemType.BANDAGE -> ({
                    if (useBandage()) refresh() else showCannotUseMessage()
                })
                ItemType.MEDICAL_KIT -> ({
                    if (useMedicalKit()) refresh() else showCannotUseMessage()
                })
                else -> null
            }
            if (useAction != null) {
                AlertDialog.Builder(activity)
                    .setTitle(stack.type.displayName)
                    .setItems(arrayOf("ИСПОЛЬЗОВАТЬ")) { _, _ -> useAction.invoke() }
                    .setNegativeButton("ОТМЕНА", null)
                    .show()
            }
        })
        panel.addView(menuButton("ЗАКРЫТЬ РЮКЗАК", true) { close() })
        showOverlay(panel)
    }
}
