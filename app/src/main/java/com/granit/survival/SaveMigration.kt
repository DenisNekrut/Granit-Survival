package com.granit.survival

import org.json.JSONArray
import org.json.JSONObject

/**
 * Normalizes older and partially incomplete saves before model decoding.
 * Migration works on a copy; the source file is never modified by this class.
 */
internal object SaveMigration {
    private const val BACKPACK_SLOTS = 16
    private const val STORAGE_SLOTS = 24
    private const val STACK_LIMIT = 20

    fun migrate(source: JSONObject): JSONObject {
        val version = source.optInt("saveVersion", -1)
        require(SaveVersion.isSupported(version)) {
            "Unsupported save version: $version (current ${SaveVersion.CURRENT})"
        }
        val root = JSONObject(source.toString())
        val state = root.optJSONObject("state")
            ?: throw IllegalArgumentException("Save is missing its state object")
        normalizeSnapshot(state)
        root.put("state", state)
        root.put("saveVersion", SaveVersion.CURRENT)
        root.put("savedAtMillis", root.optLong("savedAtMillis", 0L).coerceAtLeast(0L))
        return root
    }

    private fun normalizeSnapshot(state: JSONObject) {
        val player = state.optJSONObject("player") ?: JSONObject()
        player.put("x", finite(player.optDouble("x", 8.5), 8.5))
        player.put("y", finite(player.optDouble("y", 2.5), 2.5))
        player.put("speed", finite(player.optDouble("speed", 4.2), 4.2).coerceIn(0.1, 20.0))
        state.put("player", player)

        val needs = state.optJSONObject("needs") ?: JSONObject()
        needs.put("health", finite(needs.optDouble("health", 100.0), 100.0).coerceIn(0.0, 100.0))
        needs.put("hunger", finite(needs.optDouble("hunger", 100.0), 100.0).coerceIn(0.0, 100.0))
        needs.put("thirst", finite(needs.optDouble("thirst", 100.0), 100.0).coerceIn(0.0, 100.0))
        needs.put("energy", finite(needs.optDouble("energy", 100.0), 100.0).coerceIn(0.0, 100.0))
        state.put("needs", needs)

        val clock = state.optJSONObject("clock") ?: JSONObject()
        clock.put("day", clock.optInt("day", 1).coerceAtLeast(1))
        clock.put("minuteOfDay", clock.optInt("minuteOfDay", 420).coerceIn(0, 1439))
        clock.put("totalMinutes", clock.optLong("totalMinutes", 420L).coerceAtLeast(0L))
        state.put("clock", clock)

        state.put("inventory", validItemStacks(state.optJSONArray("inventory"), BACKPACK_SLOTS))
        state.put("bunkerStorage", validItemStacks(state.optJSONArray("bunkerStorage"), STORAGE_SLOTS))
        state.put("remainingResources", validResources(state.optJSONArray("remainingResources")))

        val zone = state.optString("zone", WorldZone.BUNKER.name)
            .takeIf { candidate -> runCatching { WorldZone.valueOf(candidate) }.isSuccess }
            ?: WorldZone.BUNKER.name
        state.put("zone", zone)
        val mapWidth = if (zone == WorldZone.BUNKER.name) 17.0 else GameEngine.WORLD_WIDTH.toDouble()
        val mapHeight = if (zone == WorldZone.BUNKER.name) 10.0 else GameEngine.WORLD_HEIGHT.toDouble()
        player.put("x", player.optDouble("x", 8.5).coerceIn(0.65, mapWidth - 0.65))
        player.put("y", player.optDouble("y", 2.5).coerceIn(0.8, mapHeight - 0.8))

        if (state.optJSONObject("psychology") == null) state.put("psychology", JSONObject())
        if (state.optJSONObject("bookLibrary") == null) state.put("bookLibrary", JSONObject())
        if (state.optJSONObject("radio") == null) state.put("radio", JSONObject())
        if (state.optJSONObject("equipment") == null) state.put("equipment", JSONObject())
        if (state.optJSONObject("statistics") == null) state.put("statistics", JSONObject())
        if (state.optJSONObject("bunkerPower") == null) state.put("bunkerPower", JSONObject())
        state.put("zoneResources", validZoneResources(state.optJSONArray("zoneResources")))
        if (state.optJSONArray("discoveredPoiIds") == null) state.put("discoveredPoiIds", JSONArray())

        val checkpoint = state.optJSONObject("wakeCheckpoint")
        if (checkpoint != null) {
            runCatching { normalizeSnapshot(checkpoint) }
                .onFailure { state.put("wakeCheckpoint", JSONObject.NULL) }
        } else if (!state.has("wakeCheckpoint")) {
            state.put("wakeCheckpoint", JSONObject.NULL)
        }
    }

    private fun validItemStacks(source: JSONArray?, slotCount: Int): JSONArray {
        val result = JSONArray()
        for (index in 0 until slotCount) {
            val item = source?.optJSONObject(index)
            if (item == null) {
                result.put(JSONObject.NULL)
                continue
            }
            val type = item.optString("type")
            val quantity = item.optInt("quantity", 0)
            val validType = runCatching { ItemType.valueOf(type) }.isSuccess
            if (validType && quantity > 0) {
                result.put(JSONObject().put("type", type).put("quantity", quantity.coerceAtMost(STACK_LIMIT)))
            } else {
                result.put(JSONObject.NULL)
            }
        }
        return result
    }

    private fun validResources(source: JSONArray?): JSONArray {
        val result = JSONArray()
        if (source == null) return result
        for (index in 0 until source.length()) {
            val resource = source.optJSONObject(index) ?: continue
            val type = resource.optString("type")
            val x = resource.optInt("x", Int.MIN_VALUE)
            val y = resource.optInt("y", Int.MIN_VALUE)
            val validType = runCatching { ItemType.valueOf(type) }.isSuccess
            if (validType && x != Int.MIN_VALUE && y != Int.MIN_VALUE) {
                result.put(JSONObject().put("x", x).put("y", y).put("type", type))
            }
        }
        return result
    }


    private fun validZoneResources(source: JSONArray?): JSONArray {
        val result = JSONArray()
        if (source == null) return result
        for (index in 0 until source.length()) {
            val entry = source.optJSONObject(index) ?: continue
            val zone = entry.optString("zone")
            if (runCatching { WorldZone.valueOf(zone) }.isFailure) continue
            val normalized = JSONObject()
                .put("zone", zone)
                .put("resources", validResources(entry.optJSONArray("resources")))
            if (!entry.isNull("seed") && entry.has("seed")) {
                normalized.put("seed", entry.optLong("seed"))
            } else {
                normalized.put("seed", JSONObject.NULL)
            }
            result.put(normalized)
        }
        return result
    }

    private fun finite(value: Double, fallback: Double): Double =
        if (value.isFinite()) value else fallback
}
