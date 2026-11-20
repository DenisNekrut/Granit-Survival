package com.granit.survival

import kotlin.math.sqrt
import kotlin.random.Random

/** Platform-independent simulation state for Granit Survival. */
data class EngineInput(val moveX: Float = 0f, val moveY: Float = 0f, val interact: Boolean = false, val fire: Boolean = false)

enum class PlayerFacing { DOWN, UP, LEFT, RIGHT }

data class PlayerState(
    // Start just inside the bunker hatch; the bedroom bed is not the initial spawn point.
    var x: Float = 8.5f,
    var y: Float = 2.5f,
    var speed: Float = 4.2f,
    var facing: PlayerFacing = PlayerFacing.DOWN
)

class GameEngine(private val mapSeedGenerator: () -> Long = { Random.nextLong() }) {
    companion object {
        const val WORLD_WIDTH = 40f
        const val WORLD_HEIGHT = 24f
        const val MEDICAL_KIT_HEALING = 35f
        const val BANDAGE_HEALING = 15f
        const val MEDICAL_STATION_HEALING = 50f
        const val MEDICAL_STATION_TREATMENT_MINUTES = 12
        const val RESOURCE_GATHER_MINUTES = 2
        const val FOREST_RELAY_REPAIR_MINUTES = 20
        const val FOREST_RELAY_REPAIR_METAL = 2
        const val FOREST_RELAY_REPAIR_FABRIC = 1
        const val RUINS_SERVICE_POWER_REPAIR_MINUTES = 15
        const val RUINS_SERVICE_POWER_REPAIR_METAL = 1
        const val RUINS_SERVICE_POWER_REPAIR_FABRIC = 1
        const val ZONE_TRAVEL_MINUTES = 8
        private const val FIXED_STEP_SECONDS = 1f / 60f
        private const val MAX_FRAME_SECONDS = 0.25f
        private const val GAME_MINUTE_REAL_SECONDS = 1f
    }

    val player = PlayerState()
    val equipment = PlayerEquipment()
    val statistics = CharacterStatistics()
    private val zoneMaps = mutableMapOf<WorldZone, TileMap>()
    private val zoneContainers = mutableMapOf<WorldZone, MutableMap<String, WorldContainer>>()
    var tileMap = createZoneMap(WorldZone.BUNKER).also { zoneMaps[WorldZone.BUNKER] = it }
        private set
    val zone: WorldZone get() = tileMap.zone
    val camera = Camera2D()
    val inventory = Inventory()
    val bunkerStorage = BunkerStorage()
    private val inventorySystem = InventorySystem(inventory, bunkerStorage)
    val needs = SurvivalNeeds()
    val psychology = PsychologicalState()
    val bookLibrary = BookLibrary()
    val radioSystem = BunkerRadioSystem()
    val worldClock = WorldClock()
    val bunkerPower = BunkerPowerSystem()
    val storyProgress = StoryProgressSystem()
    val weather: WeatherType get() = WeatherSystem.forDay(worldClock.day)
    val weatherMovementMultiplier: Float get() =
        if (zone == WorldZone.BUNKER) 1f else weather.outdoorMovementMultiplier
    val checkpointSystem = CheckpointSystem()
    private val clockAndNeedsSystem = ClockAndNeedsSystem(
        worldClock, bunkerPower, statistics, needs, psychology
    )

    private var accumulator = 0f
    private var gameMinuteAccumulator = 0f
    var simulationTimeSeconds: Double = 0.0
        private set
    var interactionCount: Int = 0
        private set
    var collectedResourceCount: Int = 0
        private set
    var lastCollectedItem: ItemType? = null
        private set
    var isMoving: Boolean = false
        private set
    private var wasInteracting = false
    val zombies = mutableListOf<ZombieActor>()
    val autoAimTarget: ZombieActor? get() =
        CombatSystem.findAutoAimTarget(zone, zombies, player, tileMap)
    private var pulseCooldownSeconds = 0f
    private var idleSeconds = 0f
    // First idle destination must differ from the initial spawn room.
    private var wanderRoomIndex = 1
    private var wanderTarget: Pair<Float, Float>? = null
    private val discoveredPoiIds = linkedSetOf<String>()
    var lastDiscoveryTitle: String? = null
        private set
    var lastDiscoveryHudLabel: String? = null
        private set

    fun isPoiDiscovered(id: String): Boolean = id in discoveredPoiIds
    fun discoveredPointsOfInterest(): Set<String> = discoveredPoiIds.toSet()
    fun discoveredPointDetails(): List<PointOfInterest> =
        PointOfInterestCatalog.all().filter { it.id in discoveredPoiIds }

    fun restoreDiscoveredPointsOfInterest(ids: Set<String>) {
        discoveredPoiIds.clear()
        discoveredPoiIds.addAll(ids.filter { PointOfInterestCatalog.byId(it) != null })
        lastDiscoveryTitle = null
        lastDiscoveryHudLabel = null
    }

    fun scanNearbyPointsOfInterest(): List<PointOfInterest> {
        val found = PointOfInterestCatalog.inZone(zone).filter { poi ->
            val dx = player.x - poi.x
            val dy = player.y - poi.y
            val storyGateOpen = poi.requiredStoryEventId?.let(storyProgress::hasEvent) ?: true
            storyGateOpen &&
                dx * dx + dy * dy <= poi.discoveryRadius * poi.discoveryRadius &&
                discoveredPoiIds.add(poi.id)
        }
        if (found.any { it.id == "ruins_checkpoint" }) {
            storyProgress.recordEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED)
        }
        if (found.any { it.id == "ruins_outer_route" }) {
            storyProgress.recordEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND)
        }
        if (found.any { it.id == "ruins_service_access" }) {
            storyProgress.recordEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND)
        }
        if (found.any { it.id == "ruins_power_junction" }) {
            storyProgress.recordEvent(StoryEventIds.RUINS_POWER_JUNCTION_FOUND)
        }
        if ("forest_radio_cache" in discoveredPoiIds) {
            when {
                storyProgress.hasEvent(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE) ->
                    storyProgress.recordEvent(StoryEventIds.RADIO_ELSEWHERE_RELAY_FOUND)
                storyProgress.hasEvent(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST) ->
                    storyProgress.recordEvent(StoryEventIds.EQUIPMENT_FIRST_RELAY_INSPECTED)
            }
        }
        found.lastOrNull()?.let {
            lastDiscoveryTitle = it.title
            lastDiscoveryHudLabel = it.hudLabel
        }
        return found
    }

    init {
        statistics.visitZone(zone)
        camera.snapTo(player.x, player.y, tileMap.width.toFloat(), tileMap.height.toFloat())
        // The initial safe state acts as the first wake-up checkpoint.
        checkpointSystem.capture(this)
    }

    /** Clear latched touch state when the render surface is paused or recreated. */
    fun releaseInputState() {
        wasInteracting = false
        isMoving = false
    }

    /** Apply a render viewport change and reframe immediately around the player. */
    fun setCameraViewport(width: Float, height: Float) {
        val changed = camera.viewportWidth != width || camera.viewportHeight != height
        camera.setViewport(width, height)
        if (changed) camera.snapTo(player.x, player.y, tileMap.width.toFloat(), tileMap.height.toFloat())
    }

    fun updateFrame(frameDeltaSeconds: Float, input: EngineInput): Int {
        accumulator += frameDeltaSeconds.coerceIn(0f, MAX_FRAME_SECONDS)
        var steps = 0
        while (accumulator >= FIXED_STEP_SECONDS) {
            fixedUpdate(FIXED_STEP_SECONDS, input)
            accumulator -= FIXED_STEP_SECONDS
            steps++
        }
        // Camera tracking belongs to the render-frame cadence, not the fixed simulation
        // cadence. This also applies a freshly resized bunker viewport before the next
        // simulation step and prevents the camera from appearing frozen between steps.
        camera.follow(player.x, player.y, tileMap.width.toFloat(), tileMap.height.toFloat(),
            frameDeltaSeconds.coerceIn(0f, MAX_FRAME_SECONDS))
        return steps
    }

    /** Sleep until 07:00, restore energy, begin the next wake cycle and save a checkpoint. */
    fun sleepUntilMorning(): Int {
        val sleptMinutes = worldClock.sleepUntilMorning()
        bunkerPower.advanceMinutes(sleptMinutes)
        needs.sleep(sleptMinutes)
        psychology.sleep(sleptMinutes)
        statistics.recordMinutes(sleptMinutes, WorldZone.BUNKER, worldClock.day)
        gameMinuteAccumulator = 0f
        if (needs.health <= 0f) {
            checkpointSystem.restore(this)
        } else {
            checkpointSystem.capture(this)
        }
        return sleptMinutes
    }

    /** Sleep is only available beside a bed in the bunker. */
    fun canSleep(): Boolean = zone == WorldZone.BUNKER && tileMap.isNearTile(TileType.BED, player.x, player.y, 1.5f)

    fun canAccessStorage(): Boolean = zone == WorldZone.BUNKER && tileMap.isNearTile(TileType.STORAGE, player.x, player.y, 2.1f)

    fun canAccessWorkbench(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.WORKBENCH, player.x, player.y, 1.5f)

    fun canUseWorkbench(): Boolean = canAccessWorkbench() && bunkerPower.hasPower

    fun canAccessBookshelf(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.BOOKSHELF, player.x, player.y, 1.25f)

    fun canAccessDiningTable(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.DINING_TABLE, player.x, player.y, 1.5f)

    fun canAccessGenerator(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.GENERATOR, player.x, player.y, 1.5f)

    fun toggleGenerator(): Boolean = canAccessGenerator() && bunkerPower.toggleGenerator()

    /** One wood unit represents four hours of abstract generator fuel. */
    fun refuelGenerator(): Boolean {
        if (!canAccessGenerator() || !inventory.remove(ItemType.WOOD, 1)) return false
        if (!bunkerPower.addFuel(BunkerPowerSystem.WOOD_FUEL_MINUTES)) {
            inventory.add(ItemType.WOOD, 1)
            return false
        }
        return advanceGameMinutes(2)
    }

    fun canAccessMedicalStation(): Boolean = zone == WorldZone.BUNKER &&
        tileMap.isNearTile(TileType.MEDICAL_STATION, player.x, player.y, 1.5f)

    /** Powered treatment at the medical station takes 12 game minutes and restores 50 health. */
    fun canUseMedicalStation(): Boolean = canAccessMedicalStation() && bunkerPower.hasPower

    fun treatAtMedicalStation(): Boolean {
        if (!canUseMedicalStation() || needs.health >= 100f ||
            inventory.count(ItemType.MEDICAL_KIT) < 1) return false
        // Advance first: if survival needs trigger a checkpoint rollback, no kit or healing is lost.
        if (!advanceGameMinutes(MEDICAL_STATION_TREATMENT_MINUTES)) return false
        if (!inventory.remove(ItemType.MEDICAL_KIT, 1)) return false
        needs.restoreHealth(MEDICAL_STATION_HEALING)
        return true
    }

    fun canUseRadio(): Boolean = zone == WorldZone.BUNKER && bunkerPower.hasPower &&
        tileMap.isNearTile(TileType.RADIO, player.x, player.y, 1.5f) && radioSystem.canListen(worldClock.day)

    fun listenToRadio(): RadioEvent? {
        if (!canUseRadio()) return null
        val listeningDay = worldClock.day
        val event = radioSystem.eventForDay(listeningDay) ?: return null
        if (!advanceGameMinutes(event.broadcastMinutes)) return null
        radioSystem.markListened(listeningDay)
        psychology.applyReading(event.moraleGain, event.stressRelief)
        return if (event.id == "weather") {
            event.copy(description = "Прогноз на завтра: " + WeatherSystem.forDay(listeningDay + 1).forecastText)
        } else event
    }

    fun readBook(bookId: String): Boolean {
        if (!canAccessBookshelf()) return false
        val book = BookCatalog.byId(bookId) ?: return false
        if (bookLibrary.hasRead(book.id)) return false
        if (!advanceGameMinutes(book.readMinutes)) return false
        bookLibrary.markRead(book.id)
        statistics.recordBookRead()
        psychology.applyReading(book.moraleGain, book.stressRelief)
        return true
    }

    fun canCraft(recipeId: String): Boolean {
        val recipe = CraftingRecipe.byId(recipeId) ?: return false
        return recipe.isUnlocked(bookLibrary) && recipe.canCraft(inventory)
    }

    fun craft(recipeId: String): Boolean {
        if (!canUseWorkbench()) return false
        val recipe = CraftingRecipe.byId(recipeId) ?: return false
        if (!recipe.isUnlocked(bookLibrary) || !recipe.canCraft(inventory)) return false
        if (!advanceGameMinutes(recipe.craftMinutes)) return false
        recipe.ingredients.forEach { (type, amount) -> inventory.remove(type, amount) }
        check(inventory.add(recipe.output, recipe.outputAmount) == 0) {
            "Crafting output did not fit after ingredient validation"
        }
        statistics.recordCrafted(recipe.outputAmount)
        return true
    }

    fun useBandage(): Boolean {
        if (needs.health >= 100f || inventory.count(ItemType.BANDAGE) < 1) return false
        if (!advanceGameMinutes(3)) return false
        if (!inventory.remove(ItemType.BANDAGE, 1)) return false
        needs.restoreHealth(BANDAGE_HEALING)
        return true
    }

    fun useMedicalKit(): Boolean {
        if (needs.health >= 100f || inventory.count(ItemType.MEDICAL_KIT) < 1) return false
        if (!advanceGameMinutes(5)) return false
        if (!inventory.remove(ItemType.MEDICAL_KIT, 1)) return false
        needs.restoreHealth(MEDICAL_KIT_HEALING)
        return true
    }

    /**
     * Enter a zone. Resource maps marked procedural receive a fresh seed on every
     * new entry; fixed maps (bunker and first forest area) are reused unchanged.
     * Save/checkpoint restoration passes regenerate=false to rebuild the exact map.
     */
    fun changeZone(target: WorldZone, regenerate: Boolean = true, entryFrom: WorldZone? = null) {
        val previousZone = zone
        if (target != previousZone) psychology.onZoneChange(previousZone, target)
        val targetMap = if (ZoneTransitionSystem.shouldGenerateFreshMap(target, regenerate)) {
            zoneContainers.remove(target)
            createZoneMap(target, mapSeedGenerator()).also { zoneMaps[target] = it }
        } else {
            zoneMaps.getOrPut(target) { createZoneMap(target) }
        }
        tileMap = targetMap
        ensureZoneContainers(target, targetMap)
        equipment.onZoneChanged(previousZone, target)
        if (target != previousZone) statistics.visitZone(target)
        lastDiscoveryTitle = null
        lastDiscoveryHudLabel = null
        val arrival = entryFrom
            ?.let { ZoneTransitionSystem.arrivalExitFor(it, target) }
            ?.let(tileMap::spawnBesideExit)
        player.x = arrival?.first ?: target.spawnX
        player.y = arrival?.second ?: target.spawnY
        camera.snapTo(player.x, player.y, tileMap.width.toFloat(), tileMap.height.toFloat())
        zombies.clear()
        zombies += CombatSystem.spawn(target, tileMap, player.x, player.y)
        pulseCooldownSeconds = 0f
        wasInteracting = false
        isMoving = false
        idleSeconds = 0f
        wanderTarget = null
    }

    /** Snapshot only maps that actually exist; an unvisited expedition remains ungenerated. */
    fun zoneResourceSnapshot(): List<ZoneResourceSnapshot> = zoneMaps.map { (target, map) ->
        ensureZoneContainers(target, map)
        ZoneResourceSnapshot(target, map.resourceSnapshot(), map.layoutSeed,
            zoneContainers[target].orEmpty().values.map { it.toSnapshot() })
    }

    fun restoreZoneResources(snapshots: List<ZoneResourceSnapshot>) {
        zoneMaps.clear(); zoneContainers.clear()
        if (snapshots.isEmpty()) {
            val bunker = createZoneMap(WorldZone.BUNKER); zoneMaps[WorldZone.BUNKER] = bunker
            ensureZoneContainers(WorldZone.BUNKER, bunker); return
        }
        snapshots.forEach { snapshot ->
            val map = createZoneMap(snapshot.zone, snapshot.seed); map.restoreResources(snapshot.resources)
            zoneMaps[snapshot.zone] = map; ensureZoneContainers(snapshot.zone, map)
            snapshot.containers.forEach { saved ->
                val container = zoneContainers[snapshot.zone]?.get(saved.id)
                    ?: WorldContainer(saved.id, snapshot.zone, saved.x, saved.y, saved.title).also {
                        zoneContainers.getOrPut(snapshot.zone) { linkedMapOf() }[saved.id] = it
                    }
                container.restore(saved.contents)
            }
        }
        val bunker = zoneMaps.getOrPut(WorldZone.BUNKER) { createZoneMap(WorldZone.BUNKER) }
        ensureZoneContainers(WorldZone.BUNKER, bunker)
    }

    private fun ensureZoneContainers(target: WorldZone, map: TileMap) {
        val containers = zoneContainers.getOrPut(target) { linkedMapOf() }
        map.forEachTile { x, y, type -> if (type == TileType.LOOT_CACHE) {
            val id = "${target.name.lowercase()}_${x}_$y"
            containers.getOrPut(id) {
                WorldContainer(id, target, x, y, "ТАЙНИК С ПРИПАСАМИ").apply { seedInitialContents() }
            }
        } }
        PointOfInterestCatalog.inZone(target).filter { it.cacheTier != null }.forEach { poi ->
            val x = kotlin.math.floor(poi.x).toInt()
            val y = kotlin.math.floor(poi.y).toInt()
            val id = "poi_${poi.id}"
            containers.getOrPut(id) {
                WorldContainer(id, target, x, y, "ТАЙНИК: ${poi.title}", lootTierOverride = poi.cacheTier)
                    .apply { seedInitialContents() }
            }
        }
    }

    /** The relay can only be inspected after the landmark has been discovered. */
    fun inspectDiscoveredForestRelay(): Boolean {
        if (!isPoiDiscovered("forest_radio_cache")) return false
        return storyProgress.recordEvent(StoryEventIds.FOREST_RELAY_INSPECTED)
    }

    /** The road clue is available only after the watch-post measurement and vehicle discovery. */
    fun inspectRoadSignalMap(): Boolean {
        if (!storyProgress.hasEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)) return false
        if (!isPoiDiscovered("road_vehicle")) return false
        return storyProgress.recordEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)
    }

    /** The ruins marker can be studied only after its landmark has been discovered. */
    fun inspectRuinsSignalMarker(): Boolean {
        if (!isPoiDiscovered("ruins_checkpoint")) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED)) return false
        return storyProgress.recordEvent(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED)
    }

    /** The outer route sign can be inspected only after its landmark has been discovered. */
    fun inspectRuinsOuterRoute(): Boolean {
        if (!isPoiDiscovered("ruins_outer_route")) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND)) return false
        return storyProgress.recordEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED)
    }

    /** The service entrance can be inspected only after it has been discovered. */
    fun inspectRuinsServiceAccess(): Boolean {
        if (!isPoiDiscovered("ruins_service_access")) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND)) return false
        return storyProgress.recordEvent(StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED)
    }

    /** The power junction is available only after the service entrance has been inspected. */
    fun inspectRuinsPowerJunction(): Boolean {
        if (!isPoiDiscovered("ruins_power_junction")) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_FOUND)) return false
        return storyProgress.recordEvent(StoryEventIds.RUINS_POWER_JUNCTION_INSPECTED)
    }

    fun canRestoreRuinsServicePower(): Boolean =
        storyProgress.hasEvent(StoryEventIds.RUINS_POWER_JUNCTION_INSPECTED) &&
            !storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_POWER_RESTORED) &&
            inventory.count(ItemType.METAL) >= RUINS_SERVICE_POWER_REPAIR_METAL &&
            inventory.count(ItemType.FABRIC) >= RUINS_SERVICE_POWER_REPAIR_FABRIC

    /** Restores the service line once, consuming materials only after survival time is advanced. */
    fun restoreRuinsServicePower(): Boolean {
        if (!canRestoreRuinsServicePower()) return false
        if (!advanceGameMinutes(RUINS_SERVICE_POWER_REPAIR_MINUTES)) return false
        if (!inventory.remove(ItemType.METAL, RUINS_SERVICE_POWER_REPAIR_METAL)) return false
        if (!inventory.remove(ItemType.FABRIC, RUINS_SERVICE_POWER_REPAIR_FABRIC)) {
            inventory.add(ItemType.METAL, RUINS_SERVICE_POWER_REPAIR_METAL)
            return false
        }
        return storyProgress.recordEvent(StoryEventIds.RUINS_SERVICE_POWER_RESTORED)
    }

    fun openRuinsServiceAccess(): Boolean {
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND)) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_INSPECTED)) return false
        if (!storyProgress.hasEvent(StoryEventIds.RUINS_SERVICE_POWER_RESTORED)) return false
        return storyProgress.recordEvent(StoryEventIds.RUINS_SERVICE_ACCESS_OPENED)
    }

    /** The watch-post check is available only after the repaired relay and landmark are both known. */
    fun inspectForestWatchPostSignal(): Boolean {
        if (!storyProgress.hasEvent(StoryEventIds.FOREST_RELAY_REPAIRED)) return false
        if (!isPoiDiscovered("forest_watch_post")) return false
        return storyProgress.recordEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)
    }

    fun canRepairDiscoveredForestRelay(): Boolean =
        isPoiDiscovered("forest_radio_cache") &&
            storyProgress.hasEvent(StoryEventIds.FOREST_RELAY_INSPECTED) &&
            !storyProgress.hasEvent(StoryEventIds.FOREST_RELAY_REPAIRED) &&
            inventory.count(ItemType.METAL) >= FOREST_RELAY_REPAIR_METAL &&
            inventory.count(ItemType.FABRIC) >= FOREST_RELAY_REPAIR_FABRIC

    /** Repairs the discovered relay once; its story state persists beyond expedition rollback. */
    fun repairDiscoveredForestRelay(): Boolean {
        if (!canRepairDiscoveredForestRelay()) return false
        // Advance first so a survival checkpoint rollback cannot consume repair materials.
        if (!advanceGameMinutes(FOREST_RELAY_REPAIR_MINUTES)) return false
        if (!inventory.remove(ItemType.METAL, FOREST_RELAY_REPAIR_METAL)) return false
        if (!inventory.remove(ItemType.FABRIC, FOREST_RELAY_REPAIR_FABRIC)) {
            inventory.add(ItemType.METAL, FOREST_RELAY_REPAIR_METAL)
            return false
        }
        return storyProgress.recordEvent(StoryEventIds.FOREST_RELAY_REPAIRED)
    }

    fun nearestWorldContainer(maxDistance: Float = 1.35f): WorldContainer? {
        val limit = maxDistance * maxDistance
        return zoneContainers[zone].orEmpty().values.minByOrNull { c ->
            val dx = c.x + 0.5f - player.x; val dy = c.y + 0.5f - player.y; dx * dx + dy * dy
        }?.takeIf { c -> val dx = c.x + 0.5f - player.x; val dy = c.y + 0.5f - player.y; dx * dx + dy * dy <= limit }
    }

    fun transferFromWorldContainerAt(id: String, slot: Int, amount: Int): Boolean {
        val c = nearestWorldContainer()?.takeIf { it.id == id } ?: return false
        if (!ItemContainerTransfer.moveAt(c, inventory, slot, amount)) return false
        return advanceGameMinutes(1)
    }

    fun transferToWorldContainerAt(id: String, slot: Int, amount: Int): Boolean {
        val c = nearestWorldContainer()?.takeIf { it.id == id } ?: return false
        if (!ItemContainerTransfer.moveAt(inventory, c, slot, amount)) return false
        return advanceGameMinutes(1)
    }

    fun transferToStorage(type: ItemType, amount: Int = inventory.count(type)): Boolean {
        if (!canAccessStorage() || !inventorySystem.transferToStorage(type, amount)) return false
        return advanceGameMinutes(1)
    }

    fun transferFromStorage(type: ItemType, amount: Int = bunkerStorage.count(type)): Boolean {
        if (!canAccessStorage() || !inventorySystem.transferFromStorage(type, amount)) return false
        return advanceGameMinutes(1)
    }

    fun transferToStorageAt(slotIndex: Int, amount: Int): Boolean {
        if (!canAccessStorage() || !inventorySystem.transferToStorageAt(slotIndex, amount)) return false
        return advanceGameMinutes(1)
    }

    fun transferFromStorageAt(slotIndex: Int, amount: Int): Boolean {
        if (!canAccessStorage() || !inventorySystem.transferFromStorageAt(slotIndex, amount)) return false
        return advanceGameMinutes(1)
    }

    fun consumeFood(): Boolean {
        if (!inventorySystem.remove(ItemType.CANNED_FOOD, 1)) return false
        needs.consumeFood()
        return advanceGameMinutes(5)
    }

    fun drinkWater(): Boolean {
        if (!inventorySystem.remove(ItemType.WATER, 1)) return false
        needs.drinkWater()
        return advanceGameMinutes(5)
    }

    /** Apply the cost of a discrete action using the same clock/needs rules as real time. */
    private fun advanceGameMinutes(minutes: Int): Boolean {
        return clockAndNeedsSystem.advanceActionMinutes(minutes, zone, isMoving) {
            checkpointSystem.restore(this)
            gameMinuteAccumulator = 0f
        }
    }


    private fun travelToZone(target: WorldZone) {
        if (target == zone) return
        val source = zone
        if (source == WorldZone.BUNKER && target == WorldZone.FOREST) {
            storyProgress.recordEvent(StoryEventIds.FIRST_BUNKER_EXIT)
        }
        changeZone(target, entryFrom = source)
        advanceGameMinutes(ZONE_TRAVEL_MINUTES)
    }

    /** Restore the complete wake-up state after a failed expedition. */
    fun respawnAtWakeCheckpoint(): Boolean = checkpointSystem.restore(this)

    fun createSaveSnapshot(): EngineSaveSnapshot = SaveSnapshotFactory.create(
        player = player,
        inventory = inventory,
        bunkerStorage = bunkerStorage,
        needs = needs,
        psychology = psychology,
        worldClock = worldClock,
        tileMap = tileMap,
        zone = zone,
        zoneResources = zoneResourceSnapshot(),
        bookLibrary = bookLibrary,
        radioSystem = radioSystem,
        discoveredPoiIds = discoveredPoiIds.toSet(),
        equipment = equipment,
        statistics = statistics,
        bunkerPower = bunkerPower,
        storyProgress = storyProgress,
        wakeCheckpoint = checkpointSystem.snapshot()
    )

    fun restoreSaveSnapshot(snapshot: EngineSaveSnapshot) {
        restoreZoneResources(snapshot.zoneResources)
        changeZone(snapshot.zone, regenerate = false)
        equipment.restore(snapshot.equipment)
        statistics.restore(snapshot.statistics)
        bunkerPower.restore(snapshot.bunkerPower)
        storyProgress.restore(snapshot.storyProgress)
        restoreDiscoveredPointsOfInterest(snapshot.discoveredPoiIds)
        if (snapshot.zoneResources.isEmpty()) tileMap.restoreResources(snapshot.remainingResources)
        player.x = snapshot.player.x
        player.y = snapshot.player.y
        player.speed = snapshot.player.speed
        inventory.restore(snapshot.inventory)
        bunkerStorage.restore(snapshot.bunkerStorage)
        needs.restore(snapshot.needs)
        psychology.restore(snapshot.psychology)
        bookLibrary.restore(snapshot.bookLibrary)
        radioSystem.restore(snapshot.radio)
        worldClock.restore(snapshot.clock)
        tileMap.restoreResources(snapshot.remainingResources)
        checkpointSystem.restoreSnapshot(snapshot.wakeCheckpoint)
        camera.snapTo(player.x, player.y, tileMap.width.toFloat(), tileMap.height.toFloat())
        accumulator = 0f
        gameMinuteAccumulator = 0f
        wasInteracting = false
        isMoving = false
        idleSeconds = 0f
        wanderTarget = null
        player.facing = PlayerFacing.DOWN
    }

    private fun fixedUpdate(delta: Float, input: EngineInput) {
        val previousX = player.x
        val previousY = player.y
        val manualLength = sqrt(input.moveX * input.moveX + input.moveY * input.moveY)
        val manualInput = manualLength > 0.08f || input.interact || input.fire
        if (manualInput) {
            idleSeconds = 0f
            wanderTarget = null
        } else if (zone == WorldZone.BUNKER) {
            idleSeconds += delta
            if (idleSeconds >= 15f && wanderTarget == null) {
                val rooms = listOf(1.5f to 1.5f, 8.5f to 2.5f, 13.5f to 1.5f,
                    1.5f to 6.5f, 9.5f to 7.5f, 13.5f to 7.5f)
                var checked = 0
                while (checked < rooms.size && wanderTarget == null) {
                    wanderRoomIndex = (wanderRoomIndex + 1) % rooms.size
                    val candidate = rooms[wanderRoomIndex]
                    if (tileMap.isWalkable(candidate.first, candidate.second)) wanderTarget = candidate
                    checked++
                }
            }
        } else {
            idleSeconds = 0f
            wanderTarget = null
        }

        val autoDirection = if (manualLength <= 0.08f && !input.interact && !input.fire && zone == WorldZone.BUNKER) {
            wanderTarget?.let { target ->
                if (kotlin.math.abs(player.x - target.first) < 0.18f &&
                    kotlin.math.abs(player.y - target.second) < 0.18f) {
                    wanderTarget = null
                    idleSeconds = 0f
                    null
                } else if (kotlin.math.floor(player.x).toInt() == kotlin.math.floor(target.first).toInt() &&
                    kotlin.math.floor(player.y).toInt() == kotlin.math.floor(target.second).toInt()) target
                else tileMap.nextPathStep(player.x, player.y, target.first, target.second)
            }
        } else null
        val moveX = if (manualLength > 0.08f) input.moveX else autoDirection?.let { (x, _) -> (x - player.x).coerceIn(-1f, 1f) } ?: 0f
        val moveY = if (manualLength > 0.08f) input.moveY else autoDirection?.let { (_, y) -> (y - player.y).coerceIn(-1f, 1f) } ?: 0f
        val length = sqrt(moveX * moveX + moveY * moveY)
        val scale = if (length > 1f) 1f / length else 1f
        val effectiveSpeed = player.speed * needs.movementSpeedMultiplier *
            psychology.movementSpeedMultiplier * weatherMovementMultiplier
        val autoStep = manualLength <= 0.08f && autoDirection != null
        val stepX = if (autoStep) (autoDirection!!.first - player.x).coerceIn(-effectiveSpeed * delta, effectiveSpeed * delta)
            else moveX * scale * effectiveSpeed * delta
        val stepY = if (autoStep) (autoDirection!!.second - player.y).coerceIn(-effectiveSpeed * delta, effectiveSpeed * delta)
            else moveY * scale * effectiveSpeed * delta
        isMoving = kotlin.math.abs(stepX) > 0.0001f || kotlin.math.abs(stepY) > 0.0001f
        if (isMoving) {
            player.facing = if (kotlin.math.abs(moveX) > kotlin.math.abs(moveY)) {
                if (moveX < 0f) PlayerFacing.LEFT else PlayerFacing.RIGHT
            } else {
                if (moveY < 0f) PlayerFacing.UP else PlayerFacing.DOWN
            }
        }
        val nextX = (player.x + stepX).coerceIn(0.65f, tileMap.width - 0.65f)
        if (tileMap.isWalkable(nextX, player.y)) player.x = nextX
        val nextY = (player.y + stepY).coerceIn(0.8f, tileMap.height - 0.8f)
        if (tileMap.isWalkable(player.x, nextY)) player.y = nextY
        val movedX = player.x - previousX
        val movedY = player.y - previousY
        statistics.recordDistance(sqrt(movedX * movedX + movedY * movedY))
        scanNearbyPointsOfInterest()
        if (input.interact && !wasInteracting) interact()
        if (input.fire && pulseCooldownSeconds <= 0f && zone != WorldZone.BUNKER && equipment.weaponDrawn) {
            pulseCooldownSeconds = 0.55f
            val selectedTarget = autoAimTarget
            CombatSystem.firePulse(zombies, selectedTarget, player)
        }
        pulseCooldownSeconds = (pulseCooldownSeconds - delta).coerceAtLeast(0f)
        val contactDamage = CombatSystem.update(zombies, player, tileMap, delta)
        if (contactDamage > 0f) {
            needs.applyDamage(contactDamage)
            if (needs.health <= 0f) {
                checkpointSystem.restore(this)
                gameMinuteAccumulator = 0f
            }
        }
        wasInteracting = input.interact
        gameMinuteAccumulator += delta
        while (gameMinuteAccumulator + 0.00001f >= GAME_MINUTE_REAL_SECONDS) {
            val survived = clockAndNeedsSystem.advanceRealtimeMinute(zone, isMoving)
            gameMinuteAccumulator -= GAME_MINUTE_REAL_SECONDS
            if (!survived) {
                checkpointSystem.restore(this)
                gameMinuteAccumulator = 0f
                break
            }
        }
        simulationTimeSeconds += delta.toDouble()
    }

    private fun interact() {
        val resource = tileMap.nearestResource(player.x, player.y, 1.35f)
        if (resource != null) {
            // Remove the world node only after the inventory accepted the item.
            if (inventorySystem.add(resource.type, 1) == 0 && tileMap.collectResource(resource)) {
                collectedResourceCount++
                statistics.recordResourceCollected()
                lastCollectedItem = resource.type
                interactionCount++
                advanceGameMinutes(RESOURCE_GATHER_MINUTES)
            }
            return
        }

        when (tileMap.nearestInteractable(player.x, player.y, 1.35f)) {
            TileType.STORAGE -> {
                // Opening a storage station is a UI action only. Item movement is
                // performed exclusively by the player's explicit transfer controls.
                interactionCount++
            }
            TileType.EXIT_TO_BUNKER -> { travelToZone(WorldZone.BUNKER); interactionCount++ }
            TileType.EXIT_TO_FOREST -> { travelToZone(WorldZone.FOREST); interactionCount++ }
            TileType.EXIT_TO_RUINS -> { travelToZone(WorldZone.RUINS); interactionCount++ }
            TileType.EXIT_TO_ROAD -> { travelToZone(WorldZone.ROAD); interactionCount++ }
            TileType.EXIT -> {
                travelToZone(if (zone == WorldZone.BUNKER) WorldZone.FOREST else WorldZone.BUNKER)
                interactionCount++
            }
            TileType.BED, TileType.WORKBENCH, TileType.BOOKSHELF, TileType.RADIO,
            TileType.DINING_TABLE, TileType.GENERATOR, TileType.MEDICAL_STATION -> interactionCount++
            else -> Unit
        }
    }
}
