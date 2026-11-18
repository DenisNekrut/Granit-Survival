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
        worldScale = minOf(screenWidth / GameEngine.WORLD_WIDTH, screenHeight / GameEngine.WORLD_HEIGHT)
        worldOffsetX = (screenWidth - GameEngine.WORLD_WIDTH * worldScale) * 0.5f
        worldOffsetY = (screenHeight - GameEngine.WORLD_HEIGHT * worldScale) * 0.5f
        GLES20.glViewport(0, 0, screenWidth, screenHeight)
    }

    fun render(engine: GameEngine, touchX: Float, touchY: Float, touching: Boolean) {
        GLES20.glClearColor(0.035f, 0.047f, 0.055f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (program == 0) return
        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionLocation)

        // Base floor and subtle tile grid.
        rect(0f, 0f, GameEngine.WORLD_WIDTH, GameEngine.WORLD_HEIGHT, 0.12f, 0.15f, 0.16f)
        for (x in 1..19) {
            rect(x.toFloat(), 0f, 0.025f, GameEngine.WORLD_HEIGHT, 0.16f, 0.18f, 0.17f)
        }
        for (y in 1..11) {
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
            }
        }

        // Simple top status strip placeholder.
        rect(0.55f, 0.48f, 5.2f, 0.42f, 0.07f, 0.08f, 0.08f)
        rect(0.72f, 0.60f, 1.35f, 0.16f, 0.56f, 0.24f, 0.19f)
        rect(2.20f, 0.60f, 1.05f, 0.16f, 0.25f, 0.43f, 0.49f)
        rect(3.38f, 0.60f, 1.05f, 0.16f, 0.69f, 0.57f, 0.28f)

        // Character: compact blocks keep the prototype's pixel-art look.
        val px = engine.player.x
        val py = engine.player.y
        rect(px - 0.28f, py - 0.12f, 0.56f, 0.62f, 0.16f, 0.25f, 0.24f)
        rect(px - 0.22f, py - 0.48f, 0.44f, 0.38f, 0.68f, 0.55f, 0.39f)
        rect(px - 0.30f, py + 0.42f, 0.22f, 0.22f, 0.12f, 0.14f, 0.14f)
        rect(px + 0.08f, py + 0.42f, 0.22f, 0.22f, 0.12f, 0.14f, 0.14f)

        // Minimal status panel and touch joystick indicator.
        rect(0.55f, 0.48f, 5.2f, 0.42f, 0.07f, 0.08f, 0.08f)
        rect(0.72f, 0.60f, 1.35f, 0.16f, 0.56f, 0.24f, 0.19f)
        rect(2.20f, 0.60f, 1.05f, 0.16f, 0.25f, 0.43f, 0.49f)
        rect(3.38f, 0.60f, 1.05f, 0.16f, 0.69f, 0.57f, 0.28f)

        if (touching && touchX < screenWidth * 0.55f) {
            val radius = 0.72f
            val centerX = ((touchX - worldOffsetX) / worldScale).coerceIn(0.9f, 4f)
            val centerY = ((touchY - worldOffsetY) / worldScale).coerceIn(7.5f, 10.8f)
            rect(centerX - radius, centerY - radius, radius * 2f, radius * 2f, 0.35f, 0.39f, 0.36f)
            rect(centerX - 0.22f, centerY - 0.22f, 0.44f, 0.44f, 0.67f, 0.65f, 0.55f)
        }

        GLES20.glDisableVertexAttribArray(positionLocation)
    }

    private fun rect(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float) {
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
