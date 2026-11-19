package com.granit.survival

/**
 * Sparse, deterministic environmental dressing.
 *
 * A coordinate hash provides spatial variety without runtime randomness, so the same
 * location looks identical after changing zones or loading a save. Low spawn rates
 * prevent the map from becoming a wall of repeated props.
 */
object ZonePropSelector {
    fun spriteFor(zone: WorldZone, tileX: Int, tileY: Int): String? {
        val hash = Math.floorMod(
            (tileX * 73_856_093) xor (tileY * 19_349_663) xor (zone.ordinal * 83_492_791),
            101
        )
        return when (zone) {
            WorldZone.BUNKER -> when {
                hash < 4 -> "crate"
                else -> null
            }
            WorldZone.FOREST -> when {
                hash < 5 -> "bush"
                hash in 37..40 -> "tree"
                else -> null
            }
            WorldZone.RUINS -> when {
                hash < 4 -> "barrel"
                hash in 24..29 -> "debris"
                else -> null
            }
            WorldZone.ROAD -> when {
                hash < 3 -> "streetlamp"
                hash in 42..45 -> "wreck"
                else -> null
            }
        }
    }
}
