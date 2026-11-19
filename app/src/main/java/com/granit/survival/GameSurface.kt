package com.granit.survival

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurface(context: Context, private val session: GameSession) : SurfaceView(context), SurfaceHolder.Callback {
    @Volatile private var renderThread: EngineRenderThread? = null
    @Volatile private var resumed = false
    @Volatile private var surfaceWidth = 1
    @Volatile private var surfaceHeight = 1
    private var joystickPointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var actionPointerId: Int = MotionEvent.INVALID_POINTER_ID

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun resumeEngine() {
        resumed = true
        startIfReady()
    }

    fun pauseEngine() {
        resumed = false
        stopThread()
    }

    fun releaseEngine() {
        resumed = false
        stopThread()
        holder.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startIfReady()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        renderThread?.setViewport(surfaceWidth, surfaceHeight)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopThread()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val thread = renderThread ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val pointerId = event.getPointerId(index)
                if (event.getX(index) < surfaceWidth * 0.55f && joystickPointerId == MotionEvent.INVALID_POINTER_ID) {
                    joystickPointerId = pointerId
                    thread.setJoystick(event.getX(index), event.getY(index), true)
                } else if (actionPointerId == MotionEvent.INVALID_POINTER_ID) {
                    actionPointerId = pointerId
                    thread.setInteracting(true)
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
                if (pointerId == actionPointerId) {
                    actionPointerId = MotionEvent.INVALID_POINTER_ID
                    thread.setInteracting(false)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                joystickPointerId = MotionEvent.INVALID_POINTER_ID
                actionPointerId = MotionEvent.INVALID_POINTER_ID
                thread.setJoystick(0f, 0f, false)
                thread.setInteracting(false)
            }
        }
        return true
    }

    @Synchronized
    private fun startIfReady() {
        if (!resumed || !holder.surface.isValid || renderThread?.isAlive == true) return
        val thread = EngineRenderThread(holder, surfaceWidth, surfaceHeight, session, context.applicationContext)
        renderThread = thread
        thread.start()
    }

    @Synchronized
    private fun stopThread() {
        val thread = renderThread ?: return
        thread.requestStop()
        try {
            thread.join(1500)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        renderThread = null
        joystickPointerId = MotionEvent.INVALID_POINTER_ID
        actionPointerId = MotionEvent.INVALID_POINTER_ID
    }
}
