package com.granit.survival

/** Lightweight code-driven pixel atlas. Frames use a shared palette and nearest-neighbour logical pixels. */
data class PixelSprite(
    val frames: List<List<String>>,
    val palette: Map<Char, FloatArray>
)

object PixelSpriteAtlas {
    private val characterPalette = mapOf(
        'H' to floatArrayOf(0.13f, 0.12f, 0.11f),
        'S' to floatArrayOf(0.68f, 0.51f, 0.37f),
        'C' to floatArrayOf(0.25f, 0.34f, 0.30f),
        'A' to floatArrayOf(0.64f, 0.48f, 0.25f),
        'P' to floatArrayOf(0.15f, 0.20f, 0.18f),
        'B' to floatArrayOf(0.28f, 0.31f, 0.29f),
        'D' to floatArrayOf(0.10f, 0.11f, 0.10f)
    )

    private val sprites = mapOf(
        "player" to PixelSprite(
            frames = listOf(
                listOf(
                    "....HHHH....", "...HHHHHH...", "...SSSSSS...", "..SSSSSSSS..",
                    "..SSSSSSSS..", "..SSSSSSSS..", "..CCAAAAAC..", ".PCCCCCCCC..",
                    ".PCCCCCCCC..", ".PCCCCCCCC..", "..CCCCCCCC..", "..BBBBBBBB..",
                    "..BB..BB....", "..BB..BB....", "DD....DD....", "DD....DD...."
                ),
                listOf(
                    "....HHHH....", "...HHHHHH...", "...SSSSSS...", "..SSSSSSSS..",
                    "..SSSSSSSS..", "..SSSSSSSS..", "..CCAAAAAC..", ".PCCCCCCCC..",
                    ".PCCCCCCCC..", ".PCCCCCCCC..", "..CCCCCCCC..", "..BBBBBBBB..",
                    "...BBBB.....", "..BB..BB....", "...DD..DD...", "...DD..DD..."
                )
            ),
            palette = characterPalette
        ),
        "wood" to PixelSprite(listOf(listOf(
            "........", "..WW....", "..WW....", "WWWWWW..",
            "..WWWW..", "..WW....", "..WW....", "........"
        )), mapOf('W' to floatArrayOf(0.66f, 0.43f, 0.24f))),
        "metal" to PixelSprite(listOf(listOf(
            "........", ".MMMMMM.", "MM....MM", "M.MMMM.M",
            "M.MMMM.M", "MM....MM", ".MMMMMM.", "........"
        )), mapOf('M' to floatArrayOf(0.64f, 0.70f, 0.71f))),
        "fabric" to PixelSprite(listOf(listOf(
            "........", ".FFFFF..", "FF.F.FF.", "F.FFF.F.",
            "FF.F.FF.", ".FFFFF..", "..F.F...", "........"
        )), mapOf('F' to floatArrayOf(0.39f, 0.58f, 0.52f))),
        "food" to PixelSprite(listOf(listOf(
            "........", "..CCCC..", ".CCCCCC.", ".C....C.",
            ".C....C.", ".CCCCCC.", "..CCCC..", "........"
        )), mapOf('C' to floatArrayOf(0.77f, 0.57f, 0.26f))),
        "water" to PixelSprite(listOf(listOf(
            "........", "...WW...", "..WWWW..", ".WWWWWW.",
            ".WWWWWW.", "..WWWW..", "...WW...", "........"
        )), mapOf('W' to floatArrayOf(0.27f, 0.59f, 0.78f)))
    )

    fun sprite(id: String): PixelSprite? = sprites[id]

    fun allIds(): Set<String> = sprites.keys

    /** Returns atlas integrity errors; an empty list means every frame is renderable. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        sprites.forEach { (id, sprite) ->
            val expectedHeight = sprite.frames.firstOrNull()?.size ?: 0
            val expectedWidth = sprite.frames.firstOrNull()?.firstOrNull()?.length ?: 0
            if (expectedHeight == 0 || expectedWidth == 0) errors += "$id has an empty frame"
            sprite.frames.forEachIndexed { frameIndex, frame ->
                if (frame.size != expectedHeight) errors += "$id frame $frameIndex has inconsistent height"
                frame.forEachIndexed { rowIndex, row ->
                    if (row.length != expectedWidth) errors += "$id frame $frameIndex row $rowIndex has inconsistent width"
                    row.filter { it != '.' && it !in sprite.palette }.toSet().forEach { symbol ->
                        errors += "$id frame $frameIndex uses undefined palette symbol $symbol"
                    }
                }
            }
        }
        return errors
    }
}
