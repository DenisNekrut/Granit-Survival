package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import android.view.Gravity

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
    private val showMessage: (String, String) -> Unit,
    private val weightedParams: () -> LinearLayout.LayoutParams
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


    fun showLibrary() {
        val panel = createPanel(
            "БИБЛИОТЕКА УБЕЖИЩА",
            "Чтение занимает игровое время. Новые знания закрепляются при следующем пробуждении."
        )
        BookCatalog.all.forEach { book ->
            val read = session.engine.bookLibrary.hasRead(book.id)
            panel.addView(label(book.title + " · " + book.category.displayName, 15f, Color.WHITE, false))
            panel.addView(label(book.description + " · " + (book.readMinutes / 60) + " ч. чтения", 12f, Color.LTGRAY, false))
            panel.addView(menuButton(if (read) "ПРОЧИТАНО" else "ЧИТАТЬ", !read) {
                if (session.engine.readBook(book.id)) {
                    Toast.makeText(activity, "Книга прочитана", Toast.LENGTH_SHORT).show()
                    showLibrary()
                } else {
                    Toast.makeText(activity, "Не удалось прочитать книгу", Toast.LENGTH_SHORT).show()
                }
            })
        }
        panel.addView(menuButton("ЗАКРЫТЬ", true) { closeStationPanel() })
        showOverlay(panel)
    }

    fun showWorkshop() {
        val engine = session.engine
        val panel = createPanel("МАСТЕРСКАЯ", "Рецепты доступны рядом с верстаком")
        if (!engine.bunkerPower.hasPower) {
            panel.addView(label("Нет питания: генератор необходимо запустить.", 13f, Color.rgb(230, 165, 116), false))
        }
        CraftingRecipe.all.forEach { recipe ->
            val costs = recipe.ingredients.entries.joinToString(" · ") { entry ->
                entry.key.displayName + ": " + entry.value
            }
            panel.addView(label(recipe.title + " — " + recipe.output.displayName, 16f, Color.WHITE, false))
            panel.addView(label(
                "Материалы: " + costs + " · Время: " + recipe.craftMinutes + " мин.",
                13f, Color.LTGRAY, false
            ))
            panel.addView(menuButton("СОЗДАТЬ", engine.canUseWorkbench() && engine.canCraft(recipe.id)) {
                if (engine.craft(recipe.id)) {
                    Toast.makeText(activity, recipe.title + ": готово", Toast.LENGTH_SHORT).show()
                    showWorkshop()
                } else {
                    val message = if (!engine.bunkerPower.hasPower) "Нет питания: запустите генератор"
                        else "Не хватает материалов или места"
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
                }
            })
        }
        panel.addView(menuButton("ЗАКРЫТЬ", true) { closeStationPanel() })
        showOverlay(panel)
    }

    fun showStorage() {
        val engine = session.engine
        fun dp(value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
        val panel = createPanel(
            "ХРАНИЛИЩЕ УБЕЖИЩА",
            "Выбирай отдельную ячейку. Само открытие контейнера ничего не перемещает."
        )

        fun addGrid(
            title: String,
            columns: Int,
            rows: Int,
            slotAt: (Int) -> ItemStack?,
            transfer: (Int, Int) -> Boolean,
            actionVerb: String,
            useSelected: ((ItemType) -> Boolean)? = null
        ) {
            panel.addView(label(title, 15f, Color.rgb(207, 194, 155), true))
            for (rowIndex in 0 until rows) {
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                for (column in 0 until columns) {
                    val index = rowIndex * columns + column
                    val stack = slotAt(index)
                    val cell = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(dp(3), dp(4), dp(3), dp(4))
                        setBackgroundColor(Color.argb(175, 28, 43, 39))
                    }
                    if (stack == null) {
                        cell.addView(label("ПУСТО", 9f, Color.rgb(151, 170, 157), false))
                    } else {
                        cell.addView(
                            InventoryPixelIconView(activity, stack.type.iconSpriteId),
                            LinearLayout.LayoutParams(dp(38), dp(38))
                        )
                        cell.addView(label(
                            stack.type.displayName,
                            8f,
                            Color.rgb(214, 221, 207),
                            false
                        ))
                        cell.addView(label("×${stack.quantity}", 10f, Color.WHITE, true))
                        cell.isClickable = true
                        cell.setOnClickListener {
                            val selected = stack
                            val canUse = useSelected != null && selected.type in setOf(
                                ItemType.CANNED_FOOD, ItemType.WATER,
                                ItemType.BANDAGE, ItemType.MEDICAL_KIT
                            )
                            val actions = buildList {
                                add(if (selected.quantity > 1) "$actionVerb 1 предмет" else "$actionVerb предмет")
                                if (selected.quantity > 1) add("$actionVerb всю стопку (${selected.quantity})")
                                if (canUse) add("ЗАБРАТЬ И ИСПОЛЬЗОВАТЬ")
                            }.toTypedArray()
                            AlertDialog.Builder(activity)
                                .setTitle(selected.type.displayName)
                                .setItems(actions) { _, choice ->
                                    val useChoice = canUse && choice == actions.lastIndex
                                    val amount = if (choice == 1 && selected.quantity > 1) selected.quantity else 1
                                    if (transfer(index, amount)) {
                                        if (useChoice) {
                                            val used = useSelected?.invoke(selected.type) == true
                                            if (!used) {
                                                Toast.makeText(
                                                    activity,
                                                    "Предмет забран в рюкзак, но сейчас его нельзя использовать",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        showStorage()
                                    } else {
                                        Toast.makeText(
                                            activity,
                                            "Перенос невозможен: проверь свободные ячейки и доступ к хранилищу",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .setNegativeButton("ОТМЕНА", null)
                                .show()
                        }
                    }
                    val cellParams = LinearLayout.LayoutParams(0, dp(90), 1f).apply {
                        setMargins(dp(2), dp(2), dp(2), dp(2))
                    }
                    row.addView(cell, cellParams)
                }
                panel.addView(row)
            }
        }

        addGrid(
            "РЮКЗАК · ${engine.inventory.columns} × ${engine.inventory.rows}",
            engine.inventory.columns,
            engine.inventory.rows,
            engine.inventory::slot,
            engine::transferToStorageAt,
            "ПОЛОЖИТЬ"
        )
        addGrid(
            "ХРАНИЛИЩЕ · ${engine.bunkerStorage.columns} × ${engine.bunkerStorage.rows}",
            engine.bunkerStorage.columns,
            engine.bunkerStorage.rows,
            engine.bunkerStorage::slot,
            engine::transferFromStorageAt,
            "ЗАБРАТЬ"
        ) { type ->
            when (type) {
                ItemType.CANNED_FOOD -> session.consumeFood()
                ItemType.WATER -> session.drinkWater()
                ItemType.BANDAGE -> engine.useBandage()
                ItemType.MEDICAL_KIT -> engine.useMedicalKit()
                else -> false
            }
        }
        panel.addView(menuButton("ЗАКРЫТЬ", true) { closeStationPanel() })
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
