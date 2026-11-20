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
    val cacheTier: WorldLootTier? = null
)

/** Authored landmarks; discovery state is stored separately in save/checkpoint snapshots. */
object PointOfInterestCatalog {
    private val entries = listOf(
        PointOfInterest("forest_watch_post", WorldZone.FOREST, 12.5f, 14.5f, "Старая лесная вышка", "На стойках ещё висит выцветшая схема лесных просек.", "WATCH POST", spriteId = "road_sign"),
        PointOfInterest("forest_camp", WorldZone.FOREST, 34.5f, 19.5f, "Пустой привал", "Кострище давно остыло. Рядом остались следы чужого лагеря.", "EMPTY CAMP", spriteId = "fallen_log_mossy", cacheTier = WorldLootTier.PROVISIONS),
        PointOfInterest("forest_radio_cache", WorldZone.FOREST, 5.5f, 12.5f, "Сломанный ретранслятор", "На корпусе сохранилась частота старой аварийной сети.", "OLD RELAY", spriteId = "radio", cacheTier = WorldLootTier.MATERIALS),
        PointOfInterest("forest_supply_crate", WorldZone.FOREST, 30.5f, 5.5f, "Лесной тайник", "Под корнями спрятан пустой ящик с маркировкой экспедиции.", "HIDDEN CRATE", spriteId = "crate_weathered"),
        PointOfInterest("ruins_checkpoint", WorldZone.RUINS, 5.5f, 13.5f, "Разбитый пост", "У стены сохранилась табличка с обозначением старого поста.", "OLD CHECKPOINT", spriteId = "road_barrier_faded"),
        PointOfInterest("ruins_shelter", WorldZone.RUINS, 25.5f, 19.5f, "Подвальное укрытие", "Вход завален, но на двери заметны свежие отметки.", "HIDDEN SHELTER", spriteId = "rubble", cacheTier = WorldLootTier.MEDICAL),
        PointOfInterest("ruins_archive", WorldZone.RUINS, 20.5f, 5.5f, "Архивный коридор", "На полу разбросаны папки с планами старого жилого квартала.", "OLD ARCHIVE", spriteId = "bookshelf"),
        PointOfInterest("ruins_generator_room", WorldZone.RUINS, 33.5f, 5.5f, "Техническая комната", "На стене осталась схема автономного электроснабжения.", "SERVICE ROOM", spriteId = "generator", cacheTier = WorldLootTier.MATERIALS),
        PointOfInterest("road_vehicle", WorldZone.ROAD, 12.5f, 12.5f, "Брошенный автомобиль", "В салоне лежит карта с пометками вдоль дороги.", "ABANDONED CAR", spriteId = "wreck", cacheTier = WorldLootTier.RARE),
        PointOfInterest("road_milestone", WorldZone.ROAD, 25.5f, 10.5f, "Старый указатель", "Надписи почти стёрлись, но направление на город ещё читается.", "OLD SIGNPOST", spriteId = "road_sign"),
        PointOfInterest("road_bus_stop", WorldZone.ROAD, 18.5f, 19.5f, "Автобусная остановка", "Расписание всё ещё висит под стеклом, хотя маршруты давно не ходят.", "BUS STOP", spriteId = "road_sign_faded"),
        PointOfInterest("road_checkpoint", WorldZone.ROAD, 34.5f, 14.5f, "Дорожный пост", "Пустой пост контроля перекрывает старый съезд с трассы.", "ROAD POST", spriteId = "road_barrier")
    )

    fun all(): List<PointOfInterest> = entries
    fun inZone(zone: WorldZone): List<PointOfInterest> = entries.filter { it.zone == zone }
    fun byId(id: String): PointOfInterest? = entries.firstOrNull { it.id == id }
}
