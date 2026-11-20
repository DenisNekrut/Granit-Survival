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
    const val THEORY_CHECK_RADIO_ELSEWHERE = "chapter_01.theory_check_radio_elsewhere"
    const val THEORY_CHECK_EQUIPMENT_FIRST = "chapter_01.theory_check_equipment_first"
    const val RADIO_ELSEWHERE_RELAY_FOUND = "chapter_01.radio_elsewhere_relay_found"
    const val EQUIPMENT_FIRST_RELAY_INSPECTED = "chapter_01.equipment_first_relay_inspected"
    const val FOREST_RELAY_INSPECTED = "chapter_01.forest_relay_inspected"
    const val FOREST_RELAY_REPAIRED = "chapter_01.forest_relay_repaired"
    const val FOREST_WATCH_POST_SIGNAL_CHECKED = "chapter_01.forest_watch_post_signal_checked"
    const val ROAD_MAP_SIGNAL_TRACE_FOUND = "chapter_01.road_map_signal_trace_found"
    const val RUINS_SIGNAL_MARKER_REACHED = "chapter_01.ruins_signal_marker_reached"
    const val RUINS_SIGNAL_MARKER_INSPECTED = "chapter_01.ruins_signal_marker_inspected"
    const val RUINS_OUTER_ROUTE_FOUND = "chapter_01.ruins_outer_route_found"
    const val RUINS_OUTER_ROUTE_INSPECTED = "chapter_01.ruins_outer_route_inspected"
    const val RUINS_SERVICE_ACCESS_FOUND = "chapter_01.ruins_service_access_found"
    const val RUINS_SERVICE_ACCESS_INSPECTED = "chapter_01.ruins_service_access_inspected"
    const val RUINS_POWER_JUNCTION_FOUND = "chapter_01.ruins_power_junction_found"
    const val RUINS_POWER_JUNCTION_INSPECTED = "chapter_01.ruins_power_junction_inspected"
    const val RUINS_SERVICE_POWER_RESTORED = "chapter_01.ruins_service_power_restored"
    const val RUINS_SERVICE_ACCESS_OPENED = "chapter_01.ruins_service_access_opened"
    const val COMMUNICATION_NODE_ENTERED = "chapter_01.communication_node_entered"
    const val COMMUNICATION_NODE_CONSOLE_FOUND = "chapter_01.communication_node_console_found"
    const val COMMUNICATION_NODE_CONSOLE_INSPECTED = "chapter_01.communication_node_console_inspected"
    const val COMMUNICATION_NODE_SHIFT_LOG_READ = "chapter_01.communication_node_shift_log_read"
    const val COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED = "chapter_01.communication_node_signal_sequence_extracted"
    const val COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED = "chapter_01.communication_node_signal_cross_checked"
    const val COMMUNICATION_NODE_ROUTE_RECORD_FOUND = "chapter_01.communication_node_route_record_found"
    const val ROAD_LINE_08_MARKER_FOUND = "chapter_01.road_line_08_marker_found"
    const val ROAD_LINE_08_MARKER_INSPECTED = "chapter_01.road_line_08_marker_inspected"
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
        THEORY_CHECK_RADIO_ELSEWHERE,
        THEORY_CHECK_EQUIPMENT_FIRST,
        RADIO_ELSEWHERE_RELAY_FOUND,
        EQUIPMENT_FIRST_RELAY_INSPECTED,
        FOREST_RELAY_INSPECTED,
        FOREST_RELAY_REPAIRED,
        FOREST_WATCH_POST_SIGNAL_CHECKED,
        ROAD_MAP_SIGNAL_TRACE_FOUND,
        RUINS_SIGNAL_MARKER_REACHED,
        RUINS_SIGNAL_MARKER_INSPECTED,
        RUINS_OUTER_ROUTE_FOUND,
        RUINS_OUTER_ROUTE_INSPECTED,
        RUINS_SERVICE_ACCESS_FOUND,
        RUINS_SERVICE_ACCESS_INSPECTED,
        RUINS_POWER_JUNCTION_FOUND,
        RUINS_POWER_JUNCTION_INSPECTED,
        RUINS_SERVICE_POWER_RESTORED,
        RUINS_SERVICE_ACCESS_OPENED,
        COMMUNICATION_NODE_ENTERED,
        COMMUNICATION_NODE_CONSOLE_FOUND,
        COMMUNICATION_NODE_CONSOLE_INSPECTED,
        COMMUNICATION_NODE_SHIFT_LOG_READ,
        COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED,
        COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED,
        COMMUNICATION_NODE_ROUTE_RECORD_FOUND,
        ROAD_LINE_08_MARKER_FOUND,
        ROAD_LINE_08_MARKER_INSPECTED,
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
            StoryObservation(StoryEventIds.RADIO_NOTES_CROSS_CHECKED, "Сверка записей и эфира", "Андрей сопоставляет пометки с поведением радиостанции. Записи не объясняют помехи, но дают понять: перед выходом нельзя полагаться только на радиосвязь."),
            StoryObservation(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE, "Гипотеза: проверить связь в другом месте", "Андрей решает, что помехи могут быть связаны с местом, где находится убежище. Это лишь рабочая версия — её ещё предстоит проверить."),
            StoryObservation(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST, "Гипотеза: проверить оборудование", "Андрей решает сначала проверить само оборудование и питание радиостанции. Пока нельзя исключить ни неисправность, ни внешнюю причину помех."),
            StoryObservation(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND, "Сигнал за пределами убежища", "У старого ретранслятора Андрей замечает, что характер помех отличается от того, что он слышал в убежище. Это поддерживает мысль проверить связь в других местах, но пока ничего не доказывает."),
            StoryObservation(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED, "Проверка старого ретранслятора", "Андрей осматривает корпус ретранслятора и замечает следы прежнего ремонта. Само устройство давно не обслуживали; по одному внешнему осмотру нельзя понять, связано ли это с помехами."),
            StoryObservation(StoryEventIds.FOREST_RELAY_INSPECTED, "Осмотр ретранслятора", "Андрей внимательно изучил старый ретранслятор и записал состояние устройства. Причина помех пока остаётся неизвестной."),
            StoryObservation(StoryEventIds.FOREST_RELAY_REPAIRED, "Ретранслятор восстановлен", "Андрей восстановил питание и заменил повреждённые соединения. Устройство подало короткий сигнал, но устойчивой связи пока нет. Теперь ретранслятор может стать ориентиром для дальнейшей проверки сигнала."),
            StoryObservation(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED, "Сверка сигнала на лесной вышке", "На старой вышке Андрей проверил направление и уровень сигнала восстановленного ретранслятора. Показания меняются, хотя положение антенны остаётся прежним. Он отмечает это на карте: следующий замер стоит провести уже за пределами леса."),
            StoryObservation(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND, "След сигнала у старой дороги", "В брошенном автомобиле Андрей находит карту с отметками вдоль дороги. Одна из отметок совпадает с направлением, полученным на лесной вышке. Это не доказывает наличие источника сигнала, но даёт конкретную точку для следующей экспедиции."),
            StoryObservation(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED, "Отметка на разбитом посту", "В руинах Андрей находит пост, отмеченный на карте. На табличке сохранился тот же условный знак, что и возле лесной вышки. След сигнала ведёт дальше, но теперь у него есть ещё одна проверяемая точка маршрута."),
            StoryObservation(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED, "Осмотр отметки на посту", "Андрей внимательно сверяет табличку с картой. Под старым знаком видна дополнительная маркировка и направление к следующему участку маршрута. Он переносит её в журнал, не делая выводов о том, кто оставил отметку."),
            StoryObservation(StoryEventIds.RUINS_OUTER_ROUTE_FOUND, "Указатель внешнего маршрута", "За разбитым постом Андрей находит старый указатель линии связи. Его маркировка совпадает с дополнительной отметкой на табличке. Теперь у экспедиции есть новая точка для проверки маршрута."),
            StoryObservation(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED, "Осмотр указателя линии связи", "На обратной стороне указателя сохранилась схема старого технического проезда. Андрей отмечает её на карте: маршрут ведёт к дальней окраине руин, где можно продолжить обследование."),
            StoryObservation(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND, "Служебный вход на окраине", "В конце технического проезда Андрей обнаруживает служебный вход в старый коммуникационный узел. На двери сохранилась маркировка, совпадающая со схемой указателя. Вход пока закрыт, но теперь известно, где продолжить обследование."),
            StoryObservation(StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED, "Осмотр служебного входа", "Дверь заперта электромеханическим замком. Рядом сохранился небольшой сервисный отсек: питание на замок не поступает, а кабель уходит под бетон к распределительной коробке. Андрей отмечает её расположение. Дверь пока не открыть."),
            StoryObservation(StoryEventIds.RUINS_POWER_JUNCTION_FOUND, "Распределительная коробка", "У дальней стены Андрей находит коробку, к которой подходит кабель от служебного входа. На крышке сохранилась маркировка той же линии питания."),
            StoryObservation(StoryEventIds.RUINS_POWER_JUNCTION_INSPECTED, "Осмотр распределительной коробки", "Внутри обнаружен отключённый участок линии и повреждённый контакт. По внешним обозначениям понятно, что питание замка можно восстановить после замены повреждённого соединения."),
            StoryObservation(StoryEventIds.RUINS_SERVICE_POWER_RESTORED, "Питание служебного входа восстановлено", "После ремонта линия снова подаёт питание. На двери загорается индикатор замка, но механизм ещё не разблокирован."),
            StoryObservation(StoryEventIds.RUINS_SERVICE_ACCESS_OPENED, "Служебный вход открыт", "Электромеханический замок срабатывает. Дверь коммуникационного узла теперь можно открыть; за ней начинается тёмный внутренний коридор."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_ENTERED, "Вход в коммуникационный узел", "Внутри прохладно и почти не слышно улицы. На стенах сохранились кабельные каналы, а в глубине помещения виден пульт с тусклым индикатором."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_CONSOLE_FOUND, "Пульт коммуникационного узла", "На пульте горит индикатор линии. Рядом лежит журнал дежурной смены, но последние записи обрываются на середине строки."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_CONSOLE_INSPECTED, "Осмотр пульта связи", "Пульт не выключен: индикатор линии горит ровным светом, а на экране отображается служебный статус «GRANIT / ЛИНИЯ 08». При этом ни одного подтверждения от внешнего абонента нет. Андрей не может понять, что именно поддерживает работу оборудования."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_SHIFT_LOG_READ, "Журнал дежурной смены", "В сохранившейся части журнала есть три отметки: «21:40 — линия GRANIT отвечает в штатном режиме», «21:47 — контрольный сигнал принят повторно», «21:52 — время на панели не совпадает с часами поста». Следующая строка обрывается. Причина расхождения неизвестна."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_SIGNAL_SEQUENCE_EXTRACTED, "Контрольная последовательность", "На пульте сохранилась последовательность контрольных отметок: 21:40, 21:47 и 21:52. Интервалы между ними не совпадают с системным временем экрана. Андрей сохраняет значения для последующей сверки с сигналом из других точек маршрута."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_SIGNAL_CROSS_CHECKED, "Сверка контрольных сигналов", "Андрей сопоставляет записи узла с показаниями лесной вышки. На вышке уровень сигнала менялся при неподвижной антенне; в журнале узла контрольные отметки разделены интервалами в семь и пять минут. Связь между этими наблюдениями не доказана, но Андрей сохраняет их как повод для следующей проверки."),
            StoryObservation(StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND, "Маршрутный лист линии 08", "В нижнем ящике пульта Андрей находит маршрутный лист. В нём указано, что линия 08 проходила через удалённый контрольный пункт, обозначенный только как «узел Б». Точные координаты в копии отсутствуют; сохранилось направление от старой дороги и пометка о резервной трассе. Это новая цель для исследования, но пока не доказательство того, что пункт продолжает работать."),
            StoryObservation(StoryEventIds.ROAD_LINE_08_MARKER_FOUND, "Маркировка резервной трассы", "На старой дороге Андрей находит отдельную метку линии 08 у дорожной опоры. Её номер совпадает с архивным листом, а стрелка указывает на просёлочный участок, которого нет на обычной схеме. Маркировка подтверждает только существование маршрута — не то, что линия сейчас действует."),
            StoryObservation(StoryEventIds.ROAD_LINE_08_MARKER_INSPECTED, "Осмотр метки линии 08", "Под слоем грязи Андрей обнаруживает вторую, более старую отметку и условный знак ответвления. Он переносит его в карту и отмечает направление к следующему участку. До проверки на месте нельзя сказать, сохранился ли сам проезд."),
        )
        return notes.filter { hasEvent(it.eventId) }
    }

    /** Records one mutually exclusive, player-authored working theory after the clues are cross-checked. */
    fun chooseFirstTheory(eventId: String): Boolean {
        if (!hasEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)) return false
        if (eventId != StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE &&
            eventId != StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST) return false
        val other = if (eventId == StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE) {
            StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST
        } else {
            StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE
        }
        if (hasEvent(other)) return false
        return recordEvent(eventId)
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
