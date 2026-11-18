package com.granit.survival
import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

class GameSurface(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    @Volatile private var renderThread: EngineRenderThread? = null
    @Volatile private var resumed = false
    init { holder.addCallback(this); isFocusable = true }

    fun resumeEngine() { resumed = true; startIfReady() }
    fun pauseEngine() { resumed = false; stopThread() }
    fun releaseEngine() { resumed = false; stopThread(); holder.removeCallback(this) }

    override fun surfaceCreated(holder: SurfaceHolder) { startIfReady() }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        renderThread?.setViewport(width, height)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) { stopThread() }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val thread = renderThread ?: return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                thread.setTouch(event.getX(event.actionIndex), event.getY(event.actionIndex), true)
            MotionEvent.ACTION_MOVE -> thread.setTouch(event.getX(0), event.getY(0), true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL ->
                thread.setTouch(0f, 0f, false)
        }
        return true
    }

    @Synchronized private fun startIfReady() {
        if (!resumed || !holder.surface.isValid || renderThread?.isAlive == true) return
        val thread = EngineRenderThread(holder)
        renderThread = thread
        thread.start()
    }
    @Synchronized private fun stopThread() {
        val thread = renderThread ?: return
        thread.requestStop()
        try { thread.join(1500) } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        renderThread = null
    }
}
