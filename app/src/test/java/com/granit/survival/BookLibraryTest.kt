package com.granit.survival

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class BookLibraryTest {
    @Test
    fun readingRequiresBookshelfAndUnlocksTechnicalRecipe() {
        val engine = GameEngine()
        assertFalse(engine.readBook("field_first_aid"))
        engine.player.x = 20.5f
        engine.player.y = 2.5f
        val before = engine.worldClock.totalMinutes
        assertTrue(engine.readBook("field_first_aid"))
        assertTrue(engine.bookLibrary.hasRead("field_first_aid"))
        assertTrue(CraftingRecipe.byId("medical_kit")!!.isUnlocked(engine.bookLibrary))
        assertEquals(before + 45, engine.worldClock.totalMinutes)
        engine.player.x = 9.5f
        engine.player.y = 1.5f
        assertFalse(engine.canCraft("medical_kit")) // unlocked, but ingredients are missing
    }

    @Test
    fun fictionAndDiaryReadingAffectMoraleAndStress() {
        val engine = GameEngine()
        engine.player.x = 20.5f
        engine.player.y = 2.5f
        val moraleBefore = engine.psychology.morale
        assertTrue(engine.readBook("old_novel"))
        assertTrue(engine.psychology.morale > moraleBefore)
        val stressBefore = engine.psychology.stress
        assertTrue(engine.readBook("bunker_diary"))
        assertTrue(engine.psychology.stress < stressBefore)
        assertFalse(engine.readBook("old_novel"))
    }

    @Test
    fun readingProgressRollsBackUntilNextWakeCheckpoint() {
        val engine = GameEngine()
        engine.sleepUntilMorning()
        engine.player.x = 20.5f
        engine.player.y = 2.5f
        assertTrue(engine.readBook("field_first_aid"))
        assertTrue(engine.bookLibrary.hasRead("field_first_aid"))
        assertTrue(engine.respawnAtWakeCheckpoint())
        assertFalse(engine.bookLibrary.hasRead("field_first_aid"))
    }

    @Test
    fun bookLibrarySnapshotRestoresOnlyKnownBookIds() {
        val library = BookLibrary()
        assertTrue(library.markRead("old_novel"))
        val snapshot = library.snapshot()
        val restored = BookLibrary()
        restored.restore(snapshot.copy(readBookIds = snapshot.readBookIds + "unknown"))
        assertTrue(restored.hasRead("old_novel"))
        assertFalse(restored.hasRead("unknown"))
        assertEquals(snapshot, restored.snapshot())
    }
}
