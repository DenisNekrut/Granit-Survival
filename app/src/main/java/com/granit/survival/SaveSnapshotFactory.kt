package com.granit.survival

/** Builds an immutable persistence snapshot from the live simulation state. */
internal object SaveSnapshotFactory {
    fun create(
        player: PlayerState,
        inventory: Inventory,
        bunkerStorage: BunkerStorage,
        needs: SurvivalNeeds,
        psychology: PsychologicalState,
        worldClock: WorldClock,
        tileMap: TileMap,
        zone: WorldZone,
        zoneResources: List<ZoneResourceSnapshot>,
        bookLibrary: BookLibrary,
        radioSystem: BunkerRadioSystem,
        discoveredPoiIds: Set<String>,
        equipment: PlayerEquipment,
        statistics: CharacterStatistics,
        bunkerPower: BunkerPowerSystem,
        storyProgress: StoryProgressSystem,
        wakeCheckpoint: WakeCheckpoint?
    ): EngineSaveSnapshot = EngineSaveSnapshot(
        player = PlayerSnapshot(player.x, player.y, player.speed),
        inventory = inventory.snapshot(),
        bunkerStorage = bunkerStorage.snapshot(),
        needs = needs.snapshot(),
        psychology = psychology.snapshot(),
        clock = worldClock.snapshot(),
        remainingResources = tileMap.resourceSnapshot(),
        zone = zone,
        zoneResources = zoneResources,
        bookLibrary = bookLibrary.snapshot(),
        radio = radioSystem.snapshot(),
        discoveredPoiIds = discoveredPoiIds,
        equipment = equipment.snapshot(),
        statistics = statistics.snapshot(),
        bunkerPower = bunkerPower.snapshot(),
        storyProgress = storyProgress.snapshot(),
        wakeCheckpoint = wakeCheckpoint
    )
}
