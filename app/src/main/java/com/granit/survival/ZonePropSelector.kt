package com.granit.survival

/**
 * Sparse, deterministic environmental dressing.
 *
 * Coordinate hashing keeps every prop stable across visits and save restoration.
 * Zone-specific variants add visual identity without filling walkable space.
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
                hash in 12..14 -> "floor_grate"
                else -> null
            }
            WorldZone.FOREST -> when {
                hash < 5 -> "bush"
                hash in 15..18 -> "grass_patch"
                hash in 37..40 -> "tree"
                hash in 72..74 -> "fallen_log"
                else -> null
            }
            WorldZone.RUINS -> when {
                hash < 4 -> "barrel"
                hash in 24..29 -> "debris"
                hash in 46..49 -> "metal_scrap"
                hash in 80..82 -> "puddle"
                else -> null
            }
            WorldZone.ROAD -> when {
                hash < 3 -> "streetlamp"
                hash in 20..23 -> "road_sign"
                hash in 42..45 -> "wreck"
                hash in 70..72 -> "puddle"
                else -> null
            }
        }
    }
}
