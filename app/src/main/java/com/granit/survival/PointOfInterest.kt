package com.granit.survival

data class PointOfInterest(
    val id: String,
    val zone: WorldZone,
    val x: Float,
    val y: Float,
    val title: String,
    val description: String,
    val hudLabel: String,
    val discoveryRadius: Float = 1.15f
)

/** Authored landmarks; discovery state is stored separately in save/checkpoint snapshots. */
object PointOfInterestCatalog {
    private val entries = listOf(
        PointOfInterest("forest_watch_post", WorldZone.FOREST, 12.5f, 14.5f, "Старая лесная вышка", "На стойках ещё висит выцветшая схема лесных просек.", "WATCH POST"),
        PointOfInterest("forest_camp", WorldZone.FOREST, 34.5f, 19.5f, "Пустой привал", "Кострище давно остыло. Рядом остались следы чужого лагеря.", "EMPTY CAMP"),
        PointOfInterest("ruins_checkpoint", WorldZone.RUINS, 5.5f, 13.5f, "Разбитый пост", "У стены сохранилась табличка с обозначением старого поста.", "OLD CHECKPOINT"),
        PointOfInterest("ruins_shelter", WorldZone.RUINS, 25.5f, 19.5f, "Подвальное укрытие", "Вход завален, но на двери заметны свежие отметки.", "HIDDEN SHELTER"),
        PointOfInterest("road_vehicle", WorldZone.ROAD, 12.5f, 12.5f, "Брошенный автомобиль", "В салоне лежит карта с пометками вдоль дороги.", "ABANDONED CAR"),
        PointOfInterest("road_milestone", WorldZone.ROAD, 25.5f, 10.5f, "Старый указатель", "Надписи почти стёрлись, но направление на город ещё читается.", "OLD SIGNPOST")
    )

    fun all(): List<PointOfInterest> = entries
    fun inZone(zone: WorldZone): List<PointOfInterest> = entries.filter { it.zone == zone }
    fun byId(id: String): PointOfInterest? = entries.firstOrNull { it.id == id }
}
