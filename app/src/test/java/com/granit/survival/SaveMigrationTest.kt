package com.granit.survival

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveMigrationTest {
    @Test
    fun migratesLegacyAndPartiallyMissingFieldsToCurrentSchema() {
        val legacy = JSONObject()
            .put("saveVersion", 1)
            .put("savedAtMillis", 1234L)
            .put("state", JSONObject()
                .put("player", JSONObject().put("x", 4.5).put("y", 6.5))
                .put("needs", JSONObject().put("health", 75))
                .put("clock", JSONObject().put("day", 3))
                .put("inventory", JSONArray()
                    .put(JSONObject().put("type", ItemType.WOOD.name).put("quantity", 2))
                    .put(JSONObject().put("type", "UNKNOWN_ITEM").put("quantity", 9))
                    .put(JSONObject().put("type", ItemType.METAL.name).put("quantity", 99))))

        val decoded = SaveCodec.decode(legacy.toString())
        val state = decoded.getJSONObject("state")

        assertEquals(SaveVersion.CURRENT, decoded.getInt("saveVersion"))
        assertEquals(4.5, state.getJSONObject("player").getDouble("x"), 0.001)
        assertEquals(4.2, state.getJSONObject("player").getDouble("speed"), 0.001)
        assertEquals(75.0, state.getJSONObject("needs").getDouble("health"), 0.001)
        assertEquals(100.0, state.getJSONObject("needs").getDouble("hunger"), 0.001)
        assertEquals(3, state.getJSONObject("clock").getInt("day"))
        assertEquals(420, state.getJSONObject("clock").getInt("minuteOfDay"))
        val migratedInventory = state.getJSONArray("inventory")
        assertEquals(16, migratedInventory.length())
        assertEquals(ItemType.WOOD.name, migratedInventory.getJSONObject(0).getString("type"))
        assertTrue(migratedInventory.isNull(1))
        assertEquals(20, migratedInventory.getJSONObject(2).getInt("quantity"))
        assertEquals(24, state.getJSONArray("bunkerStorage").length())
        assertEquals(1, legacy.getInt("saveVersion")) // migration never mutates the source object
    }


    @Test
    fun clampsPlayerCoordinatesToTheRestoredZoneBounds() {
        val damaged = JSONObject()
            .put("saveVersion", 9)
            .put("state", JSONObject()
                .put("player", JSONObject().put("x", 999).put("y", -20))
                .put("clock", JSONObject()))
        val player = SaveCodec.decode(damaged.toString())
            .getJSONObject("state").getJSONObject("player")
        assertEquals(16.35, player.getDouble("x"), 0.001)
        assertEquals(0.8, player.getDouble("y"), 0.001)
    }

    @Test
    fun rejectsSavesFromAFutureSchema() {
        val future = JSONObject()
            .put("saveVersion", SaveVersion.CURRENT + 1)
            .put("state", JSONObject())

        assertThrows(IllegalArgumentException::class.java) {
            SaveCodec.decode(future.toString())
        }
    }

    @Test
    fun rejectsMissingSaveEnvelopeInsteadOfSilentlyStartingANewGame() {
        val missingState = JSONObject().put("saveVersion", 1)
        assertThrows(IllegalArgumentException::class.java) {
            SaveCodec.decode(missingState.toString())
        }
    }

    @Test
    fun normalizesInvalidZoneAndOutOfRangeNeeds() {
        val damaged = JSONObject()
            .put("saveVersion", 9)
            .put("state", JSONObject()
                .put("zone", "NOT_A_ZONE")
                .put("needs", JSONObject().put("health", 250).put("hunger", -10))
                .put("clock", JSONObject()))

        val state = SaveCodec.decode(damaged.toString()).getJSONObject("state")
        assertEquals(WorldZone.BUNKER.name, state.getString("zone"))
        assertEquals(100.0, state.getJSONObject("needs").getDouble("health"), 0.001)
        assertEquals(0.0, state.getJSONObject("needs").getDouble("hunger"), 0.001)
    }
}
