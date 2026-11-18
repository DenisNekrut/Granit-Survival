package com.granit.survival

/** Separate bunker container. It uses the same stack rules as the field inventory. */
class BunkerStorage(
    columns: Int = 6,
    rows: Int = 4,
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

    private fun transfer(from: Inventory, to: Inventory, type: ItemType, amount: Int): Boolean {
        if (amount <= 0 || from.count(type) < amount) return false
        if (!from.remove(type, amount)) return false

        val unaccepted = to.add(type, amount)
        if (unaccepted == 0) return true

        // Roll back both sides if the destination cannot accept the whole transfer.
        to.remove(type, amount - unaccepted)
        from.add(type, amount)
        return false
    }
}
