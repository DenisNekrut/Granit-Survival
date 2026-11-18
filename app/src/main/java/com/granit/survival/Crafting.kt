package com.granit.survival

data class CraftingRecipe(
    val id: String,
    val title: String,
    val ingredients: Map<ItemType, Int>,
    val output: ItemType,
    val outputAmount: Int = 1,
    val craftMinutes: Int
) {
    init {
        require(id.isNotBlank())
        require(ingredients.isNotEmpty() && ingredients.values.all { it > 0 })
        require(outputAmount > 0 && craftMinutes >= 0)
    }

    fun canCraft(inventory: Inventory): Boolean =
        ingredients.all { (type, amount) -> inventory.count(type) >= amount } &&
            inventory.canAdd(output, outputAmount)

    companion object {
        val all = listOf(
            CraftingRecipe(
                id = "medical_kit",
                title = "Полевая аптечка",
                ingredients = mapOf(ItemType.FABRIC to 2, ItemType.METAL to 1),
                output = ItemType.MEDICAL_KIT,
                craftMinutes = 15
            )
        )

        fun byId(id: String): CraftingRecipe? = all.firstOrNull { it.id == id }
    }
}
