package com.granit.survival
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.view.SurfaceHolder
import java.util.concurrent.atomic.AtomicBoolean

/** Minimal platform renderer bootstrap. All GL calls stay on this thread. */
class EngineRenderThread(private val surfaceHolder: SurfaceHolder) : Thread("Granit-RenderThread") {
    private val running = AtomicBoolean(true)
    @Volatile private var viewportWidth = 1
    @Volatile private var viewportHeight = 1
    @Volatile private var touchX = 0f
    @Volatile private var touching = false

    fun requestStop() { running.set(false); interrupt() }
    fun setViewport(width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1); viewportHeight = height.coerceAtLeast(1)
    }
    fun setTouch(x: Float, y: Float, isTouching: Boolean) {
        touchX = x; touching = isTouching
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
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8, EGL14.EGL_NONE
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
            var lastFrame = System.nanoTime()
            while (running.get() && surfaceHolder.surface.isValid) {
                val now = System.nanoTime()
                val delta = ((now - lastFrame) / 1_000_000_000.0).toFloat()
                lastFrame = now
                GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
                val pulse = if (touching) (touchX / viewportWidth).coerceIn(0f, 1f) * 0.08f else 0f
                GLES20.glClearColor(0.035f + pulse, 0.055f, 0.075f + delta.coerceAtMost(0.05f), 1f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                if (!EGL14.eglSwapBuffers(display, eglSurface)) {
                    val eglError = EGL14.eglGetError()
                    if (eglError == EGL14.EGL_BAD_SURFACE || eglError == EGL14.EGL_CONTEXT_LOST) break
                    throw IllegalStateException("eglSwapBuffers failed: " + Integer.toHexString(eglError))
                }
                try { sleep(16) } catch (_: InterruptedException) {
                    if (!running.get()) break
                }
            }
        } catch (failure: Throwable) {
            android.util.Log.e("GranitEngine", "Render thread failed", failure)
        } finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, eglSurface)
                if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                EGL14.eglTerminate(display)
            }
        }
    }
}
