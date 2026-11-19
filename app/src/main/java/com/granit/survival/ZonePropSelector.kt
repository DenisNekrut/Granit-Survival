package com.granit.survival

/** Stable decorative prop selection derived from tile coordinates; no save-state required. */
object ZonePropSelector {
    fun spriteFor(zone: WorldZone, tileX: Int, tileY: Int): String? {
        val pattern = Math.floorMod(tileX * 7 + tileY * 11, 13)
        return when (zone) {
            WorldZone.BUNKER -> if (pattern == 0) "crate" else null
            WorldZone.FOREST -> if (pattern == 0) "bush" else null
            WorldZone.RUINS -> when {
                pattern == 0 -> "barrel"
                pattern % 4 == 0 -> "debris"
                else -> null
            }
            WorldZone.ROAD -> when (pattern) {
                0 -> "streetlamp"
                6 -> "wreck"
                else -> null
            }
        }
    }
}
