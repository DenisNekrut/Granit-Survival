package com.granit.survival

/** Separate bunker container. It uses the same stack rules as the field inventory. */
class BunkerStorage(
    val columns: Int = 6,
    val rows: Int = 4,
    stackLimit: Int = 20
) {
    private val contents = Inventory(columns, rows, stackLimit)

    override fun count(type: ItemType): Int = contents.count(type)

    override fun slot(index: Int): ItemStack? = contents.slot(index)

    override fun snapshot(): List<ItemStack?> = contents.snapshot()

    override fun restore(snapshot: List<ItemStack?>) = contents.restore(snapshot)
    override fun canAdd(type: ItemType, amount: Int): Boolean = contents.canAdd(type, amount)

    override fun add(type: ItemType, amount: Int): Int = contents.add(type, amount)

    override fun removeAt(index: Int, amount: Int): Boolean = contents.removeAt(index, amount)


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
        // Capacity is preflighted before source mutation; on the single game thread
        // the destination cannot change between this check and the add.
        check(to.add(stack.type, amount) == 0) {
            "Destination capacity changed during a slot transfer"
        }
        return true
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
