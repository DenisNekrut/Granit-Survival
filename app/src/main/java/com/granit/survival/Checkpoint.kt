package com.granit.survival

data class PlayerSnapshot(val x: Float, val y: Float, val speed: Float)

data class WakeCheckpoint(
    val player: PlayerSnapshot,
    val inventory: List<ItemStack?>,
    val bunkerStorage: List<ItemStack?>,
    val needs: SurvivalNeedsSnapshot,
    val psychology: PsychologicalStateSnapshot = PsychologicalStateSnapshot(),
    val bookLibrary: BookLibrarySnapshot = BookLibrarySnapshot(),
    val radio: BunkerRadioSnapshot = BunkerRadioSnapshot(),
    val clock: WorldClockSnapshot,
    val remainingResources: List<ResourceLocation>,
    val zone: WorldZone,
    val zoneResources: List<ZoneResourceSnapshot> = emptyList()
)

/** Captures the complete reversible run state at wake-up. */
class CheckpointSystem {
    private var checkpoint: WakeCheckpoint? = null

    fun capture(engine: GameEngine) {
        checkpoint = WakeCheckpoint(
            player = PlayerSnapshot(engine.player.x, engine.player.y, engine.player.speed),
            inventory = engine.inventory.snapshot(),
            bunkerStorage = engine.bunkerStorage.snapshot(),
            needs = engine.needs.snapshot(),
            psychology = engine.psychology.snapshot(),
            bookLibrary = engine.bookLibrary.snapshot(),
            radio = engine.radioSystem.snapshot(),
            clock = engine.worldClock.snapshot(),
            remainingResources = engine.tileMap.resourceSnapshot(),
            zone = engine.zone,
            zoneResources = engine.zoneResourceSnapshot()
        )
    }

    fun hasCheckpoint(): Boolean = checkpoint != null

    fun snapshot(): WakeCheckpoint? = checkpoint?.deepCopy()

    fun restoreSnapshot(snapshot: WakeCheckpoint?) {
        checkpoint = snapshot?.deepCopy()
    }

    fun restore(engine: GameEngine): Boolean {
        val saved = checkpoint?.deepCopy() ?: return false
        engine.restoreZoneResources(saved.zoneResources)
        engine.changeZone(saved.zone)
        if (saved.zoneResources.isEmpty()) engine.tileMap.restoreResources(saved.remainingResources)
        engine.player.x = saved.player.x
        engine.player.y = saved.player.y
        engine.player.speed = saved.player.speed
        engine.inventory.restore(saved.inventory)
        engine.bunkerStorage.restore(saved.bunkerStorage)
        engine.needs.restore(saved.needs)
        engine.psychology.restore(saved.psychology)
        engine.bookLibrary.restore(saved.bookLibrary)
        engine.radioSystem.restore(saved.radio)
        engine.worldClock.restore(saved.clock)
        engine.tileMap.restoreResources(saved.remainingResources)
        engine.camera.follow(engine.player.x, engine.player.y, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT)
        return true
    }

    private fun WakeCheckpoint.deepCopy() = copy(
        inventory = inventory.map { it?.copy() },
        bunkerStorage = bunkerStorage.map { it?.copy() },
        remainingResources = remainingResources.toList(),
        zoneResources = zoneResources.map { it.copy(resources = it.resources.toList()) },
        bookLibrary = bookLibrary.copy(readBookIds = bookLibrary.readBookIds.toSet())
    )
}
