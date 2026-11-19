package com.granit.survival

import android.content.Context
import android.util.AtomicFile
import com.granit.survival.save.SaveCodec
import com.granit.survival.save.SaveVersion
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class EngineSaveSnapshot(
    val player: PlayerSnapshot,
    val inventory: List<ItemStack?>,
    val bunkerStorage: List<ItemStack?>,
    val needs: SurvivalNeedsSnapshot,
    val psychology: PsychologicalStateSnapshot = PsychologicalStateSnapshot(),
    val bookLibrary: BookLibrarySnapshot = BookLibrarySnapshot(),
    val radio: BunkerRadioSnapshot = BunkerRadioSnapshot(),
    val clock: WorldClockSnapshot,
    val remainingResources: List<ResourceLocation>,
    val zone: WorldZone = WorldZone.BUNKER,
    val zoneResources: List<ZoneResourceSnapshot> = emptyList(),
    val discoveredPoiIds: Set<String> = emptySet(),
    val equipment: PlayerEquipmentSnapshot = PlayerEquipmentSnapshot(),
    val statistics: CharacterStatisticsSnapshot = CharacterStatisticsSnapshot(),
    val bunkerPower: BunkerPowerSnapshot = BunkerPowerSnapshot(),
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
            val root = SaveCodec.decode(
                AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
            )
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
        if (!file.exists()) null else SaveCodec.decode(
            AtomicFile(file).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        ).getLong("savedAtMillis")
    }.getOrNull()

    fun loadAuto(engine: GameEngine): Boolean = loadFromFile(autoSaveFile(), engine)

    fun load(slot: Int, engine: GameEngine): Boolean = loadFromFile(saveFile(slot), engine)

    private fun writeSnapshot(target: File, engine: GameEngine) {
        val serialized = SaveCodec.encode(engine.createSaveSnapshot().toJson(), System.currentTimeMillis())
        val atomicFile = AtomicFile(target)
        var stream: java.io.FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(serialized.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
            stream = null
        } catch (failure: Exception) {
            if (stream != null) atomicFile.failWrite(stream)
            throw IOException("Unable to write save file", failure)
        }
    }

    private fun loadFromFile(target: File, engine: GameEngine): Boolean {
        if (!target.exists()) return false
        val root = SaveCodec.decode(
            AtomicFile(target).openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
        )
        val snapshot = EngineSaveSnapshot.fromJson(root.getJSONObject("state"))
        engine.restoreSaveSnapshot(snapshot)
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
        const val SAVE_VERSION = SaveVersion.CURRENT
    }
}

private fun EngineSaveSnapshot.toJson(): JSONObject = JSONObject()
    .put("player", player.toJson())
    .put("inventory", inventory.toItemStacksJson())
    .put("bunkerStorage", bunkerStorage.toItemStacksJson())
    .put("needs", needs.toJson())
    .put("psychology", psychology.toJson())
    .put("bookLibrary", bookLibrary.toJson())
    .put("radio", radio.toJson())
    .put("clock", clock.toJson())
    .put("remainingResources", remainingResources.toResourcesJson())
    .put("zone", zone.name)
    .put("zoneResources", zoneResources.toZoneResourcesJson())
    .put("discoveredPoiIds", JSONArray(discoveredPoiIds.toList()))
    .put("equipment", equipment.toJson())
    .put("statistics", statistics.toJson())
    .put("bunkerPower", bunkerPower.toJson())
    .put("wakeCheckpoint", wakeCheckpoint?.toJson() ?: JSONObject.NULL)

private fun PlayerSnapshot.toJson() = JSONObject()
    .put("x", x.toDouble()).put("y", y.toDouble()).put("speed", speed.toDouble())

private fun SurvivalNeedsSnapshot.toJson() = JSONObject()
    .put("health", health.toDouble())
    .put("hunger", hunger.toDouble())
    .put("thirst", thirst.toDouble())
    .put("energy", energy.toDouble())

private fun BookLibrarySnapshot.toJson() = JSONObject().put("readBookIds", JSONArray(readBookIds.toList()))

private fun BunkerRadioSnapshot.toJson() = JSONObject().put("lastListenedDay", lastListenedDay)

private fun PlayerEquipmentSnapshot.toJson() = JSONObject()
    .put("weapon", weapon?.name ?: JSONObject.NULL)
    .put("weaponDrawn", weaponDrawn)

private fun BunkerPowerSnapshot.toJson() = JSONObject()
    .put("generatorRunning", generatorRunning)
    .put("fuelMinutesRemaining", fuelMinutesRemaining)

private fun CharacterStatisticsSnapshot.toJson() = JSONObject()
    .put("daysSurvived", daysSurvived)
    .put("expeditionMinutes", expeditionMinutes)
    .put("distanceTravelled", distanceTravelled.toDouble())
    .put("resourcesCollected", resourcesCollected)
    .put("itemsCrafted", itemsCrafted)
    .put("booksRead", booksRead)
    .put("visitedZones", JSONArray(visitedZones.map { it.name }))

private fun PsychologicalStateSnapshot.toJson() = JSONObject()
    .put("stress", stress.toDouble())
    .put("loneliness", loneliness.toDouble())
    .put("fatigue", fatigue.toDouble())
    .put("morale", morale.toDouble())

private fun WorldClockSnapshot.toJson() = JSONObject()
    .put("day", day).put("minuteOfDay", minuteOfDay).put("totalMinutes", totalMinutes)

private fun List<ItemStack?>.toItemStacksJson(): JSONArray = JSONArray().also { array ->
    forEach { stack ->
        array.put(if (stack == null) JSONObject.NULL else JSONObject()
            .put("type", stack.type.name)
            .put("quantity", stack.quantity))
    }
}

private fun List<ZoneResourceSnapshot>.toZoneResourcesJson(): JSONArray = JSONArray().also { array ->
    forEach { snapshot ->
        array.put(JSONObject()
            .put("zone", snapshot.zone.name)
            .put("seed", snapshot.seed ?: JSONObject.NULL)
            .put("resources", snapshot.resources.toResourcesJson()))
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
    .put("psychology", psychology.toJson())
    .put("bookLibrary", bookLibrary.toJson())
    .put("radio", radio.toJson())
    .put("clock", clock.toJson())
    .put("remainingResources", remainingResources.toResourcesJson())
    .put("zone", zone.name)
    .put("zoneResources", zoneResources.toZoneResourcesJson())
    .put("discoveredPoiIds", JSONArray(discoveredPoiIds.toList()))
    .put("equipment", equipment.toJson())
    .put("statistics", statistics.toJson())
    .put("bunkerPower", bunkerPower.toJson())

private fun EngineSaveSnapshot.Companion.fromJson(json: JSONObject): EngineSaveSnapshot {
    val checkpointJson = json.optJSONObject("wakeCheckpoint")
    val restoredZone = runCatching { WorldZone.valueOf(json.optString("zone", WorldZone.BUNKER.name)) }
        .getOrDefault(WorldZone.BUNKER)
    return EngineSaveSnapshot(
        player = json.getJSONObject("player").toPlayerSnapshot(),
        inventory = json.getJSONArray("inventory").toItemStacks(),
        bunkerStorage = json.getJSONArray("bunkerStorage").toItemStacks(),
        needs = json.getJSONObject("needs").toNeedsSnapshot(),
        psychology = json.optJSONObject("psychology")?.toPsychologySnapshot() ?: PsychologicalStateSnapshot(),
        bookLibrary = json.optJSONObject("bookLibrary")?.toBookLibrarySnapshot() ?: BookLibrarySnapshot(),
        radio = json.optJSONObject("radio")?.toBunkerRadioSnapshot() ?: BunkerRadioSnapshot(),
        clock = json.getJSONObject("clock").toClockSnapshot(),
        remainingResources = json.getJSONArray("remainingResources").toResources(),
        zone = restoredZone,
        zoneResources = json.optJSONArray("zoneResources")?.toZoneResources() ?: emptyList(),
        discoveredPoiIds = json.optJSONArray("discoveredPoiIds")?.toStringSet() ?: emptySet(),
        equipment = json.optJSONObject("equipment")?.toEquipmentSnapshot()
            ?: PlayerEquipmentSnapshot(weaponDrawn = restoredZone != WorldZone.BUNKER),
        statistics = json.optJSONObject("statistics")?.toStatisticsSnapshot() ?: CharacterStatisticsSnapshot(),
        bunkerPower = json.optJSONObject("bunkerPower")?.toBunkerPowerSnapshot() ?: BunkerPowerSnapshot(),
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

private fun JSONObject.toBunkerRadioSnapshot() = BunkerRadioSnapshot(
    lastListenedDay = optInt("lastListenedDay", 0).coerceAtLeast(0)
)

private fun JSONObject.toEquipmentSnapshot() = PlayerEquipmentSnapshot(
    weapon = runCatching { optString("weapon").takeIf { it.isNotBlank() && it != "null" }?.let { value -> WeaponType.valueOf(value) } }.getOrNull(),
    weaponDrawn = optBoolean("weaponDrawn", false)
)

private fun JSONObject.toBunkerPowerSnapshot() = BunkerPowerSnapshot(
    generatorRunning = optBoolean("generatorRunning", true),
    fuelMinutesRemaining = optInt("fuelMinutesRemaining", BunkerPowerSystem.DEFAULT_FUEL_MINUTES)
)

private fun JSONObject.toStatisticsSnapshot() = CharacterStatisticsSnapshot(
    daysSurvived = optInt("daysSurvived", 1),
    expeditionMinutes = optLong("expeditionMinutes", 0L),
    distanceTravelled = optDouble("distanceTravelled", 0.0).toFloat(),
    resourcesCollected = optInt("resourcesCollected", 0),
    itemsCrafted = optInt("itemsCrafted", 0),
    booksRead = optInt("booksRead", 0),
    visitedZones = optJSONArray("visitedZones")?.let { array ->
        (0 until array.length()).mapNotNull { index ->
            runCatching { WorldZone.valueOf(array.optString(index)) }.getOrNull()
        }.toSet()
    } ?: setOf(WorldZone.BUNKER)
)

private fun JSONObject.toBookLibrarySnapshot() = BookLibrarySnapshot(
    readBookIds = optJSONArray("readBookIds")?.let { array ->
        (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }.toSet()
    } ?: emptySet()
)

private fun JSONObject.toPsychologySnapshot() = PsychologicalStateSnapshot(
    stress = optDouble("stress", 10.0).toFloat(),
    loneliness = optDouble("loneliness", 0.0).toFloat(),
    fatigue = optDouble("fatigue", 0.0).toFloat(),
    morale = optDouble("morale", 70.0).toFloat()
)

private fun JSONObject.toClockSnapshot() = WorldClockSnapshot(
    day = getInt("day"),
    minuteOfDay = getInt("minuteOfDay"),
    totalMinutes = getLong("totalMinutes")
)

private fun JSONArray.toStringSet(): Set<String> =
    (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }.toSet()

private fun JSONArray.toItemStacks(): List<ItemStack?> = List(length()) { index ->
    if (isNull(index)) null else getJSONObject(index).let { stack ->
        ItemStack(ItemType.valueOf(stack.getString("type")), stack.getInt("quantity"))
    }
}

private fun JSONArray.toZoneResources(): List<ZoneResourceSnapshot> = List(length()) { index ->
    getJSONObject(index).let { entry ->
        ZoneResourceSnapshot(
            zone = WorldZone.valueOf(entry.getString("zone")),
            resources = entry.getJSONArray("resources").toResources(),
            seed = if (entry.isNull("seed")) null else entry.optLong("seed")
        )
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

private fun JSONObject.toWakeCheckpoint(): WakeCheckpoint {
    val restoredZone = runCatching { WorldZone.valueOf(optString("zone", WorldZone.BUNKER.name)) }
        .getOrDefault(WorldZone.BUNKER)
    return WakeCheckpoint(
    player = getJSONObject("player").toPlayerSnapshot(),
    inventory = getJSONArray("inventory").toItemStacks(),
    bunkerStorage = getJSONArray("bunkerStorage").toItemStacks(),
    needs = getJSONObject("needs").toNeedsSnapshot(),
    psychology = optJSONObject("psychology")?.toPsychologySnapshot() ?: PsychologicalStateSnapshot(),
    bookLibrary = optJSONObject("bookLibrary")?.toBookLibrarySnapshot() ?: BookLibrarySnapshot(),
    radio = optJSONObject("radio")?.toBunkerRadioSnapshot() ?: BunkerRadioSnapshot(),
    clock = getJSONObject("clock").toClockSnapshot(),
    remainingResources = getJSONArray("remainingResources").toResources(),
    zone = restoredZone,
    zoneResources = optJSONArray("zoneResources")?.toZoneResources() ?: emptyList(),
    discoveredPoiIds = optJSONArray("discoveredPoiIds")?.toStringSet() ?: emptySet(),
    equipment = optJSONObject("equipment")?.toEquipmentSnapshot()
        ?: PlayerEquipmentSnapshot(weaponDrawn = restoredZone != WorldZone.BUNKER),
    statistics = optJSONObject("statistics")?.toStatisticsSnapshot() ?: CharacterStatisticsSnapshot(),
        bunkerPower = optJSONObject("bunkerPower")?.toBunkerPowerSnapshot() ?: BunkerPowerSnapshot()
    )
}
