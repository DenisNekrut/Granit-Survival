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


    @Test
    fun libraryMarginNotesRequireExplicitInspectionAndAreIdempotent() {
        val story = StoryProgressSystem()
        assertFalse(story.hasEvent(StoryEventIds.LIBRARY_NOTES_READ))

        assertTrue(story.recordEvent(StoryEventIds.LIBRARY_NOTES_READ))
        assertFalse(story.recordEvent(StoryEventIds.LIBRARY_NOTES_READ))
        assertTrue(story.observations().any { it.eventId == StoryEventIds.LIBRARY_NOTES_READ })

        val restored = StoryProgressSystem(story.snapshot())
        assertTrue(restored.hasEvent(StoryEventIds.LIBRARY_NOTES_READ))
    }

    @Test
    fun radioAndLibraryCluesCanBeCrossCheckedOnceAndPersist() {
        val story = StoryProgressSystem()
        story.recordEvent(StoryEventIds.LIBRARY_NOTES_READ)
        assertFalse(story.hasEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED))

        story.recordEvent(StoryEventIds.RADIO_PANEL_OPENED)
        assertTrue(story.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED))
        assertFalse(story.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED))
        assertTrue(story.observations().any {
            it.eventId == StoryEventIds.RADIO_NOTES_CROSS_CHECKED
        })

        val restored = StoryProgressSystem(story.snapshot())
        assertTrue(restored.hasEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED))
    }

    @Test
    fun forestRelayInspectionIsIdempotentAndPersists() {
        val story = StoryProgressSystem()
        assertFalse(story.hasEvent(StoryEventIds.FOREST_RELAY_INSPECTED))
        assertTrue(story.recordEvent(StoryEventIds.FOREST_RELAY_INSPECTED))
        assertFalse(story.recordEvent(StoryEventIds.FOREST_RELAY_INSPECTED))
        val restored = StoryProgressSystem(story.snapshot())
        assertTrue(restored.hasEvent(StoryEventIds.FOREST_RELAY_INSPECTED))
        assertTrue(restored.observations().any {
            it.eventId == StoryEventIds.FOREST_RELAY_INSPECTED
        })
    }

    @Test
    fun firstTheoryRequiresCrossCheckAndIsMutuallyExclusiveAndPersistent() {
        val story = StoryProgressSystem()
        assertFalse(story.chooseFirstTheory(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST))
        story.recordEvent(StoryEventIds.LIBRARY_NOTES_READ)
        story.recordEvent(StoryEventIds.RADIO_PANEL_OPENED)
        story.recordEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)

        assertTrue(story.chooseFirstTheory(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST))
        assertFalse(story.chooseFirstTheory(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE))
        assertTrue(story.hasEvent(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST))
        assertFalse(story.hasEvent(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE))

        val restored = StoryProgressSystem(story.snapshot())
        assertTrue(restored.hasEvent(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST))
        assertTrue(restored.observations().any {
            it.eventId == StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST
        })
    }
}
