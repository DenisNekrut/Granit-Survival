package com.granit.survival

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class EngineSaveSnapshot(
    val player: PlayerSnapshot,
    val inventory: List<ItemStack?>,
    val bunkerStorage: List<ItemStack?>,
    val needs: SurvivalNeedsSnapshot,
    val clock: WorldClockSnapshot,
    val remainingResources: List<ResourceLocation>,
    val zone: WorldZone = WorldZone.BUNKER,
    val wakeCheckpoint: WakeCheckpoint?
) {
    companion object
}

data class SaveSlotInfo(
    val slot: Int,
    val exists: Boolean,
    val savedAtMillis: Long? = null,
    val day: Int? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val corrupted: Boolean = false
)

/**
 * Versioned JSON save repository. AtomicFile prevents a process interruption
 * from leaving a half-written save as the only copy.
 */
class GameSaveRepository(context: Context) {
    private val saveDirectory = File(context.filesDir, "saves").apply { mkdirs() }

    fun listSlots(): List<SaveSlotInfo> = (1..MAX_SLOTS).map { slot ->
        val file = saveFile(slot)
        if (!file.exists()) return@map SaveSlotInfo(slot = slot, exists = false)
        try {
            val root = JSONObject(AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
            require(root.getInt("saveVersion") in 1..SAVE_VERSION)
            val clock = root.getJSONObject("state").getJSONObject("clock")
            SaveSlotInfo(
                slot = slot,
                exists = true,
                savedAtMillis = root.getLong("savedAtMillis"),
                day = clock.getInt("day"),
                hour = clock.getInt("minuteOfDay") / 60,
                minute = clock.getInt("minuteOfDay") % 60
            )
        } catch (_: Exception) {
            SaveSlotInfo(slot = slot, exists = true, corrupted = true)
        }
    }

    @Throws(IOException::class)
    fun save(slot: Int, engine: GameEngine): SaveSlotInfo {
        writeSnapshot(saveFile(slot), engine)
        return listSlots().first { it.slot == slot }
    }

    /** Dedicated checkpoint file; it never consumes or overwrites a manual slot. */
    @Throws(IOException::class)
    fun saveAuto(engine: GameEngine) = writeSnapshot(autoSaveFile(), engine)

    fun autoSaveTimestamp(): Long? = runCatching {
        val file = autoSaveFile()
        if (!file.exists()) null else JSONObject(
            AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        ).getLong("savedAtMillis")
    }.getOrNull()

    fun loadAuto(engine: GameEngine): Boolean = loadFromFile(autoSaveFile(), engine)

    fun load(slot: Int, engine: GameEngine): Boolean = loadFromFile(saveFile(slot), engine)

    private fun writeSnapshot(target: File, engine: GameEngine) {
        val root = JSONObject()
            .put("saveVersion", SAVE_VERSION)
            .put("savedAtMillis", System.currentTimeMillis())
            .put("state", engine.createSaveSnapshot().toJson())
        val atomicFile = AtomicFile(target)
        var stream: java.io.FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
            stream = null
        } catch (failure: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            throw IOException("Unable to write save file", failure)
        }
    }

    private fun loadFromFile(target: File, engine: GameEngine): Boolean {
        if (!target.exists()) return false
        val root = JSONObject(AtomicFile(target).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        require(root.getInt("saveVersion") == SAVE_VERSION) {
            "Unsupported save version: ${root.optInt("saveVersion", -1)}"
        }
        engine.restoreSaveSnapshot(EngineSaveSnapshot.fromJson(root.getJSONObject("state")))
        return true
    }

    fun delete(slot: Int): Boolean {
        val file = saveFile(slot)
        if (!file.exists()) return false
        return AtomicFile(file).delete().let { !file.exists() }
    }

    private fun saveFile(slot: Int): File {
        require(slot in 1..MAX_SLOTS) { "Save slot must be between 1 and $MAX_SLOTS" }
        return File(saveDirectory, "slot_$slot.json")
    }

    private fun autoSaveFile() = File(saveDirectory, "autosave.json")

    companion object {
        const val MAX_SLOTS = 3
        const val SAVE_VERSION = 2
    }
}

private fun EngineSaveSnapshot.toJson(): JSONObject = JSONObject()
    .put("player", player.toJson())
    .put("inventory", inventory.toItemStacksJson())
    .put("bunkerStorage", bunkerStorage.toItemStacksJson())
    .put("needs", needs.toJson())
    .put("clock", clock.toJson())
    .put("remainingResources", remainingResources.toResourcesJson())
    .put("zone", zone.name)
    .put("wakeCheckpoint", wakeCheckpoint?.toJson() ?: JSONObject.NULL)

private fun PlayerSnapshot.toJson() = JSONObject()
    .put("x", x.toDouble()).put("y", y.toDouble()).put("speed", speed.toDouble())

private fun SurvivalNeedsSnapshot.toJson() = JSONObject()
    .put("health", health.toDouble())
    .put("hunger", hunger.toDouble())
    .put("thirst", thirst.toDouble())
    .put("energy", energy.toDouble())

private fun WorldClockSnapshot.toJson() = JSONObject()
    .put("day", day).put("minuteOfDay", minuteOfDay).put("totalMinutes", totalMinutes)

private fun List<ItemStack?>.toItemStacksJson(): JSONArray = JSONArray().also { array ->
    forEach { stack ->
        array.put(if (stack == null) JSONObject.NULL else JSONObject()
            .put("type", stack.type.name)
            .put("quantity", stack.quantity))
    }
}

private fun List<ResourceLocation>.toResourcesJson(): JSONArray = JSONArray().also { array ->
    forEach { resource ->
        array.put(JSONObject()
            .put("x", resource.x)
            .put("y", resource.y)
            .put("type", resource.type.name))
    }
}

private fun WakeCheckpoint.toJson() = JSONObject()
    .put("player", player.toJson())
    .put("inventory", inventory.toItemStacksJson())
    .put("bunkerStorage", bunkerStorage.toItemStacksJson())
    .put("needs", needs.toJson())
    .put("clock", clock.toJson())
    .put("remainingResources", remainingResources.toResourcesJson())
    .put("zone", zone.name)

private fun EngineSaveSnapshot.Companion.fromJson(json: JSONObject): EngineSaveSnapshot {
    val checkpointJson = json.optJSONObject("wakeCheckpoint")
    return EngineSaveSnapshot(
        player = json.getJSONObject("player").toPlayerSnapshot(),
        inventory = json.getJSONArray("inventory").toItemStacks(),
        bunkerStorage = json.getJSONArray("bunkerStorage").toItemStacks(),
        needs = json.getJSONObject("needs").toNeedsSnapshot(),
        clock = json.getJSONObject("clock").toClockSnapshot(),
        remainingResources = json.getJSONArray("remainingResources").toResources(),
        zone = runCatching { WorldZone.valueOf(json.optString("zone", WorldZone.BUNKER.name)) }.getOrDefault(WorldZone.BUNKER),
        wakeCheckpoint = checkpointJson?.toWakeCheckpoint()
    )
}

private fun JSONObject.toPlayerSnapshot() = PlayerSnapshot(
    x = getDouble("x").toFloat(),
    y = getDouble("y").toFloat(),
    speed = getDouble("speed").toFloat()
)

private fun JSONObject.toNeedsSnapshot() = SurvivalNeedsSnapshot(
    health = getDouble("health").toFloat(),
    hunger = getDouble("hunger").toFloat(),
    thirst = getDouble("thirst").toFloat(),
    energy = getDouble("energy").toFloat()
)

private fun JSONObject.toClockSnapshot() = WorldClockSnapshot(
    day = getInt("day"),
    minuteOfDay = getInt("minuteOfDay"),
    totalMinutes = getLong("totalMinutes")
)

private fun JSONArray.toItemStacks(): List<ItemStack?> = List(length()) { index ->
    if (isNull(index)) null else getJSONObject(index).let { stack ->
        ItemStack(ItemType.valueOf(stack.getString("type")), stack.getInt("quantity"))
    }
}

private fun JSONArray.toResources(): List<ResourceLocation> = List(length()) { index ->
    getJSONObject(index).let { resource ->
        ResourceLocation(
            resource.getInt("x"),
            resource.getInt("y"),
            ItemType.valueOf(resource.getString("type"))
        )
    }
}

private fun JSONObject.toWakeCheckpoint() = WakeCheckpoint(
    player = getJSONObject("player").toPlayerSnapshot(),
    inventory = getJSONArray("inventory").toItemStacks(),
    bunkerStorage = getJSONArray("bunkerStorage").toItemStacks(),
    needs = getJSONObject("needs").toNeedsSnapshot(),
    clock = getJSONObject("clock").toClockSnapshot(),
    remainingResources = getJSONArray("remainingResources").toResources(),
    zone = runCatching { WorldZone.valueOf(optString("zone", WorldZone.BUNKER.name)) }.getOrDefault(WorldZone.BUNKER)
)
