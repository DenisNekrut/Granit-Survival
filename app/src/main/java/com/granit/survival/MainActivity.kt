package com.granit.survival
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

class MainActivity : Activity() {
    private lateinit var gameSurface: GameSurface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gameSurface = GameSurface(this)
        setContentView(gameSurface)
    }
    override fun onResume() {
        super.onResume()
        if (::gameSurface.isInitialized) gameSurface.resumeEngine()
    }
    override fun onPause() {
        if (::gameSurface.isInitialized) gameSurface.pauseEngine()
        super.onPause()
    }
    override fun onDestroy() {
        if (::gameSurface.isInitialized) gameSurface.releaseEngine()
        super.onDestroy()
    }
}
