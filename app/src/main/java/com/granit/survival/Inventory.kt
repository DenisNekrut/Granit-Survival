package com.granit.survival

enum class ItemType(val displayName: String, val color: Int) {
    WOOD("Древесина", 0xFF9A7046.toInt()),
    METAL("Металл", 0xFF8A969A.toInt()),
    FABRIC("Ткань", 0xFF718B86.toInt()),
    CANNED_FOOD("Консервы", 0xFFB28A43.toInt()),
    WATER("Вода", 0xFF4E91B5.toInt())
}

data class ItemStack(
    val type: ItemType,
    var quantity: Int
)

/** Fixed 4x4 inventory. Stack size is intentionally small for the first prototype. */
class Inventory(
    val columns: Int = 4,
    val rows: Int = 4,
    private val stackLimit: Int = 20
) {
    private val slots = MutableList<ItemStack?>(columns * rows) { null }

    fun slot(index: Int): ItemStack? = slots.getOrNull(index)?.copy()

    fun count(type: ItemType): Int = slots.filterNotNull()
        .filter { it.type == type }
        .sumOf { it.quantity }

    fun add(type: ItemType, amount: Int = 1): Int {
        if (amount <= 0) return amount
        var remaining = amount

        for (stack in slots) {
            if (stack?.type == type && stack.quantity < stackLimit) {
                val moved = minOf(stackLimit - stack.quantity, remaining)
                stack.quantity += moved
                remaining -= moved
                if (remaining == 0) return 0
            }
        }

        for (index in slots.indices) {
            if (slots[index] == null) {
                val moved = minOf(stackLimit, remaining)
                slots[index] = ItemStack(type, moved)
                remaining -= moved
                if (remaining == 0) return 0
            }
        }
        return remaining
    }

    fun remove(type: ItemType, amount: Int = 1): Boolean {
        if (amount <= 0 || count(type) < amount) return false
        var remaining = amount
        for (index in slots.indices) {
            val stack = slots[index] ?: continue
            if (stack.type != type) continue
            val moved = minOf(stack.quantity, remaining)
            stack.quantity -= moved
            remaining -= moved
            if (stack.quantity == 0) slots[index] = null
            if (remaining == 0) return true
        }
        return true
    }

    fun snapshot(): List<ItemStack?> = slots.map { it?.copy() }
}
