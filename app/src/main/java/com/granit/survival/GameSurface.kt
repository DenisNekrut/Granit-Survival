package com.granit.survival

import android.content.Context
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.util.Log

class GameSurface(context: Context, private val session: GameSession) : SurfaceView(context), SurfaceHolder.Callback {
    companion object { private const val TAG = "GranitSurface" }
    @Volatile private var renderThread: EngineRenderThread? = null
    @Volatile private var resumed = false
    @Volatile var onRenderFailure: ((Throwable) -> Unit)? = null
    @Volatile var onInteractionProcessed: (() -> Unit)? = null
    @Volatile private var surfaceWidth = 0
    @Volatile private var surfaceHeight = 0
    @Volatile private var surfaceReady = false
    private var joystickPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var actionPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var firePointerId: Int = MotionEvent.INVALID_POINTER_ID

    init {
        // Match the RGBA_8888 EGL config selected by EngineRenderThread.
        // Without an explicit native-window format, some Android devices reject or
        // silently fail to present the GLES surface after entering a new session.
        holder.setFormat(PixelFormat.RGBA_8888)
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun resumeEngine() {
        resumed = true
        Log.i(TAG, "resumeEngine: surfaceReady=$surfaceReady size=${surfaceWidth}x$surfaceHeight valid=${holder.surface.isValid}")
        startIfReady()
    }

    fun pauseEngine() {
        Log.i(TAG, "pauseEngine: stopping render thread")
        resumed = false
        stopThread()
    }

    fun syncViewportFromView() {
        val viewWidth = width.coerceAtLeast(0)
        val viewHeight = height.coerceAtLeast(0)
        if (viewWidth > 1 && viewHeight > 1) {
            surfaceWidth = viewWidth
            surfaceHeight = viewHeight
            Log.i(TAG, "syncViewportFromView: ${viewWidth}x${viewHeight}")
            renderThread?.setViewport(viewWidth, viewHeight)
        }
    }

    fun releaseEngine() {
        resumed = false
        stopThread()
        holder.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated: format=${holder.surfaceFrame} valid=${holder.surface.isValid}")
        startIfReady()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        surfaceReady = true
        Log.i(TAG, "surfaceChanged: format=$format size=${surfaceWidth}x$surfaceHeight holderFrame=${holder.surfaceFrame.width()}x${holder.surfaceFrame.height()}")
        renderThread?.setViewport(surfaceWidth, surfaceHeight)
        startIfReady()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed: stopping render thread")
        surfaceReady = false
        stopThread()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val thread = renderThread ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                val touchX = event.getX(index)
                val touchY = event.getY(index)
                val inJoystickArea = touchX < surfaceWidth * 0.38f && touchY > surfaceHeight * 0.62f
                val inActionArea = touchX > surfaceWidth * 0.68f && touchY > surfaceHeight * 0.72f
                val inFireArea = touchX > surfaceWidth * 0.68f && touchY in (surfaceHeight * 0.48f)..(surfaceHeight * 0.68f)
                if (inJoystickArea && joystickPointerId == MotionEvent.INVALID_POINTER_ID) {
                    joystickPointerId = pointerId
                    thread.setJoystick(touchX, touchY, true)
                } else if (inFireArea && firePointerId == MotionEvent.INVALID_POINTER_ID) {
                    firePointerId = pointerId
                    thread.setFiring(true)
                } else if (inActionArea && actionPointerId == MotionEvent.INVALID_POINTER_ID) {
                    actionPointerId = pointerId
                    thread.setInteracting(true)
                    thread.requestInteraction()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val joystickIndex = event.findPointerIndex(joystickPointerId)
                if (joystickIndex >= 0) thread.setJoystick(event.getX(joystickIndex), event.getY(joystickIndex), true)
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                val pointerId = event.getPointerId(event.actionIndex)
                if (pointerId == joystickPointerId) {
                    joystickPointerId = MotionEvent.INVALID_POINTER_ID
                    thread.setJoystick(0f, 0f, false)
                }
                if (pointerId == firePointerId) {
                    firePointerId = MotionEvent.INVALID_POINTER_ID
                    thread.setFiring(false)
                }
                if (pointerId == actionPointerId) {
                    actionPointerId = MotionEvent.INVALID_POINTER_ID
                    thread.setInteracting(false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                joystickPointerId = MotionEvent.INVALID_POINTER_ID
                actionPointerId = MotionEvent.INVALID_POINTER_ID
                firePointerId = MotionEvent.INVALID_POINTER_ID
                thread.setFiring(false)
                thread.setJoystick(0f, 0f, false)
                thread.setInteracting(false)
            }
        }
        return true
    }

    @Synchronized
    private fun startIfReady() {
        if (!resumed || !surfaceReady || surfaceWidth <= 1 || surfaceHeight <= 1 ||
            !holder.surface.isValid || renderThread?.isAlive == true) return
        Log.i(TAG, "startIfReady: starting render thread with viewport=${surfaceWidth}x$surfaceHeight")
        val thread = EngineRenderThread(
            holder, surfaceWidth, surfaceHeight, session, context.applicationContext,
            onInteractionProcessed = { post { onInteractionProcessed?.invoke() } }
        ) { failure ->
            post { onRenderFailure?.invoke(failure) }
        }
        renderThread = thread
        thread.start()
    }

    @Synchronized
    private fun stopThread() {
        val thread = renderThread ?: return
        Log.i(TAG, "stopThread: requesting stop for ${thread.name}, alive=${thread.isAlive}")
        thread.requestStop()
        try {
            thread.join(1500)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        Log.i(TAG, "stopThread: joined ${thread.name}, alive=${thread.isAlive}")
        if (!thread.isAlive) {
            renderThread = null
            session.engine.releaseInputState()
        } else {
            Log.e(TAG, "render thread did not stop within timeout; keeping reference to prevent duplicate EGL threads")
        }
        joystickPointerId = MotionEvent.INVALID_POINTER_ID
        actionPointerId = MotionEvent.INVALID_POINTER_ID
        firePointerId = MotionEvent.INVALID_POINTER_ID
        thread.setJoystick(0f, 0f, false)
        thread.setInteracting(false)
        thread.setFiring(false)
    }
}
