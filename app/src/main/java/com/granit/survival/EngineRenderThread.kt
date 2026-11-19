package com.granit.survival

import android.content.Context
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
    private val session: GameSession,
    private val appContext: Context,
    private val onInteractionProcessed: () -> Unit = {},
    private val onRenderFailure: (Throwable) -> Unit = {}
) : Thread("Granit-RenderThread") {
    companion object { private const val TAG = "GranitEGL" }
    private val running = AtomicBoolean(true)
    @Volatile private var viewportWidth = initialWidth.coerceAtLeast(1)
    @Volatile private var viewportHeight = initialHeight.coerceAtLeast(1)
    @Volatile private var touchX = 0f
    @Volatile private var touchY = 0f
    @Volatile private var joystickActive = false
    @Volatile private var interacting = false
    @Volatile private var firing = false
    private val pendingInteraction = AtomicBoolean(false)

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

    fun setFiring(active: Boolean) {
        firing = active
    }

    /** Latches a short tap until at least one fixed simulation step consumes it. */
    fun requestInteraction() {
        pendingInteraction.set(true)
    }

    override fun run() {
        Log.i(TAG, "render thread started: requested viewport=${viewportWidth}x$viewportHeight surfaceValid=${surfaceHolder.surface.isValid}")
        var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var context: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
        var renderer: PixelRenderer? = null
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(display != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
            val versions = IntArray(2)
            check(EGL14.eglInitialize(display, versions, 0, versions, 1)) { "Unable to initialize EGL (error=${Integer.toHexString(EGL14.eglGetError())})" }
            Log.i(TAG, "EGL initialized: ${versions[0]}.${versions[1]} vendor=${EGL14.eglQueryString(display, EGL14.EGL_VENDOR)} version=${EGL14.eglQueryString(display, EGL14.EGL_VERSION)} clientApis=${EGL14.eglQueryString(display, EGL14.EGL_CLIENT_APIS)}")

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
            fun configAttribute(attribute: Int): Int {
                val value = IntArray(1)
                return if (EGL14.eglGetConfigAttrib(display, config, attribute, value, 0)) value[0] else -1
            }
            Log.i(TAG, "EGL config: rgba=${configAttribute(EGL14.EGL_RED_SIZE)}/${configAttribute(EGL14.EGL_GREEN_SIZE)}/${configAttribute(EGL14.EGL_BLUE_SIZE)}/${configAttribute(EGL14.EGL_ALPHA_SIZE)} depth=${configAttribute(EGL14.EGL_DEPTH_SIZE)} stencil=${configAttribute(EGL14.EGL_STENCIL_SIZE)}")
            val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
            check(context != EGL14.EGL_NO_CONTEXT) { "Unable to create GLES2 context" }

            eglSurface = EGL14.eglCreateWindowSurface(
                display, config, surfaceHolder, intArrayOf(EGL14.EGL_NONE), 0
            )
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create EGL window surface (error=${Integer.toHexString(EGL14.eglGetError())})" }
            val nativeFrame = surfaceHolder.surfaceFrame
            Log.i(TAG, "EGL window surface created: holderFrame=${nativeFrame.width()}x${nativeFrame.height()} viewport=${viewportWidth}x$viewportHeight")
            if (nativeFrame.width() > 0 && nativeFrame.height() > 0 &&
                (nativeFrame.width() != viewportWidth || nativeFrame.height() != viewportHeight)) {
                Log.w(TAG, "Surface/viewport size mismatch: native=${nativeFrame.width()}x${nativeFrame.height()} view=${viewportWidth}x$viewportHeight; surfaceChanged should synchronize these values")
            }
            check(EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
                "Unable to make EGL context current (error=${Integer.toHexString(EGL14.eglGetError())})"
            }
            Log.i(TAG, "GLES context current: renderer=${GLES20.glGetString(GLES20.GL_RENDERER)} version=${GLES20.glGetString(GLES20.GL_VERSION)} vendor=${GLES20.glGetString(GLES20.GL_VENDOR)}")

            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            renderer = PixelRenderer()
            renderer.create(appContext.assets)
            val engine = session.engine
            val gameSettings = session.settings.load()
            session.audio.setVolumes(gameSettings.musicVolume, gameSettings.effectsVolume)
            renderer.resize(viewportWidth, viewportHeight, gameSettings.displayScale)
            val fixedFrameDelayNanos = 16_000_000L
            var previousFrame = System.nanoTime()
            var wasInteracting = false

            while (running.get() && surfaceHolder.surface.isValid) {
                val frameStart = System.nanoTime()
                val deltaSeconds = ((frameStart - previousFrame) / 1_000_000_000.0)
                    .toFloat().coerceAtMost(0.25f)
                previousFrame = frameStart

                val width = viewportWidth.coerceAtLeast(1)
                val height = viewportHeight.coerceAtLeast(1)
                GLES20.glViewport(0, 0, width, height)
                val viewportError = GLES20.glGetError()
                if (viewportError != GLES20.GL_NO_ERROR) Log.e(TAG, "glViewport failed for ${width}x$height: 0x${Integer.toHexString(viewportError)}")
                renderer.resize(width, height, gameSettings.displayScale)

                val active = joystickActive
                val dx = touchX - width * 0.145f
                val dy = touchY - height * 0.80f
                val radius = (minOf(width, height) * 0.095f * gameSettings.joystickScale).coerceAtLeast(40f)
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

                val interactionRequested = pendingInteraction.get()
                val interactionHeld = interacting
                val fixedSteps = engine.updateFrame(
                    deltaSeconds,
                    input.copy(interact = interactionHeld || interactionRequested, fire = firing)
                )
                if (fixedSteps > 0 && interactionRequested && pendingInteraction.compareAndSet(true, false)) {
                    runCatching { onInteractionProcessed() }
                }
                if (interactionHeld && !wasInteracting) session.audio.playAction()
                wasInteracting = interactionHeld
                session.audio.setMoving(engine.isMoving)
                renderer?.render(
                    engine, touchX, touchY, active, interacting, firing,
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
            Log.e(TAG, "Render thread failed: viewport=${viewportWidth}x$viewportHeight surfaceValid=${surfaceHolder.surface.isValid} eglError=0x${Integer.toHexString(EGL14.eglGetError())}", failure)
            runCatching { onRenderFailure(failure) }
        } finally {
            Log.i(TAG, "render thread stopping; running=${running.get()}")
            if (display != EGL14.EGL_NO_DISPLAY) {
                if (context != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE &&
                    EGL14.eglMakeCurrent(display, eglSurface, eglSurface, context)) {
                    runCatching { renderer?.destroy() }
                }
                EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
                Log.i(TAG, "EGL display terminated")
            }
        }
    }
}
