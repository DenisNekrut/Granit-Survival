package com.granit.survival

/** Combat orchestration facade; the zombie simulation remains deterministic and headless. */
internal object CombatSystem {
    fun findAutoAimTarget(
        zone: WorldZone,
        zombies: List<ZombieActor>,
        player: PlayerState,
        map: TileMap
    ): ZombieActor? = if (zone == WorldZone.BUNKER) null
        else ZombieSystem.findVisibleTarget(zombies, player, map)

    fun firePulse(
        zombies: MutableList<ZombieActor>,
        target: ZombieActor?,
        player: PlayerState
    ) {
        if (target != null) ZombieSystem.pulseHitTarget(zombies, target)
        else ZombieSystem.pulseHit(zombies, player, player.facing)
    }

    fun update(
        zombies: MutableList<ZombieActor>,
        player: PlayerState,
        map: TileMap,
        deltaSeconds: Float
    ): Float = ZombieSystem.update(zombies, player, map, deltaSeconds)

    fun spawn(zone: WorldZone, map: TileMap, playerX: Float, playerY: Float): List<ZombieActor> =
        ZombieSystem.spawn(zone, map, playerX, playerY)
}
