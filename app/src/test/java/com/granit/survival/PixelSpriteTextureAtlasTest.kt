package com.granit.survival

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the generated texture atlas layout. These checks are the reason the
 * packing rules live in [PixelSpriteTextureAtlas.plan]: the previous hardcoded
 * 16×16 cell silently rejected the 16×24 character frames, the texture build
 * failed at runtime and every sprite fell back to per-pixel CPU drawing.
 */
class PixelSpriteTextureAtlasTest {
    private val plan = PixelSpriteTextureAtlas.plan()

    @Test
    fun everySpriteIsPackedExactlyOnce() {
        assertEquals(PixelSpriteAtlas.allIds().sorted(), plan.entries.map { it.id })
        assertEquals(PixelSpriteAtlas.allIds().size, plan.entries.map { it.id }.toSet().size)
    }

    @Test
    fun cellSizeCoversTheTallestFrame() {
        val tallest = PixelSpriteAtlas.allIds()
            .map { requireNotNull(PixelSpriteAtlas.sprite(it)) }
            .maxOf { sprite -> sprite.frames.first().size }
        assertTrue(
            "Atlas cell ${plan.cellSize} must fit the tallest frame $tallest",
            plan.cellSize >= tallest
        )
        assertTrue("2x character frames must stay covered", plan.cellSize >= 48)
    }

    @Test
    fun everyFrameFitsInsideItsCell() {
        plan.entries.forEach { entry ->
            assertTrue("${entry.id} frame width ${entry.frameWidth} exceeds cell", entry.frameWidth <= plan.cellSize)
            assertTrue("${entry.id} frame height ${entry.frameHeight} exceeds cell", entry.frameHeight <= plan.cellSize)
            entry.frames.forEach { frame ->
                assertEquals("${entry.id} frames must start on a cell boundary", 0, frame.x % plan.cellSize)
                assertEquals("${entry.id} frames must start on a cell boundary", 0, frame.y % plan.cellSize)
                assertEquals(entry.frameWidth, frame.width)
                assertEquals(entry.frameHeight, frame.height)
            }
        }
    }

    @Test
    fun packedFramesOccupyDistinctCells() {
        val cells = mutableSetOf<Pair<Int, Int>>()
        plan.entries.forEach { entry ->
            entry.frames.forEach { frame ->
                val cell = (frame.x / plan.cellSize) to (frame.y / plan.cellSize)
                assertTrue("Cell $cell is used more than once", cells.add(cell))
            }
        }
        assertEquals(plan.frameCount, cells.size)
    }

    @Test
    fun textureContainsEveryPackedFrame() {
        assertTrue("Texture width ${plan.textureWidth} is too small", plan.textureWidth > 0)
        assertTrue("Texture height ${plan.textureHeight} is too small", plan.textureHeight > 0)
        val cellsPerTexture = plan.columns * plan.rows
        assertTrue(
            "Texture holds $cellsPerTexture cells but ${plan.frameCount} frames are packed",
            cellsPerTexture >= plan.frameCount
        )
        plan.entries.forEach { entry ->
            entry.frames.forEach { frame ->
                assertTrue(frame.x.toLong() + frame.width <= plan.textureWidth)
                assertTrue(frame.y.toLong() + frame.height <= plan.textureHeight)
            }
        }
    }

    @Test
    fun plannedManifestPassesAtlasValidationAndUvConversion() {
        val manifest = SpriteAtlasManifest(
            version = 1,
            texture = "generated/pixel-sprite-atlas",
            textureWidth = plan.textureWidth,
            textureHeight = plan.textureHeight,
            filter = "nearest",
            sprites = plan.entries.map { packed ->
                SpriteAtlasEntry(
                    id = packed.id,
                    frameWidth = packed.frameWidth,
                    frameHeight = packed.frameHeight,
                    frames = packed.frames
                )
            }
        )
        assertEquals(emptyList<String>(), SpriteAtlasManifestValidator.validate(manifest))

        val player = manifest.sprites.first { it.id == "player" }
        assertNotNull("Player sprite must be addressable by the renderer", player)
        player.frames.forEach { frame ->
            val uv = manifest.uvRect(frame)
            assertTrue(uv.left in 0f..1f && uv.top in 0f..1f)
            assertTrue(uv.right in 0f..1f && uv.bottom in 0f..1f)
            assertTrue(uv.right > uv.left && uv.bottom > uv.top)
        }
    }

    @Test
    fun everyInventoryIconKeepsTransparentPixelsAroundItsSilhouette() {
        val itemIds = PixelSpriteAtlas.allIds().filter { it.startsWith("item_") }
        assertTrue("Inventory item icons must be registered", itemIds.isNotEmpty())
        itemIds.forEach { id ->
            val sprite = requireNotNull(PixelSpriteAtlas.sprite(id))
            val frame = sprite.frames.first()
            val transparentPixels = frame.sumOf { row -> row.count { it == '.' } }
            val totalPixels = frame.sumOf { it.length }
            assertTrue("$id must retain transparent background pixels", transparentPixels > totalPixels / 3)
            assertTrue("$id must keep transparent corners", frame.first().first() == '.' && frame.first().last() == '.' && frame.last().first() == '.' && frame.last().last() == '.')
        }
    }

    @Test
    fun characterAndEnvironmentFramesShareOneAtlas() {
        val playerEntry = requireNotNull(plan.entry("player"))
        assertEquals(listOf(96, 96, 96, 96), playerEntry.frames.map { it.height })
        assertEquals(listOf(64, 64, 64, 64), playerEntry.frames.map { it.width })

        val treeEntry = requireNotNull(plan.entry("tree"))
        assertEquals(64, treeEntry.frameHeight)
        assertEquals(64, treeEntry.frameWidth)

        // Both frame shapes must be addressable through the same manifest.
        assertTrue(plan.entry("exit_hatch") != null)
        assertTrue(plan.entry("player_back") != null)
    }
}
