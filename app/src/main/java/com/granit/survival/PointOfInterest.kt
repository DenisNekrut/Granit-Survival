package com.granit.survival

data class PointOfInterest(
    val id: String,
    val zone: WorldZone,
    val x: Float,
    val y: Float,
    val title: String,
    val description: String,
    val hudLabel: String,
    val spriteId: String = "crate",
    val discoveryRadius: Float = 1.15f,
    val cacheTier: WorldLootTier? = null,
    /** Optional story gate for landmarks that should appear only after a clue is found. */
    val requiredStoryEventId: String? = null
)

/** Authored landmarks; discovery state is stored separately in save/checkpoint snapshots. */
object PointOfInterestCatalog {
    private val entries = listOf(
        PointOfInterest("forest_watch_post", WorldZone.FOREST, 12.5f, 14.5f, "Старая лесная вышка", "На стойках ещё висит выцветшая схема лесных просек.", "WATCH POST", spriteId = "road_sign"),
        PointOfInterest("forest_camp", WorldZone.FOREST, 34.5f, 19.5f, "Пустой привал", "Кострище давно остыло. Рядом остались следы чужого лагеря.", "EMPTY CAMP", spriteId = "fallen_log_mossy", cacheTier = WorldLootTier.PROVISIONS),
        PointOfInterest("forest_radio_cache", WorldZone.FOREST, 5.5f, 12.5f, "Сломанный ретранслятор", "На корпусе сохранилась частота старой аварийной сети.", "OLD RELAY", spriteId = "radio", cacheTier = WorldLootTier.MATERIALS),
        PointOfInterest("forest_supply_crate", WorldZone.FOREST, 30.5f, 5.5f, "Лесной тайник", "Под корнями спрятан пустой ящик с маркировкой экспедиции.", "HIDDEN CRATE", spriteId = "crate_weathered"),
        PointOfInterest("ruins_checkpoint", WorldZone.RUINS, 5.5f, 13.5f, "Разбитый пост", "У стены сохранилась табличка с тем же условным знаком, что и на карте из брошенного автомобиля.", "OLD CHECKPOINT", spriteId = "road_barrier_faded", requiredStoryEventId = StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND),
        PointOfInterest("ruins_outer_route", WorldZone.RUINS, 10.5f, 22.5f, "Указатель линии связи", "За постом сохранился указатель старой линии связи. Его маркировка совпадает с дополнительной отметкой на табличке.", "OUTER ROUTE", spriteId = "road_sign", requiredStoryEventId = StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED),
        PointOfInterest("ruins_service_access", WorldZone.RUINS, 19.5f, 21.5f, "Служебный вход", "В конце технического проезда находится закрытый вход в старый коммуникационный узел.", "SERVICE ACCESS", spriteId = "bunker_panel", requiredStoryEventId = StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED),
        PointOfInterest("ruins_power_junction", WorldZone.RUINS, 24.5f, 21.5f, "Распределительная коробка", "У дальней стены находится коробка питания служебного входа.", "POWER JUNCTION", spriteId = "bunker_panel", requiredStoryEventId = StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED),
        PointOfInterest("communication_node_console", WorldZone.COMMUNICATION_NODE, 8.5f, 5.5f, "Пульт линии связи", "Внутри узла сохранился пульт с работающим индикатором линии.", "COMMUNICATION CONSOLE", spriteId = "radio", requiredStoryEventId = StoryEventIds.COMMUNICATION_NODE_ENTERED),
        PointOfInterest("ruins_shelter", WorldZone.RUINS, 25.5f, 19.5f, "Подвальное укрытие", "Вход завален, но на двери заметны свежие отметки.", "HIDDEN SHELTER", spriteId = "rubble", cacheTier = WorldLootTier.MEDICAL),
        PointOfInterest("ruins_archive", WorldZone.RUINS, 20.5f, 5.5f, "Архивный коридор", "На полу разбросаны папки с планами старого жилого квартала.", "OLD ARCHIVE", spriteId = "bookshelf"),
        PointOfInterest("ruins_generator_room", WorldZone.RUINS, 33.5f, 5.5f, "Техническая комната", "На стене осталась схема автономного электроснабжения.", "SERVICE ROOM", spriteId = "generator", cacheTier = WorldLootTier.MATERIALS),
        PointOfInterest("road_vehicle", WorldZone.ROAD, 12.5f, 12.5f, "Брошенный автомобиль", "В салоне лежит карта с пометками вдоль дороги.", "ABANDONED CAR", spriteId = "wreck", cacheTier = WorldLootTier.RARE),
        PointOfInterest("road_milestone", WorldZone.ROAD, 25.5f, 10.5f, "Старый указатель", "Надписи почти стёрлись, но направление на город ещё читается.", "OLD SIGNPOST", spriteId = "road_sign"),
        PointOfInterest("road_bus_stop", WorldZone.ROAD, 18.5f, 19.5f, "Автобусная остановка", "Расписание всё ещё висит под стеклом, хотя маршруты давно не ходят.", "BUS STOP", spriteId = "road_sign_faded"),
        PointOfInterest("road_checkpoint", WorldZone.ROAD, 34.5f, 14.5f, "Дорожный пост", "Пустой пост контроля перекрывает старый съезд с трассы.", "ROAD POST", spriteId = "road_barrier"),
        PointOfInterest("road_line_08_marker", WorldZone.ROAD, 29.5f, 7.5f, "Метка резервной линии 08", "На дорожной опоре сохранилась маркировка, совпадающая с архивным маршрутным листом.", "LINE 08 MARKER", spriteId = "road_sign", requiredStoryEventId = StoryEventIds.COMMUNICATION_NODE_ROUTE_RECORD_FOUND)
    )

    fun all(): List<PointOfInterest> = entries
    fun inZone(zone: WorldZone): List<PointOfInterest> = entries.filter { it.zone == zone }
    fun byId(id: String): PointOfInterest? = entries.firstOrNull { it.id == id }
}
