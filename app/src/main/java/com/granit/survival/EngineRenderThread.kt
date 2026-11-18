package com.granit.survival

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.util.Log
import android.view.SurfaceHolder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/** Owns the EGL context, fixed-step simulation and rendering on one thread. */
class EngineRenderThread(
    private val surfaceHolder: SurfaceHolder,
    initialWidth: Int,
    initialHeight: Int,
    private val session: GameSession
) : Thread("Granit-RenderThread") {
    private val running = AtomicBoolean(true)
    @Volatile private var viewportWidth = initialWidth.coerceAtLeast(1)
    @Volatile private var viewportHeight = initialHeight.coerceAtLeast(1)
    @Volatile private var touchX = 0f
    @Volatile private var touchY = 0f
    @Volatile private var joystickActive = false
    @Volatile private var interacting = false

    fun requestStop() {
        running.set(false)
        interrupt()
    }

    fun setViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
    }

    fun setJoystick(x: Float, y: Float, active: Boolean) {
        touchX = x
        touchY = y
        joystickActive = active
    }

    fun setInteracting(active: Boolean) {
        interacting = active
    }

    override fun run() {
        var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var context: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
            val versions = IntArray(2)
            check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "Unable to initialize EGL" }

            val configAttributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            check(EGL14.eglChooseConfig(display, configAttributes, 0, configs, 0, 1, count, 0) && count[0] > 0) {
                "No compatible EGL config"
            }
            val config = requireNotNull(configs[0])
            val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
            check(context != EGL14.EGL_NO_CONTEXT) { "Unable to create GLES2 context" }

            eglSurface = EGL14.eglCreateWindowSurface(
                display, config, surfaceHolder, intArrayOf(EGL14.EGL_NONE), 0
            )
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window surface" }
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
                "Unable to make EGL context current"
            }

            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            val renderer = PixelRenderer()
            renderer.create()
            renderer.resize(viewportWidth, viewportHeight)
            val engine = session.engine
            val gameSettings = session.settings.load()
            val fixedFrameDelayNanos = 16_000_000L
            var previousFrame = System.nanoTime()

            while (running.get() && surfaceHolder.surface.isValid) {
                val frameStart = System.nanoTime()
                val deltaSeconds = ((frameStart - previousFrame) / 1_000_000_000.0)
                    .toFloat().coerceAtMost(0.25f)
                previousFrame = frameStart

                val width = viewportWidth.coerceAtLeast(1)
                val height = viewportHeight.coerceAtLeast(1)
                GLES20.glViewport(0, 0, width, height)
                renderer.resize(width, height)

                val active = joystickActive
                val dx = touchX - width * 0.16f
                val dy = touchY - height * 0.78f
                val radius = (height * 0.19f * gameSettings.joystickScale).coerceAtLeast(72f)
                val distance = sqrt(dx * dx + dy * dy)
                val input = if (active && distance > radius * 0.12f) {
                    val magnitude = ((distance / radius) * gameSettings.joystickSensitivity).coerceAtMost(1f)
                    EngineInput(
                        moveX = (dx / distance) * magnitude,
                        moveY = (dy / distance) * magnitude
                    )
                } else {
                    EngineInput()
                }

                engine.updateFrame(deltaSeconds, input.copy(interact = interacting))
                renderer.render(
                    engine, touchX, touchY, active, interacting,
                    controlsOpacity = gameSettings.controlsOpacity,
                    joystickScale = gameSettings.joystickScale
                )

                if (!EGL14.eglSwapBuffers(display, eglSurface)) {
                    val eglError = EGL14.eglGetError()
                    if (eglError == EGL14.EGL_BAD_SURFACE || eglError == EGL14.EGL_CONTEXT_LOST) break
                    throw IllegalStateException("eglSwapBuffers failed: " + Integer.toHexString(eglError))
                }

                val elapsed = System.nanoTime() - frameStart
                val sleepMillis = ((fixedFrameDelayNanos - elapsed) / 1_000_000L).coerceAtLeast(1L)
                try {
                    sleep(sleepMillis)
                } catch (_: InterruptedException) {
                    if (!running.get()) break
                }
            }
        } catch (failure: Throwable) {
            Log.e("GranitEngine", "Render thread failed", failure)
        } finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
            }
        }
    }
}
