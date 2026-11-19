package com.granit.survival

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

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
    private val textureVertexBuffer: FloatBuffer = allocateFloatBuffer(12)
    private val textureUvBuffer: FloatBuffer = allocateFloatBuffer(12)

    fun create() {
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

    fun resize(width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        worldScale = minOf(screenWidth / 20f, screenHeight / 12f)
        worldOffsetX = (screenWidth - 20f * worldScale) * 0.5f
        worldOffsetY = (screenHeight - 12f * worldScale) * 0.5f
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
    }

    fun render(
        engine: GameEngine,
        joystickX: Float,
        joystickY: Float,
        touching: Boolean,
        interacting: Boolean,
        controlsOpacity: Float = 0.8f,
        joystickScale: Float = 1f
    ) {
        GLES20.glClearColor(0.035f, 0.047f, 0.055f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        currentCamera = engine.camera
        queuedVertexCount = 0
        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glEnableVertexAttribArray(vertexColorLocation)

        // Each zone has its own floor tone to make transitions immediately readable.
        val floorTone = when (engine.zone) {
            WorldZone.BUNKER -> floatArrayOf(0.12f, 0.15f, 0.16f)
            WorldZone.FOREST -> floatArrayOf(0.15f, 0.19f, 0.14f)
            WorldZone.RUINS -> floatArrayOf(0.17f, 0.16f, 0.15f)
            WorldZone.ROAD -> floatArrayOf(0.13f, 0.15f, 0.18f)
        }
        rect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT,
            floorTone[0], floorTone[1], floorTone[2])
        for (x in 1 until GameEngine.WORLD_WIDTH.toInt()) {
            rect(x.toFloat(), 0f, 0.025f, GameEngine.WORLD_HEIGHT, 0.16f, 0.18f, 0.17f)
        }
        for (y in 1 until GameEngine.WORLD_HEIGHT.toInt()) {
            rect(0f, y.toFloat(), GameEngine.WORLD_WIDTH, 0.025f, 0.16f, 0.18f, 0.17f)
        }

        // Draw only non-floor tiles; the tile map is also used by movement collision.
        engine.tileMap.forEachTile { tileX, tileY, type ->
            when (type) {
                TileType.FLOOR -> drawFloorDetail(tileX, tileY, engine.zone)
                TileType.WALL -> drawZoneWall(tileX, tileY, engine.zone)
                TileType.BED -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.31f, 0.25f, 0.18f)
                    rect(tileX + 0.08f, tileY + 0.12f, 0.84f, 0.18f, 0.43f, 0.35f, 0.23f)
                    rect(tileX + 0.13f, tileY + 0.35f, 0.28f, 0.20f, 0.62f, 0.58f, 0.45f)
                    rect(tileX + 0.48f, tileY + 0.34f, 0.38f, 0.07f, 0.20f, 0.17f, 0.14f)
                }
                TileType.WORKBENCH -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.20f, 0.35f, 0.26f)
                    rect(tileX + 0.12f, tileY + 0.18f, 0.76f, 0.18f, 0.48f, 0.35f, 0.20f)
                    rect(tileX + 0.18f, tileY + 0.38f, 0.10f, 0.48f, 0.28f, 0.23f, 0.16f)
                    rect(tileX + 0.72f, tileY + 0.38f, 0.10f, 0.48f, 0.28f, 0.23f, 0.16f)
                    rect(tileX + 0.40f, tileY + 0.08f, 0.20f, 0.06f, 0.66f, 0.60f, 0.42f)
                }
                TileType.RADIO -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.18f, 0.22f, 0.20f)
                    rect(tileX + 0.10f, tileY + 0.24f, 0.80f, 0.52f, 0.28f, 0.31f, 0.27f)
                    rect(tileX + 0.18f, tileY + 0.32f, 0.34f, 0.18f, 0.13f, 0.20f, 0.19f)
                    rect(tileX + 0.62f, tileY + 0.32f, 0.08f, 0.08f, 0.72f, 0.56f, 0.30f)
                    rect(tileX + 0.62f, tileY + 0.48f, 0.08f, 0.08f, 0.72f, 0.56f, 0.30f)
                    rect(tileX + 0.20f, tileY + 0.58f, 0.60f, 0.06f, 0.10f, 0.13f, 0.12f)
                }
                TileType.BOOKSHELF -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.24f, 0.19f, 0.14f)
                    rect(tileX + 0.12f, tileY + 0.12f, 0.76f, 0.76f, 0.39f, 0.28f, 0.18f)
                    rect(tileX + 0.18f, tileY + 0.34f, 0.64f, 0.07f, 0.15f, 0.14f, 0.12f)
                    rect(tileX + 0.18f, tileY + 0.62f, 0.64f, 0.07f, 0.15f, 0.14f, 0.12f)
                    rect(tileX + 0.23f, tileY + 0.16f, 0.10f, 0.17f, 0.58f, 0.36f, 0.23f)
                    rect(tileX + 0.39f, tileY + 0.16f, 0.12f, 0.17f, 0.30f, 0.45f, 0.39f)
                    rect(tileX + 0.58f, tileY + 0.16f, 0.13f, 0.17f, 0.58f, 0.51f, 0.36f)
                    rect(tileX + 0.28f, tileY + 0.43f, 0.12f, 0.17f, 0.31f, 0.40f, 0.52f)
                    rect(tileX + 0.49f, tileY + 0.43f, 0.16f, 0.17f, 0.63f, 0.55f, 0.34f)
                }
                TileType.STORAGE -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.34f, 0.27f, 0.17f)
                    rect(tileX + 0.12f, tileY + 0.12f, 0.76f, 0.76f, 0.18f, 0.20f, 0.19f)
                    rect(tileX + 0.18f, tileY + 0.20f, 0.64f, 0.08f, 0.43f, 0.35f, 0.22f)
                    rect(tileX + 0.46f, tileY + 0.32f, 0.08f, 0.46f, 0.40f, 0.34f, 0.22f)
                    rect(tileX + 0.40f, tileY + 0.46f, 0.20f, 0.10f, 0.58f, 0.50f, 0.30f)
                }
                TileType.EXIT, TileType.EXIT_TO_BUNKER, TileType.EXIT_TO_FOREST,
                TileType.EXIT_TO_RUINS, TileType.EXIT_TO_ROAD -> drawExitTile(tileX, tileY)
                TileType.RESOURCE_WOOD -> drawResource(tileX, tileY, "wood")
                TileType.RESOURCE_METAL -> drawResource(tileX, tileY, "metal")
                TileType.RESOURCE_FABRIC -> drawResource(tileX, tileY, "fabric")
                TileType.RESOURCE_FOOD -> drawResource(tileX, tileY, "food")
                TileType.RESOURCE_WATER -> drawResource(tileX, tileY, "water")
            }
        }

        // Furniture sprites use the shared pixel atlas for a consistent art style.
        engine.tileMap.forEachTile { tileX, tileY, type ->
            val spriteId = when (type) {
                TileType.BED -> "bed"
                TileType.WORKBENCH -> "workbench"
                TileType.STORAGE -> "storage"
                TileType.BOOKSHELF -> "bookshelf"
                TileType.RADIO -> "radio"
                else -> null
            }
            if (spriteId != null) drawAtlasSprite(spriteId, tileX + 0.5f, tileY + 0.12f, 0.052f)
        }

        // Animated pixel-art character sprite selected from the logical atlas.
        val px = engine.player.x
        val py = engine.player.y
        val playerFrame = if (engine.isMoving) (engine.simulationTimeSeconds * 4.0).toInt() else 0
        when (engine.player.facing) {
            PlayerFacing.DOWN -> drawAtlasSprite("player", px, py - 0.48f, 0.06f, playerFrame)
            PlayerFacing.UP -> drawAtlasSprite("player_back", px, py - 0.48f, 0.06f, playerFrame)
            PlayerFacing.RIGHT -> drawAtlasSprite("player_side", px, py - 0.48f, 0.06f, playerFrame)
            PlayerFacing.LEFT -> drawAtlasSprite("player_side", px, py - 0.48f, 0.06f, playerFrame, flipX = true)
        }

        // Screen-space HUD reads the live simulation values.
        screenRect(0.42f, 0.28f, 4.75f, 2.72f, 0.055f, 0.067f, 0.066f)
        drawText(
            "D${engine.worldClock.day} ${engine.worldClock.hour.toString().padStart(2, '0')}:${engine.worldClock.minute.toString().padStart(2, '0')}",
            0.68f, 0.43f, 0.075f, 0.78f, 0.76f, 0.65f
        )
        val zoneLabel = when (engine.zone) {
            WorldZone.BUNKER -> "BUNKER"
            WorldZone.FOREST -> "FOREST"
            WorldZone.RUINS -> "RUINS"
            WorldZone.ROAD -> "ROAD"
        }
        drawText(zoneLabel, 0.68f, 0.80f, 0.055f, 0.57f, 0.68f, 0.60f)
        drawText("W:" + engine.weather.shortCode, 3.55f, 0.43f, 0.055f, 0.60f, 0.72f, 0.76f)
        drawNeedBar("HP", engine.needs.health, 1.05f, 0.68f, 0.24f, 0.20f)
        drawNeedBar("FO", engine.needs.hunger, 1.52f, 0.64f, 0.43f, 0.22f)
        drawNeedBar("WT", engine.needs.thirst, 1.99f, 0.25f, 0.48f, 0.66f)
        drawNeedBar("EN", engine.needs.energy, 2.46f, 0.34f, 0.55f, 0.38f)
        drawInventory(engine.inventory)
        engine.lastDiscoveryHudLabel?.let { label ->
            screenRect(5.7f, 0.42f, 8.6f, 0.72f, 0.08f, 0.12f, 0.10f, 0.94f)
            drawText("FOUND: $label", 5.95f, 0.68f, 0.075f, 0.78f, 0.76f, 0.62f)
        }

        // Virtual joystick and interaction button.
        val joystickVisualSize = 2.4f * joystickScale
        screenRect(3.2f - joystickVisualSize * 0.5f, 9.4f - joystickVisualSize * 0.5f,
            joystickVisualSize, joystickVisualSize, 0.22f, 0.25f, 0.24f, controlsOpacity)
        if (touching) {
            val radiusPx = (screenHeight * 0.19f * joystickScale).coerceAtLeast(72f)
            val thumbX = (joystickX - worldOffsetX) / worldScale
            val thumbY = (joystickY - worldOffsetY) / worldScale
            val thumbSize = (radiusPx * 0.36f) / worldScale
            screenRect(
                thumbX - thumbSize * 0.5f,
                thumbY - thumbSize * 0.5f,
                thumbSize,
                thumbSize,
                0.67f, 0.65f, 0.55f, controlsOpacity
            )
        }
        screenRect(16.8f, 9.2f, 2.2f, 2.2f,
            if (interacting) 0.48f else 0.28f,
            if (interacting) 0.55f else 0.32f,
            if (interacting) 0.42f else 0.30f, controlsOpacity)


        flushBatch()
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(vertexColorLocation)
    }

    private fun drawFloorDetail(x: Int, y: Int, zone: WorldZone) {
        val pattern = Math.floorMod(x * 7 + y * 11, 13)
        when (zone) {
            WorldZone.BUNKER -> {
                if ((x + y) % 4 == 0) rect(x + 0.08f, y + 0.92f, 0.84f, 0.025f, 0.17f, 0.19f, 0.18f)
                if (pattern == 0) rect(x + 0.20f, y + 0.24f, 0.08f, 0.05f, 0.20f, 0.22f, 0.21f)
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
                if (pattern == 0) rect(x + 0.40f, y + 0.22f, 0.06f, 0.06f, 0.27f, 0.29f, 0.30f)
            }
        }
    }

    private fun drawZoneWall(x: Int, y: Int, zone: WorldZone) {
        when (zone) {
            WorldZone.BUNKER -> {
                rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.24f, 0.26f, 0.24f)
                rect(x + 0.04f, y + 0.06f, 0.92f, 0.12f, 0.34f, 0.35f, 0.30f)
                rect(x + 0.08f, y + 0.28f, 0.84f, 0.035f, 0.17f, 0.19f, 0.18f)
                rect(x + 0.08f, y + 0.70f, 0.84f, 0.035f, 0.17f, 0.19f, 0.18f)
            }
            WorldZone.FOREST -> {
                rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.10f, 0.16f, 0.11f)
                rect(x + 0.06f, y + 0.08f, 0.88f, 0.60f, 0.18f, 0.29f, 0.17f)
                rect(x + 0.14f, y + 0.18f, 0.68f, 0.35f, 0.22f, 0.35f, 0.19f)
                rect(x + 0.42f, y + 0.58f, 0.18f, 0.40f, 0.35f, 0.23f, 0.14f)
            }
            WorldZone.RUINS -> {
                rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.20f, 0.19f, 0.18f)
                rect(x + 0.04f, y + 0.10f, 0.45f, 0.34f, 0.34f, 0.32f, 0.29f)
                rect(x + 0.48f, y + 0.22f, 0.46f, 0.28f, 0.29f, 0.28f, 0.25f)
                rect(x + 0.18f, y + 0.60f, 0.70f, 0.25f, 0.37f, 0.34f, 0.30f)
            }
            WorldZone.ROAD -> {
                rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.22f, 0.24f, 0.25f)
                rect(x + 0.04f, y + 0.16f, 0.92f, 0.68f, 0.33f, 0.35f, 0.34f)
                rect(x + 0.08f, y + 0.30f, 0.84f, 0.08f, 0.65f, 0.55f, 0.30f)
                rect(x + 0.08f, y + 0.62f, 0.84f, 0.08f, 0.65f, 0.55f, 0.30f)
            }
        }
    }

    private fun drawExitTile(x: Int, y: Int) {
        rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.12f, 0.22f, 0.23f)
        rect(x + 0.12f, y + 0.08f, 0.76f, 0.84f, 0.28f, 0.43f, 0.42f)
        rect(x + 0.22f, y + 0.18f, 0.56f, 0.64f, 0.12f, 0.20f, 0.20f)
        rect(x + 0.34f, y + 0.46f, 0.32f, 0.08f, 0.72f, 0.67f, 0.49f)
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
        flushBatch()
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
        val uvLeft = if (flipX) uv.right else uv.left
        val uvRight = if (flipX) uv.left else uv.right
        val positions = floatArrayOf(
            x1, y1, x1, y2, x2, y1,
            x2, y1, x1, y2, x2, y2
        )
        val coordinates = floatArrayOf(
            uvLeft, uv.top, uvLeft, uv.bottom, uvRight, uv.top,
            uvRight, uv.top, uvLeft, uv.bottom, uvRight, uv.bottom
        )
        textureVertexBuffer.clear()
        textureVertexBuffer.put(positions).position(0)
        textureUvBuffer.clear()
        textureUvBuffer.put(coordinates).position(0)
        GLES20.glUseProgram(textureProgram)
        GLES20.glEnableVertexAttribArray(texturePositionLocation)
        GLES20.glEnableVertexAttribArray(textureUvLocation)
        GLES20.glVertexAttribPointer(texturePositionLocation, 2, GLES20.GL_FLOAT, false, 0, textureVertexBuffer)
        GLES20.glVertexAttribPointer(textureUvLocation, 2, GLES20.GL_FLOAT, false, 0, textureUvBuffer)
        texture.bind(0)
        GLES20.glUniform1i(textureSamplerLocation, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
        GLES20.glDisableVertexAttribArray(texturePositionLocation)
        GLES20.glDisableVertexAttribArray(textureUvLocation)
        GLES20.glUseProgram(program)
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

    private fun drawNeedBar(label: String, value: Float, y: Float, r: Float, g: Float, b: Float) {
        drawText(label, 0.68f, y, 0.055f, 0.72f, 0.72f, 0.65f)
        screenRect(1.42f, y + 0.10f, 3.25f, 0.13f, 0.15f, 0.17f, 0.16f)
        val fill = 3.21f * (value.coerceIn(0f, 100f) / 100f)
        if (fill > 0f) screenRect(1.44f, y + 0.12f, fill, 0.09f, r, g, b)
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
        val panelX = 15.45f
        val panelY = 0.45f
        val slotSize = 0.72f
        val gap = 0.08f
        screenRect(panelX - 0.12f, panelY - 0.12f, 3.32f, 3.32f, 0.055f, 0.065f, 0.068f)
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

    private fun rect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float) {
        val viewX = currentCamera?.worldToViewX(x) ?: x
        val viewY = currentCamera?.worldToViewY(y) ?: y
        val left = worldOffsetX + viewX * worldScale
        val top = worldOffsetY + viewY * worldScale
        val right = worldOffsetX + (viewX + width) * worldScale
        val bottom = worldOffsetY + (viewY + height) * worldScale
        appendQuad(left, top, right, bottom, r, g, b, 1f)
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
    private companion object { const val INITIAL_VERTEX_CAPACITY = 8192 }
}
