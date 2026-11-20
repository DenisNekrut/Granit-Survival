package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryProgressTest {
    @Test
    fun objectivesAdvanceOnlyAfterKnownStoryEvents() {
        val story = StoryProgressSystem()

        assertEquals("inspect_generator", story.currentObjective?.id)
        assertFalse(story.recordEvent("unknown_event"))
        assertEquals(0, story.completedObjectiveCount)

        assertTrue(story.recordEvent(StoryEventIds.GENERATOR_PANEL_OPENED))
        assertFalse(story.recordEvent(StoryEventIds.GENERATOR_PANEL_OPENED))
        assertEquals("check_supplies", story.currentObjective?.id)

        story.recordEvent(StoryEventIds.RADIO_PANEL_OPENED)
        assertEquals("check_supplies", story.currentObjective?.id)
        story.recordEvent(StoryEventIds.STORAGE_OPENED)
        assertEquals("leave_bunker", story.currentObjective?.id)
    }

    @Test
    fun storyProgressSnapshotRestoresAndIgnoresUnknownEvents() {
        val story = StoryProgressSystem()
        story.recordEvent(StoryEventIds.GENERATOR_PANEL_OPENED)
        story.recordEvent(StoryEventIds.STORAGE_OPENED)

        val restored = StoryProgressSystem(
            story.snapshot().copy(completedEventIds = story.snapshot().completedEventIds + "not_a_real_event")
        )

        assertEquals(2, restored.completedObjectiveCount)
        assertEquals("inspect_radio", restored.currentObjective?.id)
        assertTrue(restored.hasEvent(StoryEventIds.STORAGE_OPENED))
    }
    @Test
    fun observationsAppearOnlyAfterInspectionAndSurviveRestore() {
        val story = StoryProgressSystem()

        assertTrue(story.observations().isEmpty())
        story.recordEvent(StoryEventIds.BED_INSPECTED)
        val notes = story.observations()

        assertEquals(1, notes.size)
        assertEquals("Спальня", notes.single().title)

        val restored = StoryProgressSystem(story.snapshot())
        assertEquals(notes, restored.observations())
    }

}
