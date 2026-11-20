package com.granit.survival

/** Stable identifiers for authored story events. Event IDs are persisted in save files. */
object StoryEventIds {
    const val GENERATOR_PANEL_OPENED = "chapter_01.generator_panel_opened"
    const val STORAGE_OPENED = "chapter_01.storage_opened"
    const val RADIO_PANEL_OPENED = "chapter_01.radio_panel_opened"
    const val FIRST_BUNKER_EXIT = "chapter_01.first_bunker_exit"
    const val DINING_INSPECTED = "chapter_01.dining_inspected"
    const val MEDICAL_INSPECTED = "chapter_01.medical_inspected"
    const val BED_INSPECTED = "chapter_01.bed_inspected"
    const val LIBRARY_INSPECTED = "chapter_01.library_inspected"
    const val LIBRARY_NOTES_READ = "chapter_01.library_notes_read"
    const val RADIO_NOTES_CROSS_CHECKED = "chapter_01.radio_notes_cross_checked"
    const val WORKSHOP_INSPECTED = "chapter_01.workshop_inspected"

    val all: Set<String> = setOf(
        GENERATOR_PANEL_OPENED,
        STORAGE_OPENED,
        RADIO_PANEL_OPENED,
        FIRST_BUNKER_EXIT,
        DINING_INSPECTED,
        MEDICAL_INSPECTED,
        BED_INSPECTED,
        LIBRARY_INSPECTED,
        LIBRARY_NOTES_READ,
        RADIO_NOTES_CROSS_CHECKED,
        WORKSHOP_INSPECTED
    )
}

data class StoryObjective(
    val id: String,
    val title: String,
    val description: String,
    val eventId: String
)

data class StoryObservation(val eventId: String, val title: String, val note: String)

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


    fun observations(): List<StoryObservation> {
        val notes = listOf(
            StoryObservation(StoryEventIds.GENERATOR_PANEL_OPENED, "Генераторная", "На панели видны следы недавней проверки. Кто-то обслуживал оборудование совсем недавно."),
            StoryObservation(StoryEventIds.STORAGE_OPENED, "Хранилище", "Запасы разложены по категориям, на контейнерах есть свежие отметки учёта."),
            StoryObservation(StoryEventIds.RADIO_PANEL_OPENED, "Радиостанция", "Корпус радиостанции тёплый, а на шкале частот заметна полоска помех."),
            StoryObservation(StoryEventIds.DINING_INSPECTED, "Столовая", "На столе закреплён листок с нормами выдачи воды и продуктов. Последняя запись сделана не так давно."),
            StoryObservation(StoryEventIds.MEDICAL_INSPECTED, "Медчасть", "Инструменты разложены по местам. На шкафу висит карточка проверки запасов, но имя ответственного стёрлось."),
            StoryObservation(StoryEventIds.BED_INSPECTED, "Спальня", "На тумбе лежит блокнот с короткой пометкой: «Не забыть проверить связь»."),
            StoryObservation(StoryEventIds.LIBRARY_INSPECTED, "Библиотека", "На полях нескольких справочников есть карандашные пометки."),
            StoryObservation(StoryEventIds.LIBRARY_NOTES_READ, "Пометки в справочнике", "В пометках повторяются короткие указания: сверять показания приборов перед выходом и не полагаться на один источник связи. Рядом несколько раз написано слово «контур»."),
            StoryObservation(StoryEventIds.WORKSHOP_INSPECTED, "Мастерская", "На верстаке оставлены измерительный инструмент и список мелких ремонтных работ."),
            StoryObservation(StoryEventIds.RADIO_NOTES_CROSS_CHECKED, "Сверка записей и эфира", "Андрей сопоставляет пометки с поведением радиостанции. Записи не объясняют помехи, но дают понять: перед выходом нельзя полагаться только на радиосвязь.")
        )
        return notes.filter { hasEvent(it.eventId) }
    }

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
