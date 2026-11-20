package com.granit.survival

/**
 * Sparse, deterministic environmental dressing.
 *
 * Coordinate hashing keeps every prop stable across visits and save restoration.
 * Each family has several silhouette/material variants; selection never changes
 * collision or movement tiles.
 */
object ZonePropSelector {
    fun wallSpriteFor(zone: WorldZone, tileX: Int, tileY: Int): String {
        val hash = Math.floorMod(
            (tileX * 73_856_093) xor (tileY * 19_349_663) xor (zone.ordinal * 83_492_791),
            101
        )
        return when (zone) {
            WorldZone.BUNKER -> if (hash % 4 == 0) "bunker_panel_rusted" else "bunker_panel"
            WorldZone.FOREST -> listOf("tree", "tree_dark", "tree_dead")[hash % 3]
            WorldZone.RUINS -> listOf("rubble", "rubble_brick", "debris")[hash % 3]
            WorldZone.ROAD -> if (hash % 3 == 0) "road_barrier_faded" else "road_barrier"
        }
    }

    fun spriteFor(zone: WorldZone, tileX: Int, tileY: Int): String? {
        val hash = Math.floorMod(
            (tileX * 73_856_093) xor (tileY * 19_349_663) xor (zone.ordinal * 83_492_791),
            101
        )
        return when (zone) {
            WorldZone.BUNKER -> when {
                hash < 4 -> listOf("crate", "crate_weathered", "crate_painted")[hash % 3]
                hash in 12..14 -> if (hash % 2 == 0) "floor_grate" else "floor_grate_rust"
                else -> null
            }
            WorldZone.FOREST -> when {
                hash < 4 -> listOf("bush", "bush_dry", "bush_lush")[hash % 3]
                hash in 15..17 -> if (hash % 2 == 0) "grass_patch" else "grass_patch_dry"
                hash in 37..39 -> listOf("tree", "tree_autumn", "tree_dead")[hash % 3]
                hash in 72..73 -> if (hash % 2 == 0) "fallen_log" else "fallen_log_mossy"
                else -> null
            }
            WorldZone.RUINS -> when {
                hash < 3 -> listOf("barrel", "barrel_rust", "barrel_dark")[hash % 3]
                hash in 24..28 -> if (hash % 2 == 0) "debris" else "debris_light"
                hash in 46..48 -> if (hash % 2 == 0) "metal_scrap" else "metal_scrap_copper"
                hash in 80..81 -> if (hash % 2 == 0) "puddle" else "puddle_ice"
                else -> null
            }
            WorldZone.ROAD -> when {
                hash < 3 -> if (hash % 2 == 0) "streetlamp" else "streetlamp_broken"
                hash in 20..22 -> if (hash % 2 == 0) "road_sign" else "road_sign_faded"
                hash in 42..44 -> listOf("wreck", "wreck_rust", "wreck_blue")[hash % 3]
                hash in 70..72 -> if (hash % 2 == 0) "puddle" else "puddle_ice"
                else -> null
            }
        }
    }
}
