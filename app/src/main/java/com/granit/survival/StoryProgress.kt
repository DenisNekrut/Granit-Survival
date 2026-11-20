package com.granit.survival

/** Stable identifiers for authored story events. Event IDs are persisted in save files. */
object StoryEventIds {
    const val GENERATOR_PANEL_OPENED = "chapter_01.generator_panel_opened"
    const val STORAGE_OPENED = "chapter_01.storage_opened"
    const val RADIO_PANEL_OPENED = "chapter_01.radio_panel_opened"
    const val FIRST_BUNKER_EXIT = "chapter_01.first_bunker_exit"

    val all: Set<String> = setOf(
        GENERATOR_PANEL_OPENED,
        STORAGE_OPENED,
        RADIO_PANEL_OPENED,
        FIRST_BUNKER_EXIT
    )
}

data class StoryObjective(
    val id: String,
    val title: String,
    val description: String,
    val eventId: String
)

data class StoryProgressSnapshot(
    val chapterId: String = StoryProgressSystem.CHAPTER_ONE_ID,
    val completedEventIds: Set<String> = emptySet()
)

/**
 * Persistent narrative state. It is deliberately independent from the survival
 * wake-up checkpoint: dying or restoring an expedition checkpoint must not undo
 * story discoveries that the player has already made.
 */
class StoryProgressSystem(initial: StoryProgressSnapshot = StoryProgressSnapshot()) {
    companion object {
        const val CHAPTER_ONE_ID = "chapter_01"
        const val CHAPTER_ONE_TITLE = "Тихое убежище"

        val chapterOneObjectives = listOf(
            StoryObjective(
                id = "inspect_generator",
                title = "Осмотреть генераторную",
                description = "Проверь состояние генератора и электропитания убежища.",
                eventId = StoryEventIds.GENERATOR_PANEL_OPENED
            ),
            StoryObjective(
                id = "check_supplies",
                title = "Проверить запасы",
                description = "Осмотри хранилище и выясни, какие припасы доступны.",
                eventId = StoryEventIds.STORAGE_OPENED
            ),
            StoryObjective(
                id = "inspect_radio",
                title = "Проверить радиоприёмник",
                description = "Осмотри радиостанцию. Пока неясно, что именно удастся услышать.",
                eventId = StoryEventIds.RADIO_PANEL_OPENED
            ),
            StoryObjective(
                id = "leave_bunker",
                title = "Покинуть убежище",
                description = "Когда будешь готов, выйди наружу и начни исследование окрестностей.",
                eventId = StoryEventIds.FIRST_BUNKER_EXIT
            )
        )
    }

    private val completedEvents = linkedSetOf<String>().apply {
        addAll(initial.completedEventIds.filter { it in StoryEventIds.all })
    }

    val objectives: List<StoryObjective>
        get() = chapterOneObjectives

    val completedObjectiveCount: Int
        get() = objectives.count { it.eventId in completedEvents }

    val currentObjective: StoryObjective?
        get() = objectives.firstOrNull { it.eventId !in completedEvents }

    fun isCompleted(objective: StoryObjective): Boolean =
        objective.eventId in completedEvents

    /** Returns true only when a known event is recorded for the first time. */
    fun recordEvent(eventId: String): Boolean {
        if (eventId !in StoryEventIds.all) return false
        return completedEvents.add(eventId)
    }

    fun hasEvent(eventId: String): Boolean = eventId in completedEvents

    fun snapshot(): StoryProgressSnapshot = StoryProgressSnapshot(
        chapterId = CHAPTER_ONE_ID,
        completedEventIds = completedEvents.toSet()
    )

    fun restore(snapshot: StoryProgressSnapshot) {
        completedEvents.clear()
        if (snapshot.chapterId == CHAPTER_ONE_ID) {
            completedEvents.addAll(snapshot.completedEventIds.filter { it in StoryEventIds.all })
        }
    }
}
