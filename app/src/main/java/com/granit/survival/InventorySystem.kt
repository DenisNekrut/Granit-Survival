package com.granit.survival

/** Coordinates backpack and bunker-container mutations without owning UI or world rules. */
internal class InventorySystem(
    val backpack: Inventory,
    val bunkerStorage: BunkerStorage
) {
    fun add(type: ItemType, amount: Int): Int = backpack.add(type, amount)
    fun remove(type: ItemType, amount: Int): Boolean = backpack.remove(type, amount)

    fun transferToStorage(type: ItemType, amount: Int): Boolean =
        amount > 0 && bunkerStorage.deposit(backpack, type, amount)

    fun transferFromStorage(type: ItemType, amount: Int): Boolean =
        amount > 0 && bunkerStorage.withdraw(backpack, type, amount)

    fun depositAll(): Boolean {
        var transferred = false
        ItemType.values().forEach { type ->
            val amount = backpack.count(type)
            if (amount > 0 && bunkerStorage.deposit(backpack, type, amount)) transferred = true
        }
        return transferred
    }
}
