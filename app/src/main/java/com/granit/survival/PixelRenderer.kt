package com.granit.survival

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Small GLES2 renderer for flat, deliberately pixel-like scene geometry. */
class PixelRenderer {
    private var program = 0
    private var positionLocation = -1
    private var colorLocation = -1
    private var screenWidth = 1
    private var screenHeight = 1
    private var worldScale = 1f
    private var worldOffsetX = 0f
    private var worldOffsetY = 0f
    private var currentCamera: Camera2D? = null
    private val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer()

    fun create() {
        val vertexShader = compileShader(
            GLES20.GL_VERTEX_SHADER,
            "attribute vec2 aPosition; void main(){ gl_Position = vec4(aPosition, 0.0, 1.0); }"
        )
        val fragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor = uColor; }"
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
        colorLocation = GLES20.glGetUniformLocation(program, "uColor")
    }

    fun resize(width: Int, height: Int) {
        screenWidth = width.coerceAtLeast(1)
        screenHeight = height.coerceAtLeast(1)
        worldScale = minOf(screenWidth / 20f, screenHeight / 12f)
        worldOffsetX = (screenWidth - 20f * worldScale) * 0.5f
        worldOffsetY = (screenHeight - 12f * worldScale) * 0.5f
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
    }

    fun render(engine: GameEngine, joystickX: Float, joystickY: Float, touching: Boolean, interacting: Boolean) {
        GLES20.glClearColor(0.035f, 0.047f, 0.055f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        currentCamera = engine.camera
        GLES20.glEnableVertexAttribArray(positionLocation)

        // Base floor and subtle tile grid.
        rect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, 0.12f, 0.15f, 0.16f)
        for (x in 1 until GameEngine.WORLD_WIDTH.toInt()) {
            rect(x.toFloat(), 0f, 0.025f, GameEngine.WORLD_HEIGHT, 0.16f, 0.18f, 0.17f)
        }
        for (y in 1 until GameEngine.WORLD_HEIGHT.toInt()) {
            rect(0f, y.toFloat(), GameEngine.WORLD_WIDTH, 0.025f, 0.16f, 0.18f, 0.17f)
        }

        // Draw only non-floor tiles; the tile map is also used by movement collision.
        engine.tileMap.forEachTile { tileX, tileY, type ->
            when (type) {
                TileType.FLOOR -> Unit
                TileType.WALL -> rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.29f, 0.30f, 0.27f)
                TileType.BED -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.31f, 0.25f, 0.18f)
                    rect(tileX + 0.08f, tileY + 0.12f, 0.84f, 0.18f, 0.43f, 0.35f, 0.23f)
                }
                TileType.WORKBENCH -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.20f, 0.35f, 0.26f)
                    rect(tileX + 0.12f, tileY + 0.18f, 0.76f, 0.18f, 0.48f, 0.35f, 0.20f)
                }
                TileType.STORAGE -> {
                    rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.34f, 0.27f, 0.17f)
                    rect(tileX + 0.12f, tileY + 0.12f, 0.76f, 0.76f, 0.18f, 0.20f, 0.19f)
                }
                TileType.EXIT -> rect(tileX.toFloat(), tileY.toFloat(), 1f, 1f, 0.20f, 0.35f, 0.39f)
                TileType.RESOURCE_WOOD -> drawResource(tileX, tileY, 0.55f, 0.35f, 0.20f)
                TileType.RESOURCE_METAL -> drawResource(tileX, tileY, 0.55f, 0.60f, 0.62f)
                TileType.RESOURCE_FABRIC -> drawResource(tileX, tileY, 0.35f, 0.49f, 0.46f)
                TileType.RESOURCE_FOOD -> drawResource(tileX, tileY, 0.68f, 0.48f, 0.20f)
                TileType.RESOURCE_WATER -> drawResource(tileX, tileY, 0.22f, 0.48f, 0.68f)
            }
        }

        // Character: compact blocks keep the prototype's pixel-art look.
        val px = engine.player.x
        val py = engine.player.y
        rect(px - 0.28f, py - 0.12f, 0.56f, 0.62f, 0.16f, 0.25f, 0.24f)
        rect(px - 0.22f, py - 0.48f, 0.44f, 0.38f, 0.68f, 0.55f, 0.39f)
        rect(px - 0.30f, py + 0.42f, 0.22f, 0.22f, 0.12f, 0.14f, 0.14f)
        rect(px + 0.08f, py + 0.42f, 0.22f, 0.22f, 0.12f, 0.14f, 0.14f)

        // Screen-space HUD is independent from camera movement.
        screenRect(0.55f, 0.48f, 5.2f, 0.42f, 0.07f, 0.08f, 0.08f)
        screenRect(0.72f, 0.60f, 1.35f, 0.16f, 0.56f, 0.24f, 0.19f)
        screenRect(2.20f, 0.60f, 1.05f, 0.16f, 0.25f, 0.43f, 0.49f)
        screenRect(3.38f, 0.60f, 1.05f, 0.16f, 0.69f, 0.57f, 0.28f)
        drawInventory(engine.inventory)

        // Virtual joystick and interaction button.
        screenRect(2.0f, 8.2f, 2.4f, 2.4f, 0.22f, 0.25f, 0.24f)
        if (touching) {
            val radiusPx = (screenHeight * 0.19f).coerceAtLeast(72f)
            val thumbX = (joystickX - worldOffsetX) / worldScale
            val thumbY = (joystickY - worldOffsetY) / worldScale
            val thumbSize = (radiusPx * 0.36f) / worldScale
            screenRect(
                thumbX - thumbSize * 0.5f,
                thumbY - thumbSize * 0.5f,
                thumbSize,
                thumbSize,
                0.67f, 0.65f, 0.55f
            )
        }
        screenRect(16.8f, 9.2f, 2.2f, 2.2f,
            if (interacting) 0.48f else 0.28f,
            if (interacting) 0.55f else 0.32f,
            if (interacting) 0.42f else 0.30f)


        GLES20.glDisableVertexAttribArray(positionLocation)
    }

    private fun drawResource(x: Int, y: Int, r: Float, g: Float, b: Float) {
        rect(x.toFloat(), y.toFloat(), 1f, 1f, 0.16f, 0.18f, 0.17f)
        rect(x + 0.18f, y + 0.22f, 0.64f, 0.56f, r, g, b)
        rect(x + 0.28f, y + 0.12f, 0.44f, 0.12f, r * 0.75f, g * 0.75f, b * 0.75f)
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
            }
            screenRect(x + 0.14f, y + 0.14f, 0.44f, 0.44f, color[0], color[1], color[2])
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
        val vertices = floatArrayOf(
            toNdcX(left), toNdcY(top),
            toNdcX(left), toNdcY(bottom),
            toNdcX(right), toNdcY(top),
            toNdcX(right), toNdcY(bottom)
        )
        vertexBuffer.clear()
        vertexBuffer.put(vertices).position(0)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glUniform4f(colorLocation, r, g, b, 1f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun screenRect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float) {
        val left = worldOffsetX + x * worldScale
        val top = worldOffsetY + y * worldScale
        val right = worldOffsetX + (x + width) * worldScale
        val bottom = worldOffsetY + (y + height) * worldScale
        val vertices = floatArrayOf(
            toNdcX(left), toNdcY(top),
            toNdcX(left), toNdcY(bottom),
            toNdcX(right), toNdcY(top),
            toNdcX(right), toNdcY(bottom)
        )
        vertexBuffer.clear()
        vertexBuffer.put(vertices).position(0)
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glUniform4f(colorLocation, r, g, b, 1f)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    }

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
}
