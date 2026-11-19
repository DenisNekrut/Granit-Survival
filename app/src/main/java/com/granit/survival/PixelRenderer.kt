package com.granit.survival

import android.content.res.AssetManager
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private data class WorldSpriteCommand(
    val sortY: Float,
    val sortOrder: Int,
    val spriteId: String,
    val centerX: Float,
    val topY: Float,
    val pixelSize: Float,
    val frame: Int = 0,
    val flipX: Boolean = false,
    val contactShadow: Boolean = false
)

/** Small GLES2 renderer for flat, deliberately pixel-like scene geometry. */
class PixelRenderer {
    private var program = 0
    private var positionLocation = -1
    private var vertexColorLocation = -1
    private var screenWidth = 1
    private var screenHeight = 1
    private var worldScale = 1f
    private var worldOffsetX = 0f
    private var worldOffsetY = 0f
    private var displayScale = 1f
    private var currentCamera: Camera2D? = null
    private var vertexBuffer: FloatBuffer = allocateFloatBuffer(INITIAL_VERTEX_CAPACITY * 2)
    private var colorBuffer: FloatBuffer = allocateFloatBuffer(INITIAL_VERTEX_CAPACITY * 4)
    private var queuedVertexCount = 0
    private var textureProgram = 0
    private var texturePositionLocation = -1
    private var textureUvLocation = -1
    private var textureSamplerLocation = -1
    private var spriteTexture: GlesTexture? = null
    private var spriteManifest: SpriteAtlasManifest? = null
    private val textureVertexBuffer: FloatBuffer = allocateFloatBuffer(INITIAL_TEXTURE_VERTEX_CAPACITY * 2)
    private val textureUvBuffer: FloatBuffer = allocateFloatBuffer(INITIAL_TEXTURE_VERTEX_CAPACITY * 2)
    private var queuedTextureVertexCount = 0

    fun create(assets: AssetManager? = null) {
        val atlasErrors = PixelSpriteAtlas.validate()
        check(atlasErrors.isEmpty()) { "Invalid pixel sprite atlas: ${atlasErrors.joinToString()}" }
        val vertexShader = compileShader(
            GLES20.GL_VERTEX_SHADER,
            "attribute vec2 aPosition; attribute vec4 aColor; varying vec4 vColor; void main(){ gl_Position = vec4(aPosition, 0.0, 1.0); vColor = aColor; }"
        )
        val fragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            "precision mediump float; varying vec4 vColor; void main(){ gl_FragColor = vColor; }"
        )
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            "Pixel shader link failed: " + GLES20.glGetProgramInfoLog(program)
        }
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        vertexColorLocation = GLES20.glGetAttribLocation(program, "aColor")
        check(positionLocation >= 0 && vertexColorLocation >= 0) { "Pixel shader attributes are unavailable" }

        val textureVertexShader = compileShader(
            GLES20.GL_VERTEX_SHADER,
            "attribute vec2 aPosition; attribute vec2 aTexCoord; varying vec2 vTexCoord; void main(){ gl_Position = vec4(aPosition, 0.0, 1.0); vTexCoord = aTexCoord; }"
        )
        val textureFragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            "precision mediump float; varying vec2 vTexCoord; uniform sampler2D uTexture; void main(){ gl_FragColor = texture2D(uTexture, vTexCoord); }"
        )
        textureProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(textureProgram, textureVertexShader)
        GLES20.glAttachShader(textureProgram, textureFragmentShader)
        GLES20.glLinkProgram(textureProgram)
        GLES20.glGetProgramiv(textureProgram, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            "Texture shader link failed: " + GLES20.glGetProgramInfoLog(textureProgram)
        }
        GLES20.glDeleteShader(textureVertexShader)
        GLES20.glDeleteShader(textureFragmentShader)
        texturePositionLocation = GLES20.glGetAttribLocation(textureProgram, "aPosition")
        textureUvLocation = GLES20.glGetAttribLocation(textureProgram, "aTexCoord")
        textureSamplerLocation = GLES20.glGetUniformLocation(textureProgram, "uTexture")
        check(texturePositionLocation >= 0 && textureUvLocation >= 0 && textureSamplerLocation >= 0) {
            "Texture shader inputs are unavailable"
        }

        val externalAtlas = assets?.let { assetManager ->
            runCatching { AndroidSpriteAtlasLoader(assetManager).load("sprites/atlas-manifest.json") }
                .onFailure { android.util.Log.i("GranitRenderer", "External atlas unavailable; using built-in sprites: ${it.message}") }
                .getOrNull()
        }
        if (externalAtlas != null) {
            try {
                val texture = GlesTexture()
                texture.upload(externalAtlas.bitmap, externalAtlas.manifest.filter)
                spriteManifest = externalAtlas.manifest
                spriteTexture = texture
            } finally {
                externalAtlas.bitmap.recycle()
            }
        } else {
            runCatching {
                val generatedAtlas = PixelSpriteTextureAtlas.build()
                try {
                    val texture = GlesTexture()
                    texture.upload(generatedAtlas.bitmap, generatedAtlas.manifest.filter)
                    spriteManifest = generatedAtlas.manifest
                    spriteTexture = texture
                } finally {
                    generatedAtlas.bitmap.recycle()
                }
            }.onFailure {
                android.util.Log.e("GranitRenderer", "Unable to build fallback sprite atlas", it)
            }
        }
    }

    fun destroy() {
        spriteTexture?.release()
        spriteTexture = null
        if (program != 0) GLES20.glDeleteProgram(program)
        if (textureProgram != 0) GLES20.glDeleteProgram(textureProgram)
        program = 0
        textureProgram = 0
    }

    fun resize(width: Int, height: Int, displayScale: Float = 1f) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        this.displayScale = displayScale.coerceIn(0.8f, 1.2f)
        worldScale = (screenHeight / 12f) * this.displayScale
        worldOffsetX = 0f
        worldOffsetY = 0f
        currentCamera?.setViewport(screenWidth / worldScale, screenHeight / worldScale)
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
    }

    fun render(
        engine: GameEngine,
        joystickX: Float,
        joystickY: Float,
        touching: Boolean,
        interacting: Boolean,
        firing: Boolean = false,
        controlsOpacity: Float = 0.8f,
        joystickScale: Float = 1f
    ) {
        GLES20.glClearColor(0.055f, 0.065f, 0.067f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        val cameraHeight = if (engine.zone == WorldZone.BUNKER) 12f else 9.5f
        worldScale = (screenHeight / cameraHeight) * displayScale
        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        currentCamera = engine.camera
        currentCamera?.setViewport(screenWidth / worldScale, screenHeight / worldScale)
        queuedVertexCount = 0
        queuedTextureVertexCount = 0
        textureVertexBuffer.clear()
        textureUvBuffer.clear()
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glEnableVertexAttribArray(vertexColorLocation)

        // Each zone has its own floor tone to make transitions immediately readable.
        val floorTone = when (engine.zone) {
            WorldZone.BUNKER -> floatArrayOf(0.19f, 0.21f, 0.20f)
            WorldZone.FOREST -> floatArrayOf(0.20f, 0.27f, 0.18f)
            WorldZone.RUINS -> floatArrayOf(0.25f, 0.22f, 0.19f)
            WorldZone.ROAD -> floatArrayOf(0.20f, 0.23f, 0.26f)
        }
        rect(0f, 0f, engine.tileMap.width.toFloat(), engine.tileMap.height.toFloat(),
            floorTone[0], floorTone[1], floorTone[2])
        for (x in 1 until engine.tileMap.width) {
            rect(x.toFloat(), 0f, 0.025f, engine.tileMap.height.toFloat(), 0.22f, 0.24f, 0.22f)
        }
        for (y in 1 until engine.tileMap.height) {
            rect(0f, y.toFloat(), engine.tileMap.width.toFloat(), 0.025f, 0.22f, 0.24f, 0.22f)
        }

        // Draw only non-floor tiles; the tile map is also used by movement collision.
        engine.tileMap.forEachTile { tileX, tileY, type ->
            if (engine.zone == WorldZone.BUNKER && type != TileType.WALL) drawBunkerFloor(tileX, tileY)
            when (type) {
                TileType.FLOOR -> if (engine.zone != WorldZone.BUNKER) drawFloorDetail(tileX, tileY, engine.zone) else Unit
                TileType.WALL -> Unit
                TileType.BED, TileType.WORKBENCH, TileType.RADIO, TileType.BOOKSHELF, TileType.STORAGE,
                TileType.DINING_TABLE, TileType.GENERATOR, TileType.MEDICAL_STATION -> Unit
                TileType.EXIT, TileType.EXIT_TO_BUNKER, TileType.EXIT_TO_FOREST,
                TileType.EXIT_TO_RUINS, TileType.EXIT_TO_ROAD -> drawExitTile(tileX, tileY, type)
                TileType.RESOURCE_WOOD -> drawResource(tileX, tileY, "wood")
                TileType.RESOURCE_METAL -> drawResource(tileX, tileY, "metal")
                TileType.RESOURCE_FABRIC -> drawResource(tileX, tileY, "fabric")
                TileType.RESOURCE_FOOD -> drawResource(tileX, tileY, "food")
                TileType.RESOURCE_WATER -> drawResource(tileX, tileY, "water")
            }
        }

        // Sort tall world sprites and the player by their ground-contact Y coordinate.
        // This keeps the player behind objects to the south and in front of objects to the north.
        val worldSprites = mutableListOf<WorldSpriteCommand>()
        engine.tileMap.forEachTile { tileX, tileY, type ->
            val wallSprite = if (type == TileType.WALL) when (engine.zone) {
                WorldZone.BUNKER -> "bunker_panel"
                WorldZone.FOREST -> "tree"
                WorldZone.RUINS -> "rubble"
                WorldZone.ROAD -> "road_barrier"
            } else null
            if (wallSprite != null) {
                worldSprites += WorldSpriteCommand(
                    sortY = tileY + 1f, sortOrder = 0, spriteId = wallSprite,
                    centerX = tileX + 0.5f, topY = tileY.toFloat(),
                    pixelSize = 0.0625f, contactShadow = true
                )
            }
            val furnitureSprite = when (type) {
                TileType.BED -> "bed"
                TileType.WORKBENCH -> "workbench"
                TileType.STORAGE -> "storage"
                TileType.BOOKSHELF -> "bookshelf"
                TileType.RADIO -> "radio"
                TileType.DINING_TABLE -> "dining_table"
                TileType.GENERATOR -> "generator"
                TileType.MEDICAL_STATION -> "medical_station"
                else -> null
            }
            if (furnitureSprite != null) {
                // Snap the sprite's visible edge to the nearest adjacent wall, not just its tile center.
                val furniturePixelSize = 0.052f
                val furnitureExtent = 16f * furniturePixelSize
                val anchoredX = when {
                    engine.tileMap[tileX - 1, tileY] == TileType.WALL -> tileX + furnitureExtent * 0.5f
                    engine.tileMap[tileX + 1, tileY] == TileType.WALL -> tileX + 1f - furnitureExtent * 0.5f
                    else -> tileX + 0.5f
                }
                val anchoredTopY = when {
                    engine.tileMap[tileX, tileY - 1] == TileType.WALL -> tileY.toFloat()
                    engine.tileMap[tileX, tileY + 1] == TileType.WALL -> tileY + 1f - furnitureExtent
                    else -> tileY + (1f - furnitureExtent) * 0.5f
                }
                worldSprites += WorldSpriteCommand(
                    sortY = tileY + 1.07f, sortOrder = 0, spriteId = furnitureSprite,
                    centerX = anchoredX, topY = anchoredTopY,
                    pixelSize = furniturePixelSize
                )
            }

            if (type == TileType.FLOOR) {
                val propSprite = ZonePropSelector.spriteFor(engine.zone, tileX, tileY)
                if (propSprite != null) {
                    worldSprites += WorldSpriteCommand(
                        sortY = tileY + 0.96f, sortOrder = 0, spriteId = propSprite,
                        centerX = tileX + 0.5f, topY = tileY.toFloat(),
                        pixelSize = 0.0625f, contactShadow = true
                    )
                }
            }
        }

        val px = engine.player.x
        val py = engine.player.y
        val playerFrame = if (engine.isMoving) (engine.simulationTimeSeconds * 4.0).toInt() else 0
        val (playerSprite, playerFlipX) = when (engine.player.facing) {
            PlayerFacing.DOWN -> "player" to false
            PlayerFacing.UP -> "player_back" to false
            PlayerFacing.RIGHT -> "player_side_right" to false
            PlayerFacing.LEFT -> "player_side_left" to false
        }
        worldSprites += WorldSpriteCommand(
            sortY = py + 0.48f, sortOrder = 1, spriteId = playerSprite,
            centerX = px, topY = py - 0.48f, pixelSize = 0.04f,
            frame = playerFrame, flipX = playerFlipX
        )
        // Add fixed ceiling fixtures to the bunker room layout.
        val bunkerLamps = listOf(
            3.0f to 2.0f, 8.0f to 2.0f, 14.0f to 2.0f,
            3.0f to 7.0f, 8.5f to 7.0f, 14.0f to 7.0f
        )
        // Fixtures are intentionally hidden: only their reflected light is visible on the floor and objects.
        worldSprites.sortWith(compareBy<WorldSpriteCommand> { it.sortY }.thenBy { it.sortOrder })
        worldSprites.forEach { command ->
            if (command.contactShadow) {
                rect(command.centerX - 0.38f, command.topY + 0.82f, 0.76f, 0.08f,
                    0.035f, 0.045f, 0.04f, 0.65f)
            }
            drawAtlasSprite(
                command.spriteId, command.centerX, command.topY, command.pixelSize,
                command.frame, command.flipX
            )
        }

        // Stylized infected silhouettes are simple original shapes, sorted near ground level.
        engine.zombies.forEach { zombie ->
            val z = zombie.x
            val y = zombie.y
            val flash = zombie.hitFlash > 0f
            val palette = when (zombie.variant) {
                InfectedVariant.SHAMBLER -> floatArrayOf(0.24f, 0.34f, 0.27f)
                InfectedVariant.RUNNER -> floatArrayOf(0.36f, 0.39f, 0.25f)
                InfectedVariant.BRUTE -> floatArrayOf(0.30f, 0.27f, 0.25f)
            }
            val bodyR = if (flash) 0.62f else palette[0]
            val bodyG = if (flash) 0.86f else palette[1]
            val bodyB = if (flash) 0.78f else palette[2]
            val width = if (zombie.variant == InfectedVariant.BRUTE) 0.48f else 0.40f
            val left = z - width * 0.5f
            rect(z - 0.34f, y + 0.27f, 0.68f, 0.12f, 0.035f, 0.045f, 0.04f, 0.58f)
            rect(left, y - 0.27f, width, 0.53f, bodyR, bodyG, bodyB)
            rect(z - 0.17f, y - 0.48f, 0.34f, 0.27f, 0.39f, 0.48f, 0.39f)
            rect(z - 0.31f, y - 0.13f, 0.13f, 0.43f, bodyR, bodyG, bodyB)
            rect(z + 0.18f, y - 0.12f, 0.13f, 0.42f, bodyR, bodyG, bodyB)
            rect(z - 0.17f, y + 0.21f, 0.13f, 0.29f, 0.19f, 0.24f, 0.21f)
            rect(z + 0.04f, y + 0.21f, 0.13f, 0.29f, 0.19f, 0.24f, 0.21f)
            rect(z - 0.11f, y - 0.39f, 0.055f, 0.045f, 0.86f, 0.34f, 0.25f)
            rect(z + 0.055f, y - 0.39f, 0.055f, 0.045f, 0.86f, 0.34f, 0.25f)
        }

        // Lighting is a world-space pass: outdoor light follows the clock, bunker lamps stay on.
        drawWorldLighting(engine, bunkerLamps)
        if (engine.zone != WorldZone.BUNKER) drawCorridorFog(engine)

        // The reticle exists only while a living infected is visible within seven tiles.
        engine.autoAimTarget?.let { target ->
            val cx = target.x
            val cy = target.y - 0.08f
            val colorR = 0.96f
            val colorG = 0.76f
            val colorB = 0.30f
            val arm = 0.22f
            val gap = 0.13f
            val thickness = 0.045f
            // Four open corners make the selected actor readable without obscuring it.
            rect(cx - gap - arm, cy - 0.40f, arm, thickness, colorR, colorG, colorB, 0.98f)
            rect(cx - 0.40f, cy - gap - arm, thickness, arm, colorR, colorG, colorB, 0.98f)
            rect(cx + gap, cy - 0.40f, arm, thickness, colorR, colorG, colorB, 0.98f)
            rect(cx + 0.355f, cy - gap - arm, thickness, arm, colorR, colorG, colorB, 0.98f)
            rect(cx - gap - arm, cy + 0.355f, arm, thickness, colorR, colorG, colorB, 0.98f)
            rect(cx - 0.40f, cy + gap, thickness, arm, colorR, colorG, colorB, 0.98f)
            rect(cx + gap, cy + 0.355f, arm, thickness, colorR, colorG, colorB, 0.98f)
            rect(cx + 0.355f, cy + gap, thickness, arm, colorR, colorG, colorB, 0.98f)
        }

        // Bright cyan pulse trace is a deliberately fictional energy effect.
        if (firing && engine.equipment.weaponDrawn) {
            when (engine.player.facing) {
                PlayerFacing.DOWN -> rect(px - 0.045f, py + 0.28f, 0.09f, 3.6f, 0.16f, 0.78f, 0.88f, 0.52f)
                PlayerFacing.UP -> rect(px - 0.045f, py - 3.75f, 0.09f, 3.6f, 0.16f, 0.78f, 0.88f, 0.52f)
                PlayerFacing.RIGHT -> rect(px + 0.28f, py - 0.045f, 3.6f, 0.09f, 0.16f, 0.78f, 0.88f, 0.52f)
                PlayerFacing.LEFT -> rect(px - 3.88f, py - 0.045f, 3.6f, 0.09f, 0.16f, 0.78f, 0.88f, 0.52f)
            }
        }

        // The pulse carbine is shown as a compact cyan device while outside.
        if (engine.equipment.weaponDrawn) {
            when (engine.player.facing) {
                PlayerFacing.DOWN -> {
                    rect(px + 0.22f, py + 0.10f, 0.09f, 0.25f, 0.24f, 0.34f, 0.34f)
                    rect(px + 0.22f, py + 0.28f, 0.09f, 0.07f, 0.20f, 0.72f, 0.78f)
                }
                PlayerFacing.UP -> {
                    rect(px - 0.30f, py + 0.10f, 0.09f, 0.25f, 0.24f, 0.34f, 0.34f)
                    rect(px - 0.30f, py + 0.10f, 0.09f, 0.07f, 0.20f, 0.72f, 0.78f)
                }
                PlayerFacing.RIGHT -> {
                    rect(px + 0.22f, py + 0.10f, 0.25f, 0.09f, 0.24f, 0.34f, 0.34f)
                    rect(px + 0.40f, py + 0.10f, 0.07f, 0.09f, 0.20f, 0.72f, 0.78f)
                }
                PlayerFacing.LEFT -> {
                    rect(px - 0.47f, py + 0.10f, 0.25f, 0.09f, 0.24f, 0.34f, 0.34f)
                    rect(px - 0.47f, py + 0.10f, 0.07f, 0.09f, 0.20f, 0.72f, 0.78f)
                }
            }
        }

        // Transparent HUD: all four status bars share one horizontal row at the top.
        val hudWidth = screenWidth / worldScale
        // Keep the left/right corners clear for the always-visible menu and backpack buttons.
        val hudMargin = 2.5f
        val columnWidth = (hudWidth - hudMargin * 2f) / 4f
        val barY = 0.34f
        drawNeedBar("HP", engine.needs.health, hudMargin, barY, columnWidth - 0.10f, 0.78f, 0.24f, 0.20f)
        drawNeedBar("FO", engine.needs.hunger, hudMargin + columnWidth, barY, columnWidth - 0.10f, 0.64f, 0.43f, 0.22f)
        drawNeedBar("WT", engine.needs.thirst, hudMargin + columnWidth * 2f, barY, columnWidth - 0.10f, 0.25f, 0.48f, 0.66f)
        drawNeedBar("EN", engine.needs.energy, hudMargin + columnWidth * 3f, barY, columnWidth - 0.10f, 0.34f, 0.55f, 0.38f)
        drawText("D${engine.worldClock.day} ${engine.worldClock.hour.toString().padStart(2, '0')}:${engine.worldClock.minute.toString().padStart(2, '0')}",
            hudMargin, 1.04f, 0.060f, 0.78f, 0.76f, 0.65f)
        val zoneLabel = when (engine.zone) {
            WorldZone.BUNKER -> "BUNKER"
            WorldZone.FOREST -> "FOREST"
            WorldZone.RUINS -> "RUINS"
            WorldZone.ROAD -> "ROAD"
        }
        drawText(zoneLabel, hudWidth - 5.0f, 1.04f, 0.052f, 0.57f, 0.68f, 0.60f)
        drawText("W:" + engine.weather.shortCode, hudWidth - 2.6f, 1.04f, 0.052f, 0.60f, 0.72f, 0.76f)
        if (engine.zombies.isNotEmpty()) drawText("INFECTED: ${engine.zombies.size}", hudWidth - 5.0f, 1.52f, 0.045f, 0.78f, 0.42f, 0.32f)
        if (engine.equipment.weaponDrawn) drawText("PULSE: READY", hudWidth - 2.6f, 1.52f, 0.045f, 0.35f, 0.75f, 0.76f)
        engine.lastDiscoveryHudLabel?.let { label ->
            screenRect(hudWidth * 0.34f, 1.42f, hudWidth * 0.32f, 0.52f, 0.08f, 0.12f, 0.10f, 0.88f)
            drawText("FOUND: $label", hudWidth * 0.36f, 1.56f, 0.055f, 0.78f, 0.76f, 0.62f)
        }

        // Virtual joystick and interaction button.
        val controlUnit = minOf(screenWidth, screenHeight).toFloat()
        val joystickDiameter = controlUnit * 0.19f * joystickScale
        val joystickCenterX = screenWidth * 0.145f
        val controlCenterY = screenHeight * 0.80f
        screenRectPx(joystickCenterX - joystickDiameter * 0.5f,
            controlCenterY - joystickDiameter * 0.5f, joystickDiameter, joystickDiameter,
            0.22f, 0.25f, 0.24f, controlsOpacity)
        if (touching) {
            val thumbDiameter = joystickDiameter * 0.34f
            screenRectPx(joystickX - thumbDiameter * 0.5f, joystickY - thumbDiameter * 0.5f,
                thumbDiameter, thumbDiameter, 0.67f, 0.65f, 0.55f, controlsOpacity)
        }
        val actionDiameter = controlUnit * 0.16f * joystickScale
        val actionCenterX = screenWidth * 0.855f
        screenRectPx(actionCenterX - actionDiameter * 0.5f,
            controlCenterY - actionDiameter * 0.5f, actionDiameter, actionDiameter,
            if (interacting) 0.48f else 0.28f,
            if (interacting) 0.55f else 0.32f,
            if (interacting) 0.42f else 0.30f, controlsOpacity)


        if (engine.zone != WorldZone.BUNKER) {
            val fireDiameter = controlUnit * 0.13f * joystickScale
            val fireCenterY = screenHeight * 0.59f
            screenRectPx(actionCenterX - fireDiameter * 0.5f, fireCenterY - fireDiameter * 0.5f,
                fireDiameter, fireDiameter, if (firing) 0.78f else 0.36f, if (firing) 0.52f else 0.25f, 0.20f, controlsOpacity)
        }

        flushTextureBatch()
        flushBatch()
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(vertexColorLocation)
    }

    private fun drawWorldLighting(engine: GameEngine, bunkerLamps: List<Pair<Float, Float>>) {
        val minute = engine.worldClock.minuteOfDay
        if (engine.zone != WorldZone.BUNKER) {
            val darkness = DayNightLighting.outdoorDarkness(minute)
            if (darkness > 0.001f) {
                val nightBlue = minute < 360 || minute >= 1200
                rect(0f, 0f, engine.tileMap.width.toFloat(), engine.tileMap.height.toFloat(),
                    if (nightBlue) 0.035f else 0.12f,
                    if (nightBlue) 0.055f else 0.075f,
                    if (nightBlue) 0.12f else 0.055f,
                    darkness)
            }
            val twilight = DayNightLighting.twilightTint(minute)
            if (twilight > 0.001f) {
                rect(0f, 0f, engine.tileMap.width.toFloat(), engine.tileMap.height.toFloat(),
                    0.34f, 0.19f, 0.10f, twilight)
            }
            return
        }

        // Powered pools or dim emergency strips; ceiling fixtures remain invisible.
        rect(0f, 0f, engine.tileMap.width.toFloat(), engine.tileMap.height.toFloat(),
            0.012f, 0.018f, 0.020f, if (engine.bunkerPower.hasPower) 0.48f else 0.72f)
        if (engine.bunkerPower.hasPower) {
            bunkerLamps.forEach { (x, y) ->
                val layers = listOf(
                    floatArrayOf(6.0f, 3.8f, 0.035f, 0.72f, 0.48f, 0.25f),
                    floatArrayOf(5.3f, 3.4f, 0.045f, 0.76f, 0.51f, 0.27f),
                    floatArrayOf(4.7f, 3.0f, 0.055f, 0.80f, 0.55f, 0.29f),
                    floatArrayOf(4.1f, 2.6f, 0.065f, 0.84f, 0.59f, 0.31f),
                    floatArrayOf(3.5f, 2.2f, 0.078f, 0.88f, 0.63f, 0.33f),
                    floatArrayOf(2.9f, 1.8f, 0.090f, 0.92f, 0.67f, 0.35f),
                    floatArrayOf(2.3f, 1.4f, 0.105f, 0.96f, 0.71f, 0.37f),
                    floatArrayOf(1.7f, 1.0f, 0.120f, 1.00f, 0.76f, 0.40f)
                )
                layers.forEach { layer ->
                    val width = layer[0]
                    val height = layer[1]
                    rect(x - width * 0.5f, y + 0.12f, width, height,
                        layer[3], layer[4], layer[5], layer[2])
                }
                rect(x - 0.62f, y + 0.28f, 1.24f, 0.16f, 1.0f, 0.78f, 0.47f, 0.10f)
            }
        } else {
            // Emergency strips give minimal navigation light without full room illumination.
            bunkerLamps.forEach { (x, y) ->
                rect(x - 0.75f, y + 0.10f, 1.5f, 0.55f, 0.58f, 0.20f, 0.10f, 0.075f)
                rect(x - 0.24f, y + 0.20f, 0.48f, 0.08f, 0.92f, 0.35f, 0.16f, 0.13f)
            }
        }
    }

    private fun drawBunkerFloor(x: Int, y: Int) {
        // Individual materials for all six rooms; corridors use dark steel.
        val corridor = (y == 4 && x in setOf(2, 8, 14)) ||
            (x == 5 && y in setOf(2, 7)) || (x == 11 && y in setOf(2, 7))
        val (r, g, b) = when {
            corridor -> Triple(0.12f, 0.15f, 0.15f)
            y < 4 && x < 5 -> Triple(0.20f, 0.24f, 0.27f)
            y < 4 && x < 11 -> Triple(0.29f, 0.25f, 0.18f)
            y < 4 -> Triple(0.22f, 0.25f, 0.19f)
            x < 5 -> Triple(0.17f, 0.28f, 0.27f)
            x < 11 -> Triple(0.25f, 0.25f, 0.23f)
            else -> Triple(0.28f, 0.22f, 0.16f)
        }
        rect(x + 0.02f, y + 0.02f, 0.96f, 0.96f, r, g, b)
        val seam = if (corridor) 0.08f else 0.12f
        if ((x + y) % 2 == 0) rect(x + 0.12f, y + 0.88f, 0.76f, 0.025f, seam, seam + 0.015f, seam + 0.01f)
        if ((x * 3 + y * 5) % 11 == 0) rect(x + 0.22f, y + 0.28f, 0.12f, 0.06f, r + 0.045f, g + 0.04f, b + 0.035f)
    }

    private fun drawFloorDetail(x: Int, y: Int, zone: WorldZone) {
        val pattern = Math.floorMod(x * 7 + y * 11, 13)
        when (zone) {
            WorldZone.BUNKER -> {
                if ((x + y) % 4 == 0) rect(x + 0.08f, y + 0.92f, 0.84f, 0.025f, 0.17f, 0.19f, 0.18f)
                if (pattern == 0) {
                    rect(x + 0.20f, y + 0.24f, 0.08f, 0.05f, 0.20f, 0.22f, 0.21f)
                    drawAtlasSprite("floor_grate", x + 0.5f, y + 0.12f, 0.045f)
                }
            }
            WorldZone.FOREST -> {
                if (pattern % 3 == 0) rect(x + 0.18f, y + 0.30f, 0.12f, 0.06f, 0.20f, 0.27f, 0.16f)
                if (pattern % 5 == 0) rect(x + 0.66f, y + 0.68f, 0.08f, 0.08f, 0.25f, 0.25f, 0.19f)
            }
            WorldZone.RUINS -> {
                if (pattern % 4 == 0) {
                    rect(x + 0.18f, y + 0.20f, 0.42f, 0.035f, 0.22f, 0.21f, 0.19f)
                    rect(x + 0.55f, y + 0.20f, 0.035f, 0.28f, 0.22f, 0.21f, 0.19f)
                }
            }
            WorldZone.ROAD -> {
                if (y % 5 == 0) rect(x + 0.12f, y + 0.48f, 0.76f, 0.025f, 0.18f, 0.20f, 0.22f)
                if (pattern == 0) {
                    rect(x + 0.40f, y + 0.22f, 0.06f, 0.06f, 0.27f, 0.29f, 0.30f)
                }
            }
        }
    }

    private fun drawExitTile(x: Int, y: Int, type: TileType) {
        val sprite = if (type == TileType.EXIT_TO_BUNKER) "ladder_hatch" else "exit_hatch"
        drawAtlasSprite(sprite, x + 0.5f, y.toFloat(), 0.0625f)
    }

    private fun drawAtlasSprite(
        id: String,
        centerX: Float,
        topY: Float,
        pixelSize: Float,
        frame: Int = 0,
        flipX: Boolean = false
    ) {
        val manifest = spriteManifest
        val texture = spriteTexture
        val entry = manifest?.sprites?.firstOrNull { it.id == id }
        if (manifest != null && texture != null && entry != null) {
            val frameRect = entry.frames[Math.floorMod(frame, entry.frames.size)]
            drawTexturedSprite(manifest, texture, frameRect, centerX, topY,
                entry.frameWidth * pixelSize, entry.frameHeight * pixelSize, flipX)
            return
        }
        val sprite = PixelSpriteAtlas.sprite(id) ?: return
        val sourceRows = sprite.frames[Math.floorMod(frame, sprite.frames.size)]
        val rows = if (flipX) sourceRows.map { it.reversed() } else sourceRows
        drawPixelSprite(rows, centerX, topY, pixelSize, sprite.palette)
    }

    private fun drawTexturedSprite(
        manifest: SpriteAtlasManifest,
        texture: GlesTexture,
        frame: SpriteAtlasFrame,
        centerX: Float,
        topY: Float,
        width: Float,
        height: Float,
        flipX: Boolean
    ) {
        // Flat and textured batches are flushed at boundaries to retain painter's order.
        flushBatch()
        if (queuedTextureVertexCount + 6 > textureVertexBuffer.capacity() / 2) flushTextureBatch()

        val worldLeft = centerX - width * 0.5f
        val viewX = currentCamera?.worldToViewX(worldLeft) ?: worldLeft
        val viewY = currentCamera?.worldToViewY(topY) ?: topY
        val left = worldOffsetX + viewX * worldScale
        val top = worldOffsetY + viewY * worldScale
        val right = left + width * worldScale
        val bottom = top + height * worldScale
        val x1 = toNdcX(left); val y1 = toNdcY(top)
        val x2 = toNdcX(right); val y2 = toNdcY(bottom)
        val uv = manifest.uvRect(frame)
        SpriteQuadGeometry.append(
            textureVertexBuffer, textureUvBuffer,
            x1, y1, x2, y2, uv, flipX
        )
        queuedTextureVertexCount += 6
    }

    private fun flushTextureBatch() {
        if (queuedTextureVertexCount == 0) return
        textureVertexBuffer.flip()
        textureUvBuffer.flip()
        GLES20.glUseProgram(textureProgram)
        GLES20.glEnableVertexAttribArray(texturePositionLocation)
        GLES20.glEnableVertexAttribArray(textureUvLocation)
        GLES20.glVertexAttribPointer(texturePositionLocation, 2, GLES20.GL_FLOAT, false, 0, textureVertexBuffer)
        GLES20.glVertexAttribPointer(textureUvLocation, 2, GLES20.GL_FLOAT, false, 0, textureUvBuffer)
        spriteTexture?.bind(0)
        GLES20.glUniform1i(textureSamplerLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, queuedTextureVertexCount)
        GLES20.glDisableVertexAttribArray(texturePositionLocation)
        GLES20.glDisableVertexAttribArray(textureUvLocation)
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glEnableVertexAttribArray(vertexColorLocation)
        textureVertexBuffer.clear()
        textureUvBuffer.clear()
        queuedTextureVertexCount = 0
    }

    private fun drawPixelSprite(
        rows: List<String>,
        centerX: Float,
        topY: Float,
        pixelSize: Float,
        palette: Map<Char, FloatArray>
    ) {
        val spriteWidth = rows.maxOf { it.length }
        val left = centerX - spriteWidth * pixelSize * 0.5f
        rows.forEachIndexed { rowIndex, row ->
            var column = 0
            while (column < row.length) {
                val symbol = row[column]
                if (symbol == '.') {
                    column++
                    continue
                }
                var runEnd = column + 1
                while (runEnd < row.length && row[runEnd] == symbol) runEnd++
                val color = palette[symbol] ?: floatArrayOf(0.4f, 0.4f, 0.4f)
                rect(
                    left + column * pixelSize,
                    topY + rowIndex * pixelSize,
                    (runEnd - column) * pixelSize,
                    pixelSize,
                    color[0], color[1], color[2]
                )
                column = runEnd
            }
        }
    }

    private fun drawNeedBar(label: String, value: Float, x: Float, y: Float, width: Float, r: Float, g: Float, b: Float) {
        drawText(label, x, y, 0.052f, 0.78f, 0.78f, 0.70f)
        val barX = x + 0.48f
        val barWidth = (width - 0.56f).coerceAtLeast(0.55f)
        screenRect(barX, y + 0.08f, barWidth, 0.16f, 0.15f, 0.17f, 0.16f)
        val fill = (barWidth - 0.04f) * (value.coerceIn(0f, 100f) / 100f)
        if (fill > 0f) screenRect(barX + 0.02f, y + 0.10f, fill, 0.12f, r, g, b)
    }

    /** Outdoor visibility follows corridors; solid map cells cast the fog behind them. */
    private fun drawCorridorFog(engine: GameEngine) {
        val radius = 9.5f
        val radiusSquared = radius * radius
        for (y in 0 until engine.tileMap.height) for (x in 0 until engine.tileMap.width) {
            val cx = x + 0.5f
            val cy = y + 0.5f
            val dx = cx - engine.player.x
            val dy = cy - engine.player.y
            val visible = dx * dx + dy * dy <= radiusSquared &&
                engine.tileMap.hasLineOfSight(engine.player.x, engine.player.y, cx, cy)
            if (!visible) rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.012f, 0.018f, 0.020f, 0.94f)
        }
    }

    /** Tiny 3x5 pixel font used by the HUD; keeps text crisp at the virtual resolution. */
    private fun drawText(
        text: String,
        x: Float,
        y: Float,
        pixel: Float,
        r: Float,
        g: Float,
        b: Float
    ) {
        val glyphs = mapOf(
            '0' to listOf("111", "101", "101", "101", "111"),
            '1' to listOf("010", "110", "010", "010", "111"),
            '2' to listOf("111", "001", "111", "100", "111"),
            '3' to listOf("111", "001", "111", "001", "111"),
            '4' to listOf("101", "101", "111", "001", "001"),
            '5' to listOf("111", "100", "111", "001", "111"),
            '6' to listOf("111", "100", "111", "101", "111"),
            '7' to listOf("111", "001", "010", "010", "010"),
            '8' to listOf("111", "101", "111", "101", "111"),
            '9' to listOf("111", "101", "111", "001", "111"),
            'D' to listOf("110", "101", "101", "101", "110"),
            'C' to listOf("111", "100", "100", "100", "111"),
            'L' to listOf("100", "100", "100", "100", "111"),
            'M' to listOf("101", "111", "111", "101", "101"),
            'Y' to listOf("101", "101", "010", "010", "010"),
            'G' to listOf("111", "100", "101", "101", "111"),
            'V' to listOf("101", "101", "101", "101", "010"),
            'A' to listOf("010", "101", "111", "101", "101"),
            'B' to listOf("110", "101", "110", "101", "110"),
            'I' to listOf("111", "010", "010", "010", "111"),
            'K' to listOf("101", "101", "110", "101", "101"),
            'R' to listOf("110", "101", "110", "101", "101"),
            'S' to listOf("011", "100", "010", "001", "110"),
            'U' to listOf("101", "101", "101", "101", "111"),
            'Z' to listOf("111", "001", "010", "100", "111"),
            'H' to listOf("101", "101", "111", "101", "101"),
            'P' to listOf("110", "101", "110", "100", "100"),
            'F' to listOf("111", "100", "110", "100", "100"),
            'O' to listOf("111", "101", "101", "101", "111"),
            'W' to listOf("101", "101", "101", "111", "101"),
            'T' to listOf("111", "010", "010", "010", "010"),
            'E' to listOf("111", "100", "110", "100", "111"),
            'N' to listOf("101", "111", "111", "111", "101"),
            ':' to listOf("000", "010", "000", "010", "000"),
            ' ' to listOf("000", "000", "000", "000", "000")
        )
        var cursorX = x
        text.forEach { character ->
            val glyph = glyphs[character] ?: glyphs[' ']!!
            for (row in glyph.indices) {
                for (column in glyph[row].indices) {
                    if (glyph[row][column] == '1') {
                        screenRect(
                            cursorX + column * pixel,
                            y + row * pixel,
                            pixel * 0.82f,
                            pixel * 0.82f,
                            r, g, b
                        )
                    }
                }
            }
            cursorX += pixel * 4f
        }
    }

    private fun drawResource(x: Int, y: Int, spriteId: String) {
        rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.16f, 0.18f, 0.17f)
        drawAtlasSprite(spriteId, x + 0.5f, y + 0.12f, 0.075f)
    }

    private fun drawInventory(inventory: Inventory) {
        // HUD and backpack use the current screen-space viewport, not fixed world coordinates.
        // This keeps the inventory attached to the right display edge on wide and narrow phones.
        val slotSize = 0.72f
        val gap = 0.08f
        val panelWidth = inventory.columns * slotSize + (inventory.columns - 1) * gap + 0.24f
        val viewportWidth = screenWidth / worldScale
        val panelX = (viewportWidth - panelWidth - 0.42f).coerceAtLeast(0.42f)
        val panelY = 0.45f
        screenRect(panelX - 0.12f, panelY - 0.12f, panelWidth, 3.32f, 0.055f, 0.065f, 0.068f)
        for (index in 0 until inventory.columns * inventory.rows) {
            val column = index % inventory.columns
            val row = index / inventory.columns
            val x = panelX + column * (slotSize + gap)
            val y = panelY + row * (slotSize + gap)
            screenRect(x, y, slotSize, slotSize, 0.15f, 0.17f, 0.17f)
            val stack = inventory.slot(index) ?: continue
            val color = when (stack.type) {
                ItemType.WOOD -> floatArrayOf(0.60f, 0.39f, 0.22f)
                ItemType.METAL -> floatArrayOf(0.56f, 0.62f, 0.64f)
                ItemType.FABRIC -> floatArrayOf(0.38f, 0.55f, 0.50f)
                ItemType.CANNED_FOOD -> floatArrayOf(0.73f, 0.53f, 0.24f)
                ItemType.WATER -> floatArrayOf(0.26f, 0.55f, 0.74f)
                ItemType.MEDICAL_KIT -> floatArrayOf(0.72f, 0.30f, 0.28f)
                ItemType.BANDAGE -> floatArrayOf(0.82f, 0.78f, 0.66f)
            }
            if (stack.type == ItemType.MEDICAL_KIT || stack.type == ItemType.BANDAGE) {
                screenRect(x + 0.30f, y + 0.16f, 0.12f, 0.40f, color[0], color[1], color[2])
                screenRect(x + 0.16f, y + 0.30f, 0.40f, 0.12f, color[0], color[1], color[2])
            } else {
                screenRect(x + 0.14f, y + 0.14f, 0.44f, 0.44f, color[0], color[1], color[2])
            }
            val quantityWidth = 0.12f + 0.42f * (stack.quantity / 20f)
            screenRect(x + 0.14f, y + 0.62f, quantityWidth, 0.045f, 0.78f, 0.75f, 0.62f)
        }
    }

    private fun rect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float, alpha: Float = 1f) {
        val viewX = currentCamera?.worldToViewX(x) ?: x
        val viewY = currentCamera?.worldToViewY(y) ?: y
        val left = worldOffsetX + viewX * worldScale
        val top = worldOffsetY + viewY * worldScale
        val right = worldOffsetX + (viewX + width) * worldScale
        val bottom = worldOffsetY + (viewY + height) * worldScale
        appendQuad(left, top, right, bottom, r, g, b, alpha.coerceIn(0f, 1f))
    }

    private fun screenRectPx(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float, alpha: Float = 1f) {
        appendQuad(x, y, x + width, y + height, r, g, b, alpha.coerceIn(0f, 1f))
    }

    private fun screenRect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float, alpha: Float = 1f) {
        val left = worldOffsetX + x * worldScale
        val top = worldOffsetY + y * worldScale
        val right = worldOffsetX + (x + width) * worldScale
        val bottom = worldOffsetY + (y + height) * worldScale
        appendQuad(left, top, right, bottom, r, g, b, alpha.coerceIn(0f, 1f))
    }

    /** Adds two triangles to the frame batch without issuing a draw call. */
    private fun appendQuad(left: Float, top: Float, right: Float, bottom: Float, r: Float, g: Float, b: Float, a: Float) {
        flushTextureBatch()
        ensureBatchCapacity(queuedVertexCount + 6)
        val x1 = toNdcX(left); val y1 = toNdcY(top)
        val x2 = toNdcX(right); val y2 = toNdcY(bottom)
        vertexBuffer.put(x1).put(y1).put(x1).put(y2).put(x2).put(y1)
        vertexBuffer.put(x2).put(y1).put(x1).put(y2).put(x2).put(y2)
        repeat(6) { colorBuffer.put(r).put(g).put(b).put(a) }
        queuedVertexCount += 6
    }

    private fun ensureBatchCapacity(requiredVertices: Int) {
        if (requiredVertices <= vertexBuffer.capacity() / 2) return
        var capacity = vertexBuffer.capacity() / 2
        while (capacity < requiredVertices) capacity *= 2
        val nextVertices = allocateFloatBuffer(capacity * 2)
        val nextColors = allocateFloatBuffer(capacity * 4)
        vertexBuffer.flip(); colorBuffer.flip()
        nextVertices.put(vertexBuffer); nextColors.put(colorBuffer)
        vertexBuffer = nextVertices; colorBuffer = nextColors
    }

    private fun flushBatch() {
        if (queuedVertexCount == 0) return
        vertexBuffer.flip(); colorBuffer.flip()
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glVertexAttribPointer(vertexColorLocation, 4, GLES20.GL_FLOAT, false, 0, colorBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, queuedVertexCount)
        vertexBuffer.clear(); colorBuffer.clear(); queuedVertexCount = 0
    }

    private fun allocateFloatBuffer(floatCount: Int): FloatBuffer =
        ByteBuffer.allocateDirect(floatCount * java.lang.Float.BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()
    private fun toNdcX(x: Float) = (x / screenWidth) * 2f - 1f
    private fun toNdcY(y: Float) = 1f - (y / screenHeight) * 2f

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            "Pixel shader compile failed: " + GLES20.glGetShaderInfoLog(shader)
        }
        return shader
    }
    private companion object { const val INITIAL_VERTEX_CAPACITY = 8192; const val INITIAL_TEXTURE_VERTEX_CAPACITY = 4096 }
}
