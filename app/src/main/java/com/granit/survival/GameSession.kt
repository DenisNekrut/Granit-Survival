package com.granit.survival

import android.content.Context

/**
 * Owns the long-lived game state and Android persistence adapters.
 * Activity actions that load/save are performed only after the render thread is paused.
 */
class GameSession(context: Context) {
    val saves = GameSaveRepository(context.applicationContext)
    val settings = GameSettingsRepository(context.applicationContext)
    var engine: GameEngine = GameEngine()
        private set

    fun startNewGame() {
        engine = GameEngine()
    }

    fun saveSlot(slot: Int): SaveSlotInfo = saves.save(slot, engine)

    fun loadSlot(slot: Int): Boolean = saves.load(slot, engine)

    fun sleepAndAutosave(): Boolean {
        if (!engine.canSleep()) return false
        engine.sleepUntilMorning()
        saves.saveAuto(engine)
        return true
    }

    fun loadMostRecent(): Boolean {
        val candidate = saves.listSlots()
            .filter { it.exists && !it.corrupted && it.savedAtMillis != null }
            .maxByOrNull { it.savedAtMillis ?: 0L }
        val autoTimestamp = saves.autoSaveTimestamp()
        return if (autoTimestamp != null && autoTimestamp > (candidate?.savedAtMillis ?: 0L)) {
            runCatching { saves.loadAuto(engine) }.getOrDefault(false)
        } else {
            candidate?.let { runCatching { loadSlot(it.slot) }.getOrDefault(false) } ?: false
        }
    }
}
