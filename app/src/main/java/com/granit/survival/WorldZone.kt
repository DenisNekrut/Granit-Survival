package com.granit.survival

data class ZoneResourceSnapshot(
    val zone: WorldZone,
    val resources: List<ResourceLocation>,
    /** Seed for deterministic reconstruction of a procedural map; null for fixed maps. */
    val seed: Long? = null
)

enum class WorldZone(val displayName: String, val spawnX: Float, val spawnY: Float) {
    BUNKER("Убежище", 1.5f, 1.5f),
    FOREST("Лес", 20.5f, 12.5f),
    RUINS("Руины", 34.5f, 12.5f),
    ROAD("Дорога", 5.5f, 12.5f);

    val isProcedurallyGenerated: Boolean
        get() = this == RUINS || this == ROAD
}
