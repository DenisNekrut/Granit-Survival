package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/** Pause menu screen; all navigation is supplied by the Activity coordinator. */
internal class PauseMenuScreen(
    private val activity: Activity,
    private val session: GameSession,
    private val createPanel: (String, String) -> LinearLayout,
    private val label: (String, Float, Int, Boolean) -> TextView,
    private val menuButton: (String, Boolean, () -> Unit) -> Button,
    private val showOverlay: (LinearLayout) -> Unit,
    private val enterGame: () -> Unit,
    private val showCharacterStatistics: () -> Unit,
    private val confirmReturnToMainMenu: () -> Unit,
    private val showJournal: () -> Unit,
    private val showBookLibrary: () -> Unit,
    private val showDiscoveryJournal: () -> Unit,
    private val showCrafting: () -> Unit,
    private val showStorage: () -> Unit,
    private val showSaveSlots: (Boolean) -> Unit,
    private val showSettings: () -> Unit,
    private val refresh: () -> Unit
) {
    fun show() {
        val engine = session.engine
        val panel = createPanel("ПАУЗА", "Состояние игры сохранено в памяти")
        panel.addView(label("Психологическое состояние", 16f, GranitPalette.textPrimary, false))
        panel.addView(label(
            "Стресс: ${engine.psychology.stress.toInt()}  ·  Одиночество: ${engine.psychology.loneliness.toInt()}",
            13f, GranitPalette.textSecondary, false
        ))
        panel.addView(label(
            "Усталость: ${engine.psychology.fatigue.toInt()}  ·  Мораль: ${engine.psychology.morale.toInt()}",
            13f, Color.LTGRAY, false
        ))
        panel.addView(menuButton("ПРОДОЛЖИТЬ", true) { enterGame() })
        panel.addView(menuButton("КРАТКОЕ РУКОВОДСТВО", true) { showSurvivalGuide() })
        panel.addView(menuButton("СТАТИСТИКА ПЕРСОНАЖА", true) { showCharacterStatistics() })
        panel.addView(menuButton("ГЛАВНОЕ МЕНЮ", true) { confirmReturnToMainMenu() })
        panel.addView(menuButton("ЖУРНАЛ ЭКСПЕДИЦИЙ", true) { showJournal() })
        panel.addView(menuButton("ЕСТЬ КОНСЕРВЫ", engine.inventory.count(ItemType.CANNED_FOOD) > 0) {
            if (session.consumeFood()) {
                Toast.makeText(activity, "Консервы использованы", Toast.LENGTH_SHORT).show()
                refresh()
            }
        })
        panel.addView(menuButton("ПИТЬ ВОДУ", engine.inventory.count(ItemType.WATER) > 0) {
            if (session.drinkWater()) {
                Toast.makeText(activity, "Вода использована", Toast.LENGTH_SHORT).show()
                refresh()
            }
        })
        panel.addView(menuButton("РАДИОПРИЁМНИК", engine.canUseRadio()) {
            val event = engine.listenToRadio()
            if (event != null) {
                AlertDialog.Builder(activity)
                    .setTitle(event.title)
                    .setMessage(event.description)
                    .setPositiveButton("ПОНЯТНО") { _, _ -> refresh() }
                    .show()
            } else {
                Toast.makeText(activity, "Сейчас эфир недоступен", Toast.LENGTH_SHORT).show()
            }
        })
        panel.addView(menuButton("БИБЛИОТЕКА", engine.canAccessBookshelf()) { showBookLibrary() })
        panel.addView(menuButton(
            "ЖУРНАЛ НАХОДОК (${engine.discoveredPointsOfInterest().size}/${PointOfInterestCatalog.all().size})",
            true
        ) { showDiscoveryJournal() })
        panel.addView(menuButton("МАСТЕРСКАЯ", engine.canUseWorkbench()) { showCrafting() })
        panel.addView(menuButton(
            "ПЕРЕВЯЗАТЬСЯ",
            engine.inventory.count(ItemType.BANDAGE) > 0 && engine.needs.health < 100f
        ) {
            if (engine.useBandage()) {
                Toast.makeText(activity, "Бинт использован", Toast.LENGTH_SHORT).show()
                refresh()
            }
        })
        panel.addView(menuButton(
            "АПТЕЧКА",
            engine.inventory.count(ItemType.MEDICAL_KIT) > 0 && engine.needs.health < 100f
        ) {
            if (engine.useMedicalKit()) {
                Toast.makeText(activity, "Аптечка использована", Toast.LENGTH_SHORT).show()
                refresh()
            }
        })
        panel.addView(menuButton("ХРАНИЛИЩЕ", engine.canAccessStorage()) { showStorage() })
        panel.addView(menuButton("СОН ДО 07:00", engine.canSleep()) {
            runCatching { session.sleepAndAutosave() }
                .onSuccess { slept ->
                    if (slept) {
                        Toast.makeText(activity, "Новый день. Автосохранение создано.", Toast.LENGTH_SHORT).show()
                        refresh()
                    } else {
                        Toast.makeText(activity, "Для сна нужно находиться рядом с кроватью.", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    Toast.makeText(activity, "Не удалось создать автосохранение: ${it.message}", Toast.LENGTH_LONG).show()
                }
        })
        panel.addView(menuButton("СОХРАНИТЬ ИГРУ", true) { showSaveSlots(true) })
        panel.addView(menuButton("СОХРАНЕНИЯ", true) { showSaveSlots(false) })
        panel.addView(menuButton("НАСТРОЙКИ", true) { showSettings() })
        showOverlay(panel)
    }
    private fun showSurvivalGuide() {
        val panel = createPanel(
            "ПАМЯТКА ВЫЖИВШЕГО",
            "Короткие правила. Время в игре продолжает идти во время большинства действий."
        )
        fun section(title: String, body: String) {
            panel.addView(label(title, 15f, Color.rgb(207, 194, 155), true))
            panel.addView(label(body, 13f, Color.rgb(214, 221, 207), false))
        }
        section("ОСНОВЫ ВЫЖИВАНИЯ",
            "Следи за здоровьем, голодом, жаждой и энергией. Еда и вода восполняют потребности, отдых восстанавливает силы. Не затягивай с лечением и возвращением в убежище.")
        section("ВЗАИМОДЕЙСТВИЕ С МИРОМ",
            "Подойди к объекту или ресурсу и нажми контекстную кнопку действия. Если рядом несколько целей, ориентируйся на выбранную цель. Некоторые объекты требуют времени или определённых условий.")
        section("РЮКЗАК И ХРАНИЛИЩЕ",
            "Открой рюкзак кнопкой с изображением сумки. Предметы находятся в отдельных ячейках. В хранилище выбери ячейку и действие «ПОЛОЖИТЬ» или «ЗАБРАТЬ». Само открытие хранилища ничего не переносит.")
        section("ПРЕДМЕТЫ И МЕДИЦИНА",
            "Нажми на ячейку интерактивного предмета, чтобы увидеть доступное действие. Консервы и вода поддерживают потребности, бинт и аптечка помогают восстановить здоровье. Использование возможно при подходящих условиях.")
        section("БЕЗОПАСНОСТЬ В БУНКЕРЕ",
            "Следи за топливом генератора. Кровать позволяет завершить день и создать контрольную точку. Верстак используется для изготовления предметов, библиотека — для чтения.")
        section("ЭКСПЕДИЦИИ",
            "Перед выходом проверь запасы и экипировку. Исследуй лес, руины и дорогу, собирай полезные ресурсы и возвращайся до критического истощения. Используй контрольную точку перед вылазкой.")
        panel.addView(menuButton("НАЗАД К ПАУЗЕ", true) { show() })
        showOverlay(panel)
    }

}
