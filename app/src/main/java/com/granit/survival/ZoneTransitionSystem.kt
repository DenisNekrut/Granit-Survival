package com.granit.survival

/** Pure zone-transition rules shared by travel, loading and arrival placement. */
internal object ZoneTransitionSystem {
    fun arrivalExitFor(from: WorldZone, target: WorldZone): TileType? = when (target) {
        WorldZone.BUNKER -> if (from != WorldZone.BUNKER) TileType.EXIT_TO_FOREST else null
        WorldZone.COMMUNICATION_NODE -> if (from == WorldZone.RUINS) TileType.EXIT_TO_RUINS else null
        WorldZone.FOREST -> when (from) {
            WorldZone.BUNKER -> TileType.EXIT_TO_BUNKER
            WorldZone.RUINS -> TileType.EXIT_TO_RUINS
            WorldZone.ROAD -> TileType.EXIT_TO_ROAD
            WorldZone.COMMUNICATION_NODE -> TileType.EXIT_TO_COMMUNICATION_NODE
            WorldZone.FOREST -> null
        }
        WorldZone.RUINS, WorldZone.ROAD ->
            if (from == WorldZone.FOREST) TileType.EXIT_TO_FOREST else null
    }

    fun shouldGenerateFreshMap(target: WorldZone, regenerate: Boolean): Boolean =
        regenerate && target.isProcedurallyGenerated
}
