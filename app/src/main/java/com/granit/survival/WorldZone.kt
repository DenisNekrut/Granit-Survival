package com.granit.survival

data class ZoneResourceSnapshot(val zone: WorldZone, val resources: List<ResourceLocation>)

enum class WorldZone(val displayName: String, val spawnX: Float, val spawnY: Float) {
    BUNKER("Убежище", 9.5f, 7.0f),
    FOREST("Лес", 20.5f, 12.5f),
    RUINS("Руины", 34.5f, 12.5f),
    ROAD("Дорога", 5.5f, 12.5f)
}
