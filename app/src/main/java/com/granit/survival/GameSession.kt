package com.granit.survival

import android.content.Context

/**
 * Owns the long-lived game state and Android persistence adapters.
 * Activity actions that load/save are performed only after the render thread is paused.
 */
class GameSession(context: Context) {
    val saves = GameSaveRepository(context.applicationContext)
    val settings = GameSettingsRepository(context.applicationContext)
    val audio = GranitAudioEngine()
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

    fun transferToStorage(type: ItemType): Boolean = engine.transferToStorage(type)

    fun transferFromStorage(type: ItemType): Boolean = engine.transferFromStorage(type)

    fun consumeFood(): Boolean = engine.consumeFood()

    fun drinkWater(): Boolean = engine.drinkWater()

    var lastLoadError: String? = null
        private set

    fun loadMostRecent(): Boolean {
        lastLoadError = null
        val slots = saves.listSlots()
        val candidate = slots
            .filter { it.exists && !it.corrupted && it.savedAtMillis != null }
            .maxByOrNull { it.savedAtMillis ?: 0L }
        if (candidate == null && slots.any { it.exists && it.corrupted }) {
            lastLoadError = "Файл сохранения повреждён. Открой список слотов и проверьте его."
        }
        val autoTimestamp = saves.autoSaveTimestamp()
        if (autoTimestamp == null && saves.hasAutoSaveFile() && lastLoadError == null) {
            lastLoadError = "Файл автосохранения повреждён или не читается."
        }
        if (autoTimestamp != null && autoTimestamp > (candidate?.savedAtMillis ?: 0L)) {
            try {
                if (saves.loadAuto(engine)) return true
            } catch (failure: Exception) {
                lastLoadError = failure.message ?: "Не удалось загрузить автосохранение"
            }
        }
        if (candidate != null) {
            try {
                return loadSlot(candidate.slot)
            } catch (failure: Exception) {
                lastLoadError = failure.message ?: "Не удалось загрузить слот ${candidate.slot}"
            }
        }
        return false
    }
}
