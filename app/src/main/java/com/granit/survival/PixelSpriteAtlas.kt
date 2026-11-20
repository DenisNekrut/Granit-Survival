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
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...SSSSSS...",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..CCAAAAAC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    "..CCCCCCCC..",
                    "..BBBBBBBB..",
                    "..BB..BB....",
                    "...BBBB.....",
                    "DD....DD....",
                    "DD....DD...."
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...SSSSSS...",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..CCAAAAAC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    "..CCCCCCCC..",
                    "..BBBBBBBB..",
                    "...BBBB.....",
                    "..BB..BB....",
                    "...DD..DD...",
                    "...DD..DD..."
                ),
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
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...HHHHHH...",
                    "..HHHHHHHH..",
                    "..HHHHHHHH..",
                    "..HHHHHHHH..",
                    "..CCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    "..CCCCCCCC..",
                    "..BBBBBBBB..",
                    "..BB..BB....",
                    "...BBBB.....",
                    "DD....DD....",
                    "DD....DD...."
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...HHHHHH...",
                    "..HHHHHHHH..",
                    "..HHHHHHHH..",
                    "..HHHHHHHH..",
                    "..CCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    ".PCCCCCCCC..",
                    "..CCCCCCCC..",
                    "..BBBBBBBB..",
                    "...BBBB.....",
                    "..BB..BB....",
                    "...DD..DD...",
                    "...DD..DD..."
                ),
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
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...HHHHHH...",
                    "...SSSSSS...",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..CCAAAAA...",
                    ".PCCCCCCC...",
                    ".PCCCCCCC...",
                    ".PCCCCCCC...",
                    "..CCCCCCC...",
                    "..BBBBBBB...",
                    "..BB..BB....",
                    "...BBBB.....",
                    "...DD.DD....",
                    "...DD.DD...."
                ),
                listOf(
                    "....HHHH....",
                    "...HHHHHH...",
                    "...HHHHHH...",
                    "...SSSSSS...",
                    "..SSSSSSSS..",
                    "..SSSSSSSS..",
                    "..CCAAAAA...",
                    ".PCCCCCCC...",
                    ".PCCCCCCC...",
                    ".PCCCCCCC...",
                    "..CCCCCCC...",
                    "..BBBBBBB...",
                    "...BBBB.....",
                    "..BB..BB....",
                    "....DD.DD...",
                    "....DD.DD..."
                ),
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
        "tree" to PixelSprite(listOf(listOf(
            ".......GG.......",
            ".....GGGGGG.....",
            "....GGGGGGGG....",
            "...GGGGGGGGGG...",
            "..GGGGGGGGGGGG..",
            ".GGGGGGGGGGGGGG.",
            ".GGGGDDGGGGGGGG.",
            "..GGGGGGGGGGGG..",
            "...GGGGGGGGGG...",
            "....GGGGGGGG....",
            ".....GGGGGG.....",
            ".......TT.......",
            ".......TT.......",
            "......TTTT......",
            "......TTTT......",
            "................"
        )), mapOf(
            'G' to floatArrayOf(0.18f, 0.31f, 0.20f),
            'D' to floatArrayOf(0.12f, 0.23f, 0.16f),
            'T' to floatArrayOf(0.37f, 0.25f, 0.16f)
        )),
        "rubble" to PixelSprite(listOf(listOf(
            "................",
            "....MMMM........",
            "...MMMMMM.......",
            "..MM..MMMM......",
            ".MMMMMMMMMM.....",
            ".MMMMMMMMMMMM...",
            "..MMMMMMMMMMMM..",
            "...MMMMMMMMMM...",
            "....MMMMMMMM....",
            "..MM..MMMM......",
            ".MMMMMMMM.......",
            ".MMMMMMM........",
            "..MMMM..........",
            "................",
            "................",
            "................"
        )), mapOf('M' to floatArrayOf(0.39f, 0.38f, 0.35f))),
        "road_barrier" to PixelSprite(listOf(listOf(
            "................",
            "................",
            "..RRRRRRRRRRRR..",
            "..RWWRRWWRRWWR..",
            "..RRWWRRWWRRWW..",
            "..RWWRRWWRRWWR..",
            "..RRRRRRRRRRRR..",
            "....BBBBBBBB....",
            "....BB....BB....",
            "....BB....BB....",
            "...BBBB..BBBB...",
            "................",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'R' to floatArrayOf(0.62f, 0.25f, 0.19f),
            'W' to floatArrayOf(0.72f, 0.68f, 0.55f),
            'B' to floatArrayOf(0.20f, 0.22f, 0.21f)
        )),
        "bunker_panel" to PixelSprite(listOf(listOf(
            "MMMMMMMMMMMMMMMM",
            "MBBBBBBBBBBBBBBM",
            "MBMMMMMMMMMMMMBM",
            "MBM..........MBM",
            "MBM..........MBM",
            "MBM....MMMM..MBM",
            "MBM....MMMM..MBM",
            "MBM..........MBM",
            "MBM..........MBM",
            "MBMMMMMMMMMMMMBM",
            "MBBBBBBBBBBBBBBM",
            "MMMMMMMMMMMMMMMM",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'M' to floatArrayOf(0.35f, 0.39f, 0.37f),
            'B' to floatArrayOf(0.16f, 0.19f, 0.18f)
        )),
        "bush" to PixelSprite(listOf(listOf(
            "................",
            "................",
            ".....GGGG.......",
            "...GGGGGGGG.....",
            "..GGGGGGGGGG....",
            ".GGGGDDGGGGGG...",
            ".GGGGGGGGGGGG...",
            "..GGGGGGGGGG....",
            "...GGGGGGGG.....",
            ".....GGGG.......",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'G' to floatArrayOf(0.22f, 0.34f, 0.19f),
            'D' to floatArrayOf(0.14f, 0.25f, 0.15f)
        )),
        "debris" to PixelSprite(listOf(listOf(
            "................",
            "................",
            "...MM...........",
            "..MMMM..........",
            ".MMMMM....MM....",
            "..MMMM...MMMM...",
            "...MM...MMMMM...",
            "........MMMM....",
            "....MM...MM.....",
            "...MMMM.........",
            "..MMMMM.........",
            "...MMM..........",
            "................",
            "................",
            "................",
            "................"
        )), mapOf('M' to floatArrayOf(0.43f, 0.41f, 0.37f))),
        "streetlamp" to PixelSprite(listOf(listOf(
            "......MMMM......",
            ".....MMMMMM.....",
            "......MMMM......",
            ".........M......",
            ".........M......",
            ".........M......",
            ".........M......",
            ".........M......",
            ".........M......",
            ".........M......",
            ".........M......",
            "........MMM.....",
            ".......MMMMM....",
            "................",
            "................",
            "................"
        )), mapOf('M' to floatArrayOf(0.48f, 0.48f, 0.42f))),
        "floor_grate" to PixelSprite(listOf(listOf(
            "................",
            "................",
            "..MMMMMMMMMMMM..",
            "..MBBM..MBBM..M.",
            "..MBBM..MBBM..M.",
            "..MMMMMMMMMMMM..",
            "..MBBM..MBBM..M.",
            "..MBBM..MBBM..M.",
            "..MMMMMMMMMMMM..",
            "..MBBM..MBBM..M.",
            "..MBBM..MBBM..M.",
            "..MMMMMMMMMMMM..",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'M' to floatArrayOf(0.35f, 0.39f, 0.37f),
            'B' to floatArrayOf(0.12f, 0.15f, 0.14f)
        )),
        "crate" to PixelSprite(listOf(listOf(
            "................",
            "..WWWWWWWWWWWW..",
            "..WBBBBBBBBBBW..",
            "..WBWWWWWWWWBW..",
            "..WBW......WBW..",
            "..WBW......WBW..",
            "..WBWWWWWWWWBW..",
            "..WBBBBBBBBBBW..",
            "..WWWWWWWWWWWW..",
            "..WW..........W.",
            "..WW..........W.",
            "..WW..........W.",
            "..WW..........W.",
            "................",
            "................",
            "................"
        )), mapOf(
            'W' to floatArrayOf(0.39f, 0.27f, 0.17f),
            'B' to floatArrayOf(0.19f, 0.16f, 0.13f)
        )),
        "barrel" to PixelSprite(listOf(listOf(
            "................",
            ".....MMMMMM.....",
            "....MMMMMMMM....",
            "...MMBBBBBBMM...",
            "...MMBBBBBBMM...",
            "...MMBBBBBBMM...",
            "...MMMMMMMMMM...",
            "...MMBBBBBBMM...",
            "...MMBBBBBBMM...",
            "...MMBBBBBBMM...",
            "....MMMMMMMM....",
            ".....MMMMMM.....",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'M' to floatArrayOf(0.46f, 0.49f, 0.46f),
            'B' to floatArrayOf(0.30f, 0.34f, 0.32f)
        )),
        "wreck" to PixelSprite(listOf(listOf(
            "................",
            ".....MMMMMM.....",
            "...MMMMMMMMMM...",
            "..MMBBBBBBBBMM..",
            ".MMBBBBBBBBBBMM.",
            ".MMBBMMMMBBBBMM.",
            ".MMMMMMMMMMMMMM.",
            ".MMBBBBBBBBBBMM.",
            "..MMBBBBBBBBMM..",
            "...MM....MM.....",
            "...MM....MM.....",
            "................",
            "................",
            "................",
            "................",
            "................"
        )), mapOf(
            'M' to floatArrayOf(0.34f, 0.37f, 0.35f),
            'B' to floatArrayOf(0.19f, 0.23f, 0.21f)
        )),
        "water" to PixelSprite(listOf(listOf(
            "........", "...WW...", "..WWWW..", ".WWWWWW.",
            ".WWWWWW.", "..WWWW..", "...WW...", "........"
        )), mapOf('W' to floatArrayOf(0.27f, 0.59f, 0.78f)))
    ) + GeneratedPixelSprites.all + mapOf(
        "grass_patch" to PixelSprite(listOf(listOf(
            "................", "................", "................", "......GG........",
            "...GGGGGG.......", "..GGGGGGGG......", ".GGGGGGGGGG.....", "..GGGGGGGG......",
            "...GGGGGG.......", ".....GG.........", "................", "................",
            "................", "................", "................", "................"
        )), mapOf(
            'G' to floatArrayOf(0.30f, 0.39f, 0.22f)
        )),
        "fallen_log" to PixelSprite(listOf(listOf(
            "................", "................", "................", "................",
            "....BBBB........", "...BBBBBB.......", "..BBBBBBBB......", ".BBBBBBBBBB.....",
            ".BBWWWWWWBB.....", "..BBBBBBBB......", "...BBBBBB.......", "....BBBB........",
            "................", "................", "................", "................"
        )), mapOf(
            'B' to floatArrayOf(0.32f, 0.23f, 0.15f),
            'W' to floatArrayOf(0.48f, 0.36f, 0.24f)
        )),
        "metal_scrap" to PixelSprite(listOf(listOf(
            "................", "................", "...MMMM.........", "..MMMMMM........",
            ".MMBBMMMM.......", "..MMMMMMMM......", "...MMMMBBMM.....", "....MMMMMMM.....",
            "..MMBBMMMM......", ".MMMMMMMM.......", "..MMMM..........", "................",
            "................", "................", "................", "................"
        )), mapOf(
            'M' to floatArrayOf(0.43f, 0.47f, 0.45f),
            'B' to floatArrayOf(0.22f, 0.27f, 0.25f)
        )),
        "puddle" to PixelSprite(listOf(listOf(
            "................", "................", "................", "................",
            ".....WWWW.......", "...WWWWWWWW.....", "..WWWWWWWWWW....", ".WWWWWWWWWWWW...",
            "..WWWWWWWWWW....", "...WWWWWWWW.....", ".....WWWW.......", "................",
            "................", "................", "................", "................"
        )), mapOf(
            'W' to floatArrayOf(0.24f, 0.43f, 0.49f)
        )),
        "road_sign" to PixelSprite(listOf(listOf(
            "................", ".....MMMMMM.....", ".....MWWWWM.....", ".....MWWWWM.....",
            ".....MMMMMM.....", "........M.......", "........M.......", "........M.......",
            "........M.......", "........M.......", ".......MMM......", "......MMMMM.....",
            "................", "................", "................", "................"
        )), mapOf(
            'M' to floatArrayOf(0.38f, 0.40f, 0.37f),
            'W' to floatArrayOf(0.63f, 0.59f, 0.43f)
        ))
    )

    /**
     * Hand-authored style variants are derived from the original pixel silhouettes.
     * Small contour offsets and independent material grading prevent repeated props
     * from looking like exact copies while keeping the same collision footprint.
     */
    private fun createVariant(
        baseId: String,
        mirror: Boolean = false,
        rowShift: Int = 0,
        redScale: Float = 1f,
        greenScale: Float = 1f,
        blueScale: Float = 1f,
        lift: Float = 0f
    ): PixelSprite {
        val source = requireNotNull(sprites[baseId]) { "Missing base sprite $baseId" }
        val frames = source.frames.map { frame ->
            frame.mapIndexed { rowIndex, row ->
                val oriented = if (mirror) row.reversed() else row
                val shift = if (rowIndex % 3 == 1) rowShift else 0
                when {
                    shift > 0 -> ".".repeat(shift) + oriented.dropLast(shift)
                    shift < 0 -> oriented.drop(-shift) + ".".repeat(-shift)
                    else -> oriented
                }
            }
        }
        val palette = source.palette.mapValues { (_, color) ->
            floatArrayOf(
                (color[0] * redScale + lift).coerceIn(0f, 1f),
                (color[1] * greenScale + lift).coerceIn(0f, 1f),
                (color[2] * blueScale + lift).coerceIn(0f, 1f)
            )
        }
        return PixelSprite(frames, palette)
    }

    private val variants: Map<String, PixelSprite> by lazy {
        mapOf(
            "tree_autumn" to createVariant("tree", mirror = true, rowShift = 1, redScale = 1.18f, greenScale = 0.88f, blueScale = 0.72f, lift = 0.015f),
            "tree_dead" to createVariant("tree", rowShift = -1, redScale = 0.78f, greenScale = 0.82f, blueScale = 0.86f),
            "tree_dark" to createVariant("tree", mirror = true, redScale = 0.76f, greenScale = 0.91f, blueScale = 0.80f),
            "bush_dry" to createVariant("bush", mirror = true, rowShift = 1, redScale = 1.15f, greenScale = 0.96f, blueScale = 0.72f, lift = 0.02f),
            "bush_lush" to createVariant("bush", rowShift = -1, redScale = 0.82f, greenScale = 1.16f, blueScale = 0.78f),
            "crate_weathered" to createVariant("crate", mirror = true, rowShift = 1, redScale = 0.88f, greenScale = 0.82f, blueScale = 0.72f),
            "crate_painted" to createVariant("crate", rowShift = -1, redScale = 0.76f, greenScale = 1.08f, blueScale = 1.14f, lift = 0.015f),
            "barrel_rust" to createVariant("barrel", mirror = true, redScale = 1.18f, greenScale = 0.82f, blueScale = 0.72f, lift = 0.015f),
            "barrel_dark" to createVariant("barrel", rowShift = 1, redScale = 0.72f, greenScale = 0.78f, blueScale = 0.76f),
            "debris_light" to createVariant("debris", mirror = true, rowShift = 1, redScale = 1.12f, greenScale = 1.08f, blueScale = 1.02f, lift = 0.025f),
            "rubble_brick" to createVariant("rubble", mirror = true, rowShift = -1, redScale = 1.20f, greenScale = 0.86f, blueScale = 0.76f, lift = 0.01f),
            "metal_scrap_copper" to createVariant("metal_scrap", mirror = true, rowShift = 1, redScale = 1.20f, greenScale = 0.91f, blueScale = 0.72f, lift = 0.01f),
            "puddle_ice" to createVariant("puddle", mirror = true, redScale = 0.82f, greenScale = 1.05f, blueScale = 1.18f, lift = 0.025f),
            "wreck_rust" to createVariant("wreck", mirror = true, rowShift = 1, redScale = 1.18f, greenScale = 0.84f, blueScale = 0.74f, lift = 0.012f),
            "wreck_blue" to createVariant("wreck", rowShift = -1, redScale = 0.78f, greenScale = 0.98f, blueScale = 1.18f),
            "road_sign_faded" to createVariant("road_sign", mirror = true, rowShift = 1, redScale = 0.88f, greenScale = 0.92f, blueScale = 0.90f, lift = 0.04f),
            "fallen_log_mossy" to createVariant("fallen_log", mirror = true, rowShift = -1, redScale = 0.82f, greenScale = 1.12f, blueScale = 0.78f),
            "grass_patch_dry" to createVariant("grass_patch", mirror = true, rowShift = 1, redScale = 1.14f, greenScale = 0.94f, blueScale = 0.70f, lift = 0.02f),
            "streetlamp_broken" to createVariant("streetlamp", mirror = true, rowShift = 1, redScale = 0.82f, greenScale = 0.86f, blueScale = 0.84f),
            "floor_grate_rust" to createVariant("floor_grate", mirror = true, redScale = 1.16f, greenScale = 0.88f, blueScale = 0.72f, lift = 0.01f)
        )
    }

    fun sprite(id: String): PixelSprite? = sprites[id] ?: variants[id]

    fun allIds(): Set<String> = sprites.keys + variants.keys

    fun allIds(): Set<String> = sprites.keys

    /** Returns atlas integrity errors; an empty list means every frame is renderable. */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        allIds().forEach { id ->
            val sprite = requireNotNull(sprite(id))
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
