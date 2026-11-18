package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CraftingTest {
    @Test
    fun recipeRequiresWorkbenchAndConsumesIngredients() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.FABRIC, 2)
        engine.inventory.add(ItemType.METAL, 1)
        assertFalse(engine.craft("medical_kit"))

        engine.player.x = 9.5f
        engine.player.y = 1.5f
        val startTime = engine.worldClock.totalMinutes
        assertTrue(engine.canUseWorkbench())
        assertTrue(engine.craft("medical_kit"))
        assertEquals(0, engine.inventory.count(ItemType.FABRIC))
        assertEquals(0, engine.inventory.count(ItemType.METAL))
        assertEquals(1, engine.inventory.count(ItemType.MEDICAL_KIT))
        assertEquals(startTime + 15, engine.worldClock.totalMinutes)
    }

    @Test
    fun craftingFailsWithoutMaterialsOrWhenInventoryCannotFitOutput() {
        val engine = GameEngine()
        engine.player.x = 9.5f
        engine.player.y = 1.5f
        assertFalse(engine.craft("medical_kit"))

        val recipe = CraftingRecipe.byId("medical_kit")!!
        val full = Inventory(columns = 3, rows = 1)
        full.add(ItemType.FABRIC, 2)
        full.add(ItemType.METAL, 1)
        full.add(ItemType.WOOD, 20)
        assertFalse(recipe.canCraft(full))
    }

    @Test
    fun medicalKitRestoresHealthAndIsConsumed() {
        val engine = GameEngine()
        engine.needs.restore(SurvivalNeedsSnapshot(health = 50f, hunger = 80f, thirst = 80f, energy = 50f))
        val before = engine.needs.health
        engine.inventory.add(ItemType.MEDICAL_KIT, 1)
        assertTrue(engine.useMedicalKit())
        assertTrue(engine.needs.health > before)
        assertEquals(0, engine.inventory.count(ItemType.MEDICAL_KIT))
    }

    @Test
    fun bandageCanBeCraftedAndUsedForSmallHealing() {
        val engine = GameEngine()
        engine.player.x = 9.5f
        engine.player.y = 1.5f
        engine.inventory.add(ItemType.FABRIC, 1)
        val start = engine.worldClock.totalMinutes

        assertTrue(engine.craft("bandage"))
        assertEquals(0, engine.inventory.count(ItemType.FABRIC))
        assertEquals(1, engine.inventory.count(ItemType.BANDAGE))
        assertEquals(start + 3, engine.worldClock.totalMinutes)

        engine.needs.restore(SurvivalNeedsSnapshot(health = 60f, hunger = 80f, thirst = 80f, energy = 80f))
        assertTrue(engine.useBandage())
        assertEquals(0, engine.inventory.count(ItemType.BANDAGE))
        assertEquals(75f, engine.needs.health, 0.001f)
    }

    @Test
    fun healingItemsAreNotConsumedAtFullHealth() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.BANDAGE, 1)
        engine.inventory.add(ItemType.MEDICAL_KIT, 1)

        assertFalse(engine.useBandage())
        assertFalse(engine.useMedicalKit())
        assertEquals(1, engine.inventory.count(ItemType.BANDAGE))
        assertEquals(1, engine.inventory.count(ItemType.MEDICAL_KIT))
    }

    @Test
    fun unknownRecipeDoesNotChangeInventory() {
        val engine = GameEngine()
        engine.inventory.add(ItemType.FABRIC, 3)
        assertFalse(engine.craft("unknown"))
        assertEquals(3, engine.inventory.count(ItemType.FABRIC))
    }
}
