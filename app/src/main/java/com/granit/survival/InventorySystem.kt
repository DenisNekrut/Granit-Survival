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

}
