package com.granit.survival

import kotlin.math.abs
import kotlin.math.sqrt

/** Stylized fictional infected NPC; combat remains abstract and game-only. */
enum class InfectedVariant(
    val health: Float,
    val moveSpeed: Float,
    val contactDamage: Float,
    val attackInterval: Float
) {
    SHAMBLER(8f, 0.72f, 3f, 1.8f),
    RUNNER(6f, 1.08f, 2f, 1.35f),
    BRUTE(12f, 0.48f, 4f, 2.2f)
}

data class ZombieActor(
    var x: Float,
    var y: Float,
    var health: Float = 8f,
    var attackCooldown: Float = 0f,
    var hitFlash: Float = 0f,
    val variant: InfectedVariant = InfectedVariant.SHAMBLER
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
        val count = when (zone) {
            WorldZone.FOREST -> 3
            WorldZone.RUINS -> 4
            WorldZone.ROAD -> 3
            WorldZone.BUNKER -> 0
        }
        val variants = when (zone) {
            WorldZone.FOREST -> listOf(InfectedVariant.SHAMBLER, InfectedVariant.RUNNER, InfectedVariant.SHAMBLER)
            WorldZone.RUINS -> listOf(InfectedVariant.SHAMBLER, InfectedVariant.BRUTE, InfectedVariant.RUNNER, InfectedVariant.SHAMBLER)
            WorldZone.ROAD -> listOf(InfectedVariant.RUNNER, InfectedVariant.SHAMBLER, InfectedVariant.BRUTE)
            WorldZone.BUNKER -> emptyList()
        }
        return candidates.take(count).mapIndexed { index, point ->
            val variant = variants[index % variants.size]
            ZombieActor(point.first, point.second, health = variant.health, variant = variant)
        }.toMutableList()
    }

    fun update(
        zombies: MutableList<ZombieActor>,
        player: PlayerState,
        map: TileMap,
        deltaSeconds: Float
    ): Float {
        var contactDamage = 0f
        zombies.forEach { zombie ->
            zombie.attackCooldown = (zombie.attackCooldown - deltaSeconds).coerceAtLeast(0f)
            zombie.hitFlash = (zombie.hitFlash - deltaSeconds).coerceAtLeast(0f)
            val dx = player.x - zombie.x
            val dy = player.y - zombie.y
            val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            if (distance > CONTACT_DISTANCE) {
                val step = zombie.variant.moveSpeed * deltaSeconds
                val stepX = dx / distance * step
                val stepY = dy / distance * step
                // Try the dominant axis first, then the other axis; this avoids many
                // corner-sticking cases without introducing expensive pathfinding.
                if (abs(dx) >= abs(dy)) {
                    if (map.isWalkable(zombie.x + stepX, zombie.y)) zombie.x += stepX
                    else if (map.isWalkable(zombie.x, zombie.y + stepY)) zombie.y += stepY
                } else {
                    if (map.isWalkable(zombie.x, zombie.y + stepY)) zombie.y += stepY
                    else if (map.isWalkable(zombie.x + stepX, zombie.y)) zombie.x += stepX
                }
            } else if (zombie.attackCooldown <= 0f) {
                zombie.attackCooldown = zombie.variant.attackInterval
                contactDamage += zombie.variant.contactDamage
            }
        }
        return contactDamage
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
            val side = abs(dx * direction.second - dy * direction.first)
            if (forward in 0f..5f && side <= 0.85f) zombie to distanceSquared else null
        }.minByOrNull { it.second }?.first ?: return null
        target.health -= 4f
        target.hitFlash = 0.18f
        if (target.health <= 0f) zombies.remove(target)
        return target
    }
}
