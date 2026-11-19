package com.granit.survival

import kotlin.math.sqrt

/** Stylized fictional infected NPC; no real-world weapon or injury simulation. */
data class ZombieActor(
    var x: Float,
    var y: Float,
    var health: Float = 8f,
    var attackCooldown: Float = 0f,
    var hitFlash: Float = 0f
)

object ZombieSystem {
    const val CONTACT_DISTANCE = 0.78f
    const val MOVE_SPEED = 0.72f
    const val CONTACT_DAMAGE = 3f
    const val ATTACK_INTERVAL_SECONDS = 1.8f

    fun spawn(zone: WorldZone, map: TileMap, spawnX: Float, spawnY: Float): MutableList<ZombieActor> {
        if (zone == WorldZone.BUNKER) return mutableListOf()
        val candidates = buildList {
            for (y in 2 until map.height - 2) for (x in 2 until map.width - 2) {
                if (map.get(x, y) != TileType.FLOOR) continue
                val dx = x + 0.5f - spawnX
                val dy = y + 0.5f - spawnY
                if (dx * dx + dy * dy >= 36f) add(x + 0.5f to y + 0.5f)
            }
        }.sortedBy { (x, y) -> (x.toInt() * 31 + y.toInt() * 17 + zone.ordinal * 13) % 97 }
        val count = if (zone == WorldZone.FOREST) 2 else 3
        return candidates.take(count).map { ZombieActor(it.first, it.second) }.toMutableList()
    }

    fun update(
        zombies: MutableList<ZombieActor>,
        player: PlayerState,
        map: TileMap,
        deltaSeconds: Float,
        onContact: (Float) -> Unit
    ) {
        zombies.forEach { zombie ->
            zombie.attackCooldown = (zombie.attackCooldown - deltaSeconds).coerceAtLeast(0f)
            zombie.hitFlash = (zombie.hitFlash - deltaSeconds).coerceAtLeast(0f)
            val dx = player.x - zombie.x
            val dy = player.y - zombie.y
            val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            if (distance > CONTACT_DISTANCE) {
                val step = MOVE_SPEED * deltaSeconds
                val nextX = zombie.x + dx / distance * step
                val nextY = zombie.y + dy / distance * step
                if (map.isWalkable(nextX, zombie.y)) zombie.x = nextX
                if (map.isWalkable(zombie.x, nextY)) zombie.y = nextY
            } else if (zombie.attackCooldown <= 0f) {
                zombie.attackCooldown = ATTACK_INTERVAL_SECONDS
                onContact(CONTACT_DAMAGE)
            }
        }
    }

    fun pulseHit(zombies: MutableList<ZombieActor>, player: PlayerState, facing: PlayerFacing): ZombieActor? {
        val direction = when (facing) {
            PlayerFacing.UP -> 0f to -1f
            PlayerFacing.DOWN -> 0f to 1f
            PlayerFacing.LEFT -> -1f to 0f
            PlayerFacing.RIGHT -> 1f to 0f
        }
        val target = zombies.mapNotNull { zombie ->
            val dx = zombie.x - player.x
            val dy = zombie.y - player.y
            val distanceSquared = dx * dx + dy * dy
            val forward = dx * direction.first + dy * direction.second
            val side = kotlin.math.abs(dx * direction.second - dy * direction.first)
            if (forward in 0f..5f && side <= 0.85f) zombie to distanceSquared else null
        }.minByOrNull { it.second }?.first ?: return null
        target.health -= 4f
        target.hitFlash = 0.18f
        if (target.health <= 0f) zombies.remove(target)
        return target
    }
}
