package com.granit.survival.save

import com.granit.survival.GameEngine
import com.granit.survival.ItemType
import com.granit.survival.WorldZone

import org.json.JSONObject

/** Structural and range checks applied after migration and before game-state mutation. */
internal object SaveValidator {
    fun validate(root: JSONObject): JSONObject {
        val version = root.optInt("saveVersion", -1)
        require(version == SaveVersion.CURRENT) {
            "Save migration did not produce current schema ${SaveVersion.CURRENT}"
        }
        val state = root.optJSONObject("state")
            ?: throw IllegalArgumentException("Save is missing its state object")
        val player = state.optJSONObject("player")
            ?: throw IllegalArgumentException("Save is missing player data")
        val needs = state.optJSONObject("needs")
            ?: throw IllegalArgumentException("Save is missing needs data")
        val clock = state.optJSONObject("clock")
            ?: throw IllegalArgumentException("Save is missing clock data")

        val playerX = player.optDouble("x", Double.NaN)
        val playerY = player.optDouble("y", Double.NaN)
        val playerSpeed = player.optDouble("speed", Double.NaN)
        val isBunker = state.optString("zone", WorldZone.BUNKER.name) == WorldZone.BUNKER.name
        val maxX = if (isBunker) 16.35 else GameEngine.WORLD_WIDTH.toDouble() - 0.65
        val maxY = if (isBunker) 9.2 else GameEngine.WORLD_HEIGHT.toDouble() - 0.8
        require(playerX.isFinite() && playerX in 0.65..maxX &&
            playerY.isFinite() && playerY in 0.8..maxY &&
            playerSpeed.isFinite() && playerSpeed in 0.1..20.0) {
            "Invalid player coordinates or speed"
        }
        require(listOf("health", "hunger", "thirst", "energy").all {
            needs.optDouble(it, Double.NaN).let { value -> value.isFinite() && value in 0.0..100.0 }
        }) { "Invalid survival needs values" }
        require(clock.optInt("day", 0) >= 1 &&
            clock.optInt("minuteOfDay", -1) in 0..1439 &&
            clock.optLong("totalMinutes", -1L) >= 0L) { "Invalid world clock" }
        return root
    }
}
