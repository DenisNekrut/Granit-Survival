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
        "player_back" to PixelSprite(
            frames = listOf(
                listOf(
                    "....HHHH....", "...HHHHHH...", "...HHHHHH...", "..HHHHHHHH..",
                    "..HHHHHHHH..", "..HHHHHHHH..", "..CCCCCCCC..", ".PCCCCCCCC..",
                    ".PCCCCCCCC..", ".PCCCCCCCC..", "..CCCCCCCC..", "..BBBBBBBB..",
                    "..BB..BB....", "..BB..BB....", "DD....DD....", "DD....DD...."
                ),
                listOf(
                    "....HHHH....", "...HHHHHH...", "...HHHHHH...", "..HHHHHHHH..",
                    "..HHHHHHHH..", "..HHHHHHHH..", "..CCCCCCCC..", ".PCCCCCCCC..",
                    ".PCCCCCCCC..", ".PCCCCCCCC..", "..CCCCCCCC..", "..BBBBBBBB..",
                    "...BBBB.....", "..BB..BB....", "...DD..DD...", "...DD..DD..."
                )
            ),
            palette = characterPalette
        ),
        "player_side" to PixelSprite(
            frames = listOf(
                listOf(
                    "....HHHH....", "...HHHHHH...", "...HHHHHH...", "...SSSSSS...",
                    "..SSSSSSSS..", "..SSSSSSSS..", "..CCAAAAA...", ".PCCCCCCC...",
                    ".PCCCCCCC...", ".PCCCCCCC...", "..CCCCCCC...", "..BBBBBBB...",
                    "..BB..BB....", "..BB..BB....", "...DD.DD....", "...DD.DD...."
                ),
                listOf(
                    "....HHHH....", "...HHHHHH...", "...HHHHHH...", "...SSSSSS...",
                    "..SSSSSSSS..", "..SSSSSSSS..", "..CCAAAAA...", ".PCCCCCCC...",
                    ".PCCCCCCC...", ".PCCCCCCC...", "..CCCCCCC...", "..BBBBBBB...",
                    "...BBBB.....", "..BB..BB....", "....DD.DD...", "....DD.DD..."
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
        "bandage" to PixelSprite(listOf(listOf(
            "........", "..BBBB..", ".BBBBBB.", "BB....BB",
            "BB....BB", ".BBBBBB.", "..BBBB..", "........"
        )), mapOf('B' to floatArrayOf(0.82f, 0.78f, 0.66f))),
        "bed" to PixelSprite(listOf(listOf(
            "................",
            ".WWWWWWWWWWWWWW.",
            ".WCCCCCCCCCCCCW.",
            ".WCCCCCCCCCCCCW.",
            ".WWWWWWWWWWWWWW.",
            "..WWWWWWWWWWWW..",
            "..WWWWWWWWWWWW..",
            "..WW..........W.",
            "..WW..........W.",
            "..WW..........W.",
            "..WW..........W.",
            "..WW..........W.",
            ".BBBB........BB.",
            ".BBBB........BB.",
            "................",
            "................"
        )), mapOf('W' to floatArrayOf(0.42f, 0.31f, 0.21f), 'C' to floatArrayOf(0.68f, 0.63f, 0.51f), 'B' to floatArrayOf(0.20f, 0.19f, 0.17f))),
        "workbench" to PixelSprite(listOf(listOf(
            "................",
            "................",
            "..MMMMMMMMMMMM..",
            "..MMMMMMMMMMMM..",
            "..WWWWWWWWWWWW..",
            "..WWWWWWWWWWWW..",
            "....WW....WW....",
            "....WW....WW....",
            "....WW....WW....",
            "....WW....WW....",
            "....WW....WW....",
            "....WW....WW....",
            "....WW....WW....",
            "...BBBB..BBBB...",
            "...BBBB..BBBB...",
            "................"
        )), mapOf('M' to floatArrayOf(0.55f, 0.38f, 0.23f), 'W' to floatArrayOf(0.28f, 0.24f, 0.19f), 'B' to floatArrayOf(0.16f, 0.17f, 0.16f))),
        "storage" to PixelSprite(listOf(listOf(
            "................",
            "..WWWWWWWWWWWW..",
            ".WCCCCCCCCCCCCW.",
            "WCCWWWWWWWWWWCCW",
            "WCCW........WCCW",
            "WCCW...MM...WCCW",
            "WCCW...MM...WCCW",
            "WCCW...MM...WCCW",
            "WCCW........WCCW",
            "WCCWWWWWWWWWWCCW",
            "WCCCCCCCCCCCCCCW",
            ".WCCCCCCCCCCCCW.",
            "..WWWWWWWWWWWW..",
            "................",
            "................",
            "................"
        )), mapOf('W' to floatArrayOf(0.34f, 0.25f, 0.17f), 'C' to floatArrayOf(0.49f, 0.34f, 0.20f), 'M' to floatArrayOf(0.78f, 0.65f, 0.36f))),
        "bookshelf" to PixelSprite(listOf(listOf(
            "................",
            ".WWWWWWWWWWWWWW.",
            ".WBBBBBBBBBBBBW.",
            ".WRRRGGGBBBBAAW.",
            ".WRRRGGGBBBBAAW.",
            ".W....W....W..W.",
            ".WMM....AAAA..W.",
            ".WMM....AAAA..W.",
            ".W....W....W..W.",
            ".WBBBBBBBBBBBBW.",
            ".WRRRGGGBBBBAAW.",
            ".WRRRGGGBBBBAAW.",
            ".WWWWWWWWWWWWWW.",
            "................",
            "................",
            "................"
        )), mapOf('W' to floatArrayOf(0.35f, 0.24f, 0.16f), 'B' to floatArrayOf(0.19f, 0.16f, 0.13f), 'R' to floatArrayOf(0.65f, 0.35f, 0.24f), 'G' to floatArrayOf(0.30f, 0.50f, 0.42f), 'M' to floatArrayOf(0.52f, 0.58f, 0.58f), 'A' to floatArrayOf(0.70f, 0.57f, 0.33f))),
        "radio" to PixelSprite(listOf(listOf(
            ".....AA.........",
            "....A.A.........",
            "...A..A.........",
            "..WWWWWWWWWWWW..",
            ".WCCCCCCCCCCCCW.",
            ".WCCMMMMMMCCCCW.",
            ".WCCM....MCCCCW.",
            ".WCCMMMMMMCCCCW.",
            ".WCCCCCCCCCCCCW.",
            ".WCC..AA..AA..CW",
            ".WCCCCCCCCCCCCW.",
            ".WBBBBBBBBBBBBW.",
            "..WWWWWWWWWWWW..",
            "................",
            "................",
            "................"
        )), mapOf('W' to floatArrayOf(0.25f, 0.29f, 0.27f), 'C' to floatArrayOf(0.38f, 0.43f, 0.39f), 'M' to floatArrayOf(0.12f, 0.16f, 0.15f), 'A' to floatArrayOf(0.76f, 0.59f, 0.30f), 'B' to floatArrayOf(0.18f, 0.20f, 0.18f))),
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
