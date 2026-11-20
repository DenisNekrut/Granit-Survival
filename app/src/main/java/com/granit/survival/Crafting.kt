package com.granit.survival

data class CraftingRecipe(
    val id: String,
    val title: String,
    val ingredients: Map<ItemType, Int>,
    val output: ItemType,
    val outputAmount: Int = 1,
    val craftMinutes: Int,
    val requiredBookId: String? = null
) {
    init {
        require(id.isNotBlank())
        require(ingredients.isNotEmpty() && ingredients.values.all { it > 0 })
        require(outputAmount > 0 && craftMinutes >= 0)
    }

    fun isUnlocked(library: BookLibrary): Boolean = requiredBookId == null || library.hasRead(requiredBookId)

    fun canCraft(inventory: Inventory): Boolean {
        if (!ingredients.all { (type, amount) -> inventory.count(type) >= amount }) return false

        // Check capacity after consuming ingredients. A full backpack can still craft
        // when the consumed stacks free a slot for the result.
        val projected = Inventory(inventory.columns, inventory.rows).also {
            it.restore(inventory.snapshot())
        }
        ingredients.forEach { (type, amount) -> projected.remove(type, amount) }
        return projected.canAdd(output, outputAmount)
    }

    companion object {
        val all = listOf(
            CraftingRecipe(
                id = "bandage",
                title = "Перевязочный бинт",
                ingredients = mapOf(ItemType.FABRIC to 1),
                output = ItemType.BANDAGE,
                craftMinutes = 3
            ),
            CraftingRecipe(
                id = "battery_pack",
                title = "Комплект батареек",
                ingredients = mapOf(ItemType.METAL to 2, ItemType.ELECTRONICS to 1),
                output = ItemType.BATTERY,
                outputAmount = 2,
                craftMinutes = 5
            ),
            CraftingRecipe(
                id = "medical_kit",
                title = "Полевая аптечка",
                ingredients = mapOf(ItemType.FABRIC to 2, ItemType.METAL to 1),
                output = ItemType.MEDICAL_KIT,
                craftMinutes = 15,
                requiredBookId = "field_first_aid"
            )
        )

        fun byId(id: String): CraftingRecipe? = all.firstOrNull { it.id == id }
    }
}
