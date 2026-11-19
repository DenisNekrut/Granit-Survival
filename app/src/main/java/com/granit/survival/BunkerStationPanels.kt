package com.granit.survival

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast

/** Native UI panels for the bunker utility stations. */
internal class BunkerStationPanels(
    private val activity: Activity,
    private val session: GameSession,
    private val createPanel: (String, String) -> LinearLayout,
    private val label: (String, Float, Int, Boolean) -> TextView,
    private val menuButton: (String, Boolean, () -> Unit) -> Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val enterGame: () -> Unit,
    private val closeStationPanel: () -> Unit,
    private val showMessage: (String, String) -> Unit
) {
    fun showRadio() {
        val engine = session.engine
        if (!engine.bunkerPower.hasPower) {
            showMessage("РАДИОПРИЁМНИК", "Нет питания. Для работы радиостанции запустите генератор.")
            return
        }
        val event = engine.listenToRadio()
        if (event != null) showMessage(event.title, event.description)
        else showMessage("РАДИОПРИЁМНИК", "Сегодня эфир остаётся пустым.")
    }

    fun showBed() {
        if (!session.engine.canSleep()) {
            showMessage("СПАЛЬНЯ", "Сейчас нельзя лечь спать.")
            return
        }
        val panel = createPanel("СПАЛЬНЯ", "Можно завершить день и восстановить силы")
        panel.addView(label("Сон завершит текущий день. После пробуждения будет создана контрольная точка.", 13f, Color.rgb(206, 215, 202), false))
        panel.addView(menuButton("СПАТЬ ДО 07:00", true) {
            runCatching { session.sleepAndAutosave() }
                .onSuccess { slept ->
                    if (slept) {
                        enterGame()
                        Toast.makeText(activity, "Новый день. Автосохранение создано.", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { error ->
                    Toast.makeText(activity, "Не удалось сохранить игру: " + error.message, Toast.LENGTH_LONG).show()
                }
        })
        panel.addView(menuButton("ЗАКРЫТЬ", true) { closeStationPanel() })
        showOverlay(panel)
    }

    fun showGenerator() {
        val engine = session.engine
        val panel = createPanel("ГЕНЕРАТОРНАЯ", "Энергосистема убежища")
        val powered = engine.bunkerPower.hasPower
        panel.addView(label(
            if (powered) "ПИТАНИЕ: ОСНОВНАЯ СЕТЬ" else "ПИТАНИЕ: АВАРИЙНЫЙ РЕЖИМ",
            15f, if (powered) Color.rgb(164, 210, 170) else Color.rgb(230, 165, 116), false
        ))
        panel.addView(label("Запас топлива: " + engine.bunkerPower.fuelHoursRemaining + " ч", 14f, Color.WHITE, false))
        panel.addView(label(
            "Генератор: " + if (engine.bunkerPower.generatorRunning) "работает" else "остановлен",
            13f, Color.rgb(190, 198, 190), false
        ))
        panel.addView(menuButton(
            if (engine.bunkerPower.generatorRunning) "ОСТАНОВИТЬ ГЕНЕРАТОР" else "ЗАПУСТИТЬ ГЕНЕРАТОР",
            engine.bunkerPower.generatorRunning || engine.bunkerPower.fuelMinutesRemaining > 0
        ) {
            engine.toggleGenerator()
            showGenerator()
        })
        panel.addView(menuButton(
            "ЗАГРУЗИТЬ ДРОВА (+4 ЧАСА)",
            engine.inventory.count(ItemType.WOOD) > 0 &&
                engine.bunkerPower.fuelMinutesRemaining < BunkerPowerSystem.MAX_FUEL_MINUTES
        ) {
            if (engine.refuelGenerator()) showGenerator()
        })
        panel.addView(label("При отключении генератора остаётся слабое аварийное освещение.", 12f, Color.rgb(151, 170, 157), false))
        panel.addView(menuButton("ЗАКРЫТЬ", true) { enterGame() })
        showOverlay(panel)
    }

    fun showDining() {
        val engine = session.engine
        val panel = createPanel("СТОЛОВАЯ", "Запасы экспедиции")
        panel.addView(label("Консервы: ${engine.inventory.count(ItemType.CANNED_FOOD)}", 15f, Color.WHITE, false))
        panel.addView(label("Вода: ${engine.inventory.count(ItemType.WATER)}", 15f, Color.WHITE, false))
        panel.addView(menuButton("СЪЕСТЬ КОНСЕРВЫ", engine.inventory.count(ItemType.CANNED_FOOD) > 0) {
            if (session.consumeFood()) showDining()
        })
        panel.addView(menuButton("ВЫПИТЬ ВОДУ", engine.inventory.count(ItemType.WATER) > 0) {
            if (session.drinkWater()) showDining()
        })
        panel.addView(menuButton("ЗАКРЫТЬ", true) { enterGame() })
        showOverlay(panel)
    }

    fun showMedical() {
        val engine = session.engine
        val panel = createPanel("МЕДЧАСТЬ", "Медицинская станция и расходные материалы")
        val hurt = engine.needs.health < 100f
        panel.addView(label("Здоровье: ${engine.needs.health.toInt()}%", 16f, Color.WHITE, false))
        panel.addView(label(
            if (engine.bunkerPower.hasPower) "Станция запитана" else "Нет питания: доступна только ручная помощь",
            12f, if (engine.bunkerPower.hasPower) Color.rgb(164, 210, 170) else Color.rgb(230, 165, 116), false
        ))
        panel.addView(menuButton(
            "ПРОВЕСТИ ОБРАБОТКУ (+50 ЗДОРОВЬЯ, 12 МИН.)",
            engine.canUseMedicalStation() && hurt && engine.inventory.count(ItemType.MEDICAL_KIT) > 0
        ) {
            if (engine.treatAtMedicalStation()) showMedical()
        })
        panel.addView(menuButton("ИСПОЛЬЗОВАТЬ БИНТ (+15)", engine.inventory.count(ItemType.BANDAGE) > 0 && hurt) {
            if (engine.useBandage()) showMedical()
        })
        panel.addView(menuButton("ИСПОЛЬЗОВАТЬ АПТЕЧКУ (+35)", engine.inventory.count(ItemType.MEDICAL_KIT) > 0 && hurt) {
            if (engine.useMedicalKit()) showMedical()
        })
        panel.addView(label(
            "Обработка на станции требует генератор и одну аптечку. Ручная помощь работает и без питания.",
            11f, Color.rgb(151, 170, 157), false
        ))
        panel.addView(menuButton("ЗАКРЫТЬ", true) { enterGame() })
        showOverlay(panel)
    }
}
