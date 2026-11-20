package com.granit.survival

/** Loot specialization is derived from the stable container id, never rerolled on reopen. */
enum class WorldLootTier(val label: String) {
    STANDARD("ОБЫЧНЫЙ"),
    MATERIALS("МАТЕРИАЛЫ"),
    MEDICAL("МЕДИЦИНСКИЙ"),
    PROVISIONS("ПРОВИЗИЯ"),
    RARE("РЕДКИЕ НАХОДКИ")
}

/** Persistent location-bound loot storage, saved with its owning world zone. */
class WorldContainer(
    val id: String,
    val zone: WorldZone,
    val x: Int,
    val y: Int,
    val title: String,
    override val columns: Int = 4,
    override val rows: Int = 3,
    lootTierOverride: WorldLootTier? = null
) : ItemContainer {
    private val contents = Inventory(columns, rows)

    val lootTier: WorldLootTier = lootTierOverride ?: WorldLootTier.values()[Math.floorMod(id.hashCode(), WorldLootTier.values().size)]
    val displayTitle: String get() = "$title · ${lootTier.label}"

    override fun slot(index: Int): ItemStack? = contents.slot(index)
    override fun count(type: ItemType): Int = contents.count(type)
    override fun snapshot(): List<ItemStack?> = contents.snapshot()
    override fun restore(snapshot: List<ItemStack?>) = contents.restore(snapshot)
    override fun canAdd(type: ItemType, amount: Int): Boolean = contents.canAdd(type, amount)
    override fun add(type: ItemType, amount: Int): Int = contents.add(type, amount)
    override fun removeAt(index: Int, amount: Int): Boolean = contents.removeAt(index, amount)
    override fun remove(type: ItemType, amount: Int): Boolean = contents.remove(type, amount)

    internal fun seedInitialContents() {
        if (contents.snapshot().any { it != null }) return
        when (zone) {
            WorldZone.BUNKER -> Unit
            WorldZone.FOREST -> {
                contents.add(ItemType.WOOD, 3)
                contents.add(ItemType.WATER, 1)
                contents.add(ItemType.CANNED_FOOD, 1)
                contents.add(ItemType.ELECTRONICS, 1)
            }
            WorldZone.RUINS -> {
                contents.add(ItemType.METAL, 4)
                contents.add(ItemType.FABRIC, 2)
                contents.add(ItemType.BANDAGE, 1)
            }
            WorldZone.ROAD -> {
                contents.add(ItemType.CANNED_FOOD, 2)
                contents.add(ItemType.WATER, 2)
                contents.add(ItemType.METAL, 1)
            }
        }

        when (lootTier) {
            WorldLootTier.STANDARD -> when (zone) {
                WorldZone.FOREST -> contents.add(ItemType.FABRIC, 1)
                WorldZone.RUINS -> contents.add(ItemType.WOOD, 1)
                WorldZone.ROAD -> contents.add(ItemType.METAL, 1)
                WorldZone.BUNKER -> Unit
            }
            WorldLootTier.MATERIALS -> {
                contents.add(ItemType.METAL, if (zone == WorldZone.RUINS) 5 else 3)
                contents.add(ItemType.FABRIC, 2)
                contents.add(ItemType.ELECTRONICS, 2)
            }
            WorldLootTier.MEDICAL -> {
                contents.add(ItemType.BANDAGE, 2)
                contents.add(ItemType.MEDICAL_KIT, 1)
            }
            WorldLootTier.PROVISIONS -> {
                contents.add(ItemType.CANNED_FOOD, 3)
                contents.add(ItemType.WATER, 3)
            }
            WorldLootTier.RARE -> {
                contents.add(ItemType.MEDICAL_KIT, 1)
                contents.add(ItemType.BANDAGE, 1)
                contents.add(ItemType.METAL, 3)
                contents.add(ItemType.CANNED_FOOD, 1)
                contents.add(ItemType.BATTERY, 2)
            }
        }
    }

    internal fun toSnapshot() = WorldContainerSnapshot(id, x, y, title, contents.snapshot())
}

internal object ItemContainerTransfer {
    fun moveAt(from: ItemContainer, to: ItemContainer, slotIndex: Int, amount: Int): Boolean {
        val stack = from.slot(slotIndex) ?: return false
        if (amount <= 0 || stack.quantity < amount || !to.canAdd(stack.type, amount)) return false
        if (!from.removeAt(slotIndex, amount)) return false
        check(to.add(stack.type, amount) == 0) {
            "Destination capacity changed during a slot transfer"
        }
        return true
    }
}
