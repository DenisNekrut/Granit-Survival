package com.granit.survival

/** Separate bunker container. It uses the same stack rules as the field inventory. */
class BunkerStorage(
    val columns: Int = 6,
    val rows: Int = 4,
    stackLimit: Int = 20
) {
    private val contents = Inventory(columns, rows, stackLimit)

    fun count(type: ItemType): Int = contents.count(type)

    fun slot(index: Int): ItemStack? = contents.slot(index)

    fun snapshot(): List<ItemStack?> = contents.snapshot()

    fun restore(snapshot: List<ItemStack?>) = contents.restore(snapshot)

    fun deposit(from: Inventory, type: ItemType, amount: Int): Boolean =
        transfer(from, contents, type, amount)

    fun withdraw(to: Inventory, type: ItemType, amount: Int): Boolean =
        transfer(contents, to, type, amount)

    /** Moves items from one explicitly selected backpack cell. */
    fun depositAt(from: Inventory, slotIndex: Int, amount: Int): Boolean =
        transferAt(from, contents, slotIndex, amount)

    /** Moves items from one explicitly selected storage cell. */
    fun withdrawAt(to: Inventory, slotIndex: Int, amount: Int): Boolean =
        transferAt(contents, to, slotIndex, amount)

    private fun transferAt(from: Inventory, to: Inventory, slotIndex: Int, amount: Int): Boolean {
        val stack = from.slot(slotIndex) ?: return false
        if (amount <= 0 || stack.quantity < amount || !to.canAdd(stack.type, amount)) return false
        if (!from.removeAt(slotIndex, amount)) return false
        val unaccepted = to.add(stack.type, amount)
        if (unaccepted == 0) return true

        // Defensive rollback; capacity is checked before the source is mutated.
        check(to.remove(stack.type, amount - unaccepted))
        check(from.add(stack.type, amount) == 0)
        return false
    }

    private fun transfer(from: Inventory, to: Inventory, type: ItemType, amount: Int): Boolean {
        if (amount <= 0 || from.count(type) < amount || !to.canAdd(type, amount)) return false
        if (!from.remove(type, amount)) return false

        // Capacity is preflighted, so this add must be complete and cannot reorder items on rollback.
        check(to.add(type, amount) == 0) {
            "Destination capacity changed during an inventory transfer"
        }
        return true
    }
}
