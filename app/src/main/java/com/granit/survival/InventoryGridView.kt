package com.granit.survival

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shared pixel-art grid for every item container.
 * The view only renders slots; ownership, transfer rules and item actions stay in the caller.
 */
internal class InventoryGridView(
    context: Context,
    columns: Int,
    rows: Int,
    private val cellHeightDp: Int,
    private val iconSizeDp: Int,
    private val dp: (Int) -> Int,
    slotAt: (Int) -> ItemStack?,
    onSlotClick: (Int, ItemStack) -> Unit
) : LinearLayout(context) {

    init {
        require(container.columns > 0 && container.rows > 0)
        orientation = VERTICAL

        for (rowIndex in 0 until container.rows) {
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
            }
            for (column in 0 until container.columns) {
                val index = rowIndex * columns + column
                val stack = slotAt(index)
                val cell = LinearLayout(context).apply {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(3), dp(4), dp(3), dp(4))
                    setBackgroundColor(Color.argb(175, 28, 43, 39))
                }

                if (stack == null) {
                    cell.addView(cellLabel("ПУСТО", 9f, Color.rgb(151, 170, 157), false))
                } else {
                    cell.addView(
                        InventoryPixelIconView(context, stack.type.iconSpriteId),
                        LayoutParams(dp(iconSizeDp), dp(iconSizeDp))
                    )
                    cell.addView(cellLabel(
                        stack.type.displayName,
                        8f,
                        Color.rgb(214, 221, 207),
                        false
                    ))
                    cell.addView(cellLabel(
                        "×${stack.quantity}",
                        10f,
                        Color.WHITE,
                        true
                    ))
                    cell.isClickable = true
                    cell.setOnClickListener { onSlotClick(index, stack.copy()) }
                }

                val cellParams = LayoutParams(0, dp(cellHeightDp), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                }
                row.addView(cell, cellParams)
            }
            addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun cellLabel(
        value: String,
        sizeSp: Float,
        color: Int,
        bold: Boolean
    ): TextView = TextView(context).apply {
        text = value
        textSize = sizeSp
        setTextColor(color)
        gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        maxLines = 2
    }
}
