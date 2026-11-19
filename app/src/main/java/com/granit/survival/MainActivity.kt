package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

/** Native Android menus wrap the custom GLES game surface; no UI framework/engine dependency. */
class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var gameSurface: GameSurface
    private lateinit var session: GameSession
    private var activeGame = false
    private var pausedGame = false
    private var canSaveFromSlots = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        session = GameSession(this)
        root = FrameLayout(this)
        gameSurface = GameSurface(this, session)
        gameSurface.onRenderFailure = { failure ->
            if (activeGame && !isFinishing) {
                activeGame = false
                AlertDialog.Builder(this)
                    .setTitle("Не удалось запустить игровой экран")
                    .setMessage("Рендер остановился: ${failure.javaClass.simpleName}: ${failure.message ?: "без подробностей"}\\n\\nПодробности доступны в Android Logcat по тегу GranitEngine.")
                    .setPositiveButton("В ГЛАВНОЕ МЕНЮ") { _, _ -> showMainMenu() }
                    .setCancelable(false)
                    .show()
            }
        }
        root.addView(gameSurface, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        showMainMenu()
    }

    override fun onResume() {
        super.onResume()
        if (::gameSurface.isInitialized && activeGame) {
            gameSurface.resumeEngine()
            val audioSettings = session.settings.load()
            session.audio.setVolumes(audioSettings.musicVolume, audioSettings.effectsVolume)
            session.audio.start()
        }
    }

    override fun onPause() {
        if (::gameSurface.isInitialized) gameSurface.pauseEngine()
        if (::session.isInitialized) session.audio.stop()
        super.onPause()
    }

    override fun onDestroy() {
        if (::gameSurface.isInitialized) gameSurface.releaseEngine()
        if (::session.isInitialized) session.audio.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android API; retained for Android 8+ back-key compatibility")
    override fun onBackPressed() {
        when {
            activeGame -> showPauseMenu()
            pausedGame -> showPauseMenu()
            else -> showMainMenu()
        }
    }

    private fun showMainMenu() {
        activeGame = false
        pausedGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        val panel = createPanel("GRANIT SURVIVAL", "Тишина — тоже часть выживания")
        panel.addView(menuButton("НОВАЯ ИГРА") {
            if (session.saves.listSlots().any { it.exists && !it.corrupted }) {
                AlertDialog.Builder(this)
                    .setTitle("Начать новую игру?")
                    .setMessage("Текущие сохранения останутся в слотах, но начнётся новая игровая сессия.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("НАЧАТЬ") { _, _ -> startNewGame() }
                    .show()
            } else startNewGame()
        })
        val hasSave = session.saves.autoSaveTimestamp() != null ||
            session.saves.listSlots().any { it.exists && !it.corrupted }
        panel.addView(menuButton("ПРОДОЛЖИТЬ", enabled = hasSave) {
            if (session.loadMostRecent()) enterGame()
            else Toast.makeText(this, "Нет доступного сохранения", Toast.LENGTH_SHORT).show()
        })
        panel.addView(menuButton("СОХРАНЕНИЯ") { showSaveSlots(false) })
        panel.addView(menuButton("НАСТРОЙКИ") { showSettings() })
        panel.addView(menuButton("ВЫХОД") { finish() })
        showOverlay(panel)
    }

    private fun startNewGame() {
        session.startNewGame()
        enterGame()
    }

    private fun enterGame() {
        pausedGame = false
        activeGame = true
        root.removeViewAt(root.childCount - 1)
        gameSurface.visibility = View.VISIBLE
        gameSurface.resumeEngine()
        val audioSettings = session.settings.load()
        session.audio.setVolumes(audioSettings.musicVolume, audioSettings.effectsVolume)
        session.audio.start()
    }

    private fun showPauseMenu() {
        activeGame = false
        pausedGame = true
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        val panel = createPanel("ПАУЗА", "Состояние игры сохранено в памяти")
        panel.addView(label("Психологическое состояние", 16f, Color.WHITE))
        panel.addView(label("Стресс: " + session.engine.psychology.stress.toInt() + "  ·  Одиночество: " + session.engine.psychology.loneliness.toInt(), 13f, Color.LTGRAY))
        panel.addView(label("Усталость: " + session.engine.psychology.fatigue.toInt() + "  ·  Мораль: " + session.engine.psychology.morale.toInt(), 13f, Color.LTGRAY))
        panel.addView(menuButton("ПРОДОЛЖИТЬ") { enterGame() })
        panel.addView(menuButton("ЖУРНАЛ ЭКСПЕДИЦИЙ") { showJournal() })
        panel.addView(menuButton("ЕСТЬ КОНСЕРВЫ", enabled = session.engine.inventory.count(ItemType.CANNED_FOOD) > 0) {
            if (session.consumeFood()) { Toast.makeText(this, "Консервы использованы", Toast.LENGTH_SHORT).show(); showPauseMenu() }
        })
        panel.addView(menuButton("ПИТЬ ВОДУ", enabled = session.engine.inventory.count(ItemType.WATER) > 0) {
            if (session.drinkWater()) { Toast.makeText(this, "Вода использована", Toast.LENGTH_SHORT).show(); showPauseMenu() }
        })
        panel.addView(menuButton("РАДИОПРИЁМНИК", enabled = session.engine.canUseRadio()) {
            val event = session.engine.listenToRadio()
            if (event != null) {
                AlertDialog.Builder(this)
                    .setTitle(event.title)
                    .setMessage(event.description)
                    .setPositiveButton("ПОНЯТНО") { _, _ -> showPauseMenu() }
                    .show()
            } else Toast.makeText(this, "Сейчас эфир недоступен", Toast.LENGTH_SHORT).show()
        })
        panel.addView(menuButton("БИБЛИОТЕКА", enabled = session.engine.canAccessBookshelf()) { showBookLibrary() })
        panel.addView(menuButton("ЖУРНАЛ НАХОДОК (" + session.engine.discoveredPointsOfInterest().size + "/" + PointOfInterestCatalog.all().size + ")") {
            showDiscoveryJournal()
        })
        panel.addView(menuButton("МАСТЕРСКАЯ", enabled = session.engine.canUseWorkbench()) { showCrafting() })
        panel.addView(menuButton("ПЕРЕВЯЗАТЬСЯ", enabled = session.engine.inventory.count(ItemType.BANDAGE) > 0 && session.engine.needs.health < 100f) {
            if (session.engine.useBandage()) { Toast.makeText(this, "Бинт использован", Toast.LENGTH_SHORT).show(); showPauseMenu() }
        })
        panel.addView(menuButton("АПТЕЧКА", enabled = session.engine.inventory.count(ItemType.MEDICAL_KIT) > 0 && session.engine.needs.health < 100f) {
            if (session.engine.useMedicalKit()) { Toast.makeText(this, "Аптечка использована", Toast.LENGTH_SHORT).show(); showPauseMenu() }
        })
        panel.addView(menuButton("ХРАНИЛИЩЕ", enabled = session.engine.canAccessStorage()) { showStorage() })
        panel.addView(menuButton("СОН ДО 07:00", enabled = session.engine.canSleep()) {
            runCatching { session.sleepAndAutosave() }
                .onSuccess { slept ->
                    if (slept) {
                        Toast.makeText(this, "Новый день. Автосохранение создано.", Toast.LENGTH_SHORT).show()
                        showPauseMenu()
                    } else Toast.makeText(this, "Для сна нужно находиться рядом с кроватью.", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(this, "Не удалось создать автосохранение: ${it.message}", Toast.LENGTH_LONG).show() }
        })
        panel.addView(menuButton("СОХРАНИТЬ ИГРУ") { showSaveSlots(true) })
        panel.addView(menuButton("СОХРАНЕНИЯ") { showSaveSlots(false) })
        panel.addView(menuButton("НАСТРОЙКИ") { showSettings() })
        panel.addView(menuButton("ГЛАВНОЕ МЕНЮ") {
            AlertDialog.Builder(this)
                .setTitle("Вернуться в главное меню?")
                .setMessage("Несохранённый прогресс текущей вылазки будет потерян.")
                .setNegativeButton("ОСТАТЬСЯ", null)
                .setPositiveButton("В МЕНЮ") { _, _ -> showMainMenu() }
                .show()
        })
        showOverlay(panel)
    }

    private fun showDiscoveryJournal() {
        val panel = createPanel("ЖУРНАЛ ЭКСПЕДИЦИЙ", "Места, обнаруженные во время вылазок")
        val discoveries = session.engine.discoveredPointDetails()
        if (discoveries.isEmpty()) {
            panel.addView(label("Пока ничего не найдено. Исследуйте лес, руины и дорогу.", 14f, Color.LTGRAY))
        } else {
            WorldZone.values().filter { zone -> discoveries.any { it.zone == zone } }.forEach { zone ->
                panel.addView(label(zone.displayName.uppercase(), 17f, Color.rgb(207, 194, 155), true))
                discoveries.filter { it.zone == zone }.forEach { poi ->
                    panel.addView(label(poi.title, 15f, Color.WHITE))
                    panel.addView(label(poi.description, 12f, Color.LTGRAY))
                }
            }
        }
        panel.addView(menuButton("НАЗАД") { showPauseMenu() })
        showOverlay(panel)
    }

    private fun showJournal() {
        val discovered = session.engine.discoveredPointDetails()
        val panel = createPanel("ЖУРНАЛ ЭКСПЕДИЦИЙ", discovered.size.toString() + " / " + PointOfInterestCatalog.all().size + " мест обнаружено")
        if (discovered.isEmpty()) {
            panel.addView(label("Пока нет записей. Исследуйте лес, руины и дорогу, чтобы находить интересные места.", 14f, Color.LTGRAY))
        } else {
            discovered.forEach { poi ->
                panel.addView(label(poi.title + " · " + poi.zone.displayName, 16f, Color.WHITE))
                panel.addView(label(poi.description, 13f, Color.LTGRAY))
            }
        }
        panel.addView(menuButton("НАЗАД") { showPauseMenu() })
        showOverlay(panel)
    }

    private fun showBookLibrary() {
        val panel = createPanel("БИБЛИОТЕКА УБЕЖИЩА", "Чтение занимает игровое время. Новые знания закрепляются при следующем пробуждении.")
        BookCatalog.all.forEach { book ->
            val read = session.engine.bookLibrary.hasRead(book.id)
            panel.addView(label(book.title + " · " + book.category.displayName, 15f, Color.WHITE))
            panel.addView(label(book.description + " · " + book.readMinutes + " мин.", 12f, Color.LTGRAY))
            panel.addView(menuButton(if (read) "ПРОЧИТАНО" else "ЧИТАТЬ", enabled = !read) {
                if (session.engine.readBook(book.id)) {
                    Toast.makeText(this, "Книга прочитана", Toast.LENGTH_SHORT).show()
                    showBookLibrary()
                } else Toast.makeText(this, "Не удалось прочитать книгу", Toast.LENGTH_SHORT).show()
            })
        }
        panel.addView(menuButton("НАЗАД") { showPauseMenu() })
        showOverlay(panel)
    }

    private fun showCrafting() {
        val panel = createPanel("МАСТЕРСКАЯ", "Рецепты доступны рядом с верстаком")
        CraftingRecipe.all.forEach { recipe ->
            val costs = recipe.ingredients.entries.joinToString(" · ") { entry -> entry.key.displayName + ": " + entry.value }
            panel.addView(label(recipe.title + " — " + recipe.output.displayName, 16f, Color.WHITE))
            panel.addView(label("Материалы: " + costs + " · Время: " + recipe.craftMinutes + " мин.", 13f, Color.LTGRAY))
            panel.addView(menuButton("СОЗДАТЬ", enabled = session.engine.canCraft(recipe.id)) {
                if (session.engine.craft(recipe.id)) {
                    Toast.makeText(this, recipe.title + ": готово", Toast.LENGTH_SHORT).show()
                    showCrafting()
                } else Toast.makeText(this, "Не хватает материалов или места", Toast.LENGTH_SHORT).show()
            })
        }
        panel.addView(menuButton("НАЗАД") { showPauseMenu() })
        showOverlay(panel)
    }

    private fun showStorage() {
        val panel = createPanel("ХРАНИЛИЩЕ УБЕЖИЩА", "Перенос ресурсов между рюкзаком и контейнером")
        ItemType.values().forEach { type ->
            val backpack = session.engine.inventory.count(type)
            val stored = session.engine.bunkerStorage.count(type)
            panel.addView(label("${type.displayName}:  рюкзак $backpack  ·  хранилище $stored", 14f, Color.WHITE))
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(menuButton("ПОЛОЖИТЬ ВСЁ", enabled = backpack > 0) {
                if (session.transferToStorage(type)) showStorage()
                else Toast.makeText(this, "Не удалось переместить предметы", Toast.LENGTH_SHORT).show()
            }, weightedParams())
            row.addView(menuButton("ЗАБРАТЬ ВСЁ", enabled = stored > 0) {
                if (session.transferFromStorage(type)) showStorage()
                else Toast.makeText(this, "В рюкзаке недостаточно места", Toast.LENGTH_SHORT).show()
            }, weightedParams())
            panel.addView(row)
        }
        panel.addView(menuButton("НАЗАД") { showPauseMenu() })
        showOverlay(panel)
    }

    private fun showSaveSlots(allowSave: Boolean) {
        activeGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        canSaveFromSlots = allowSave
        val panel = createPanel("СОХРАНЕНИЯ", "Три независимых слота на устройстве")
        session.saves.listSlots().forEach { slot ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(8), dp(12), dp(8))
                setBackgroundColor(Color.rgb(34, 43, 42))
            }
            val info = when {
                slot.corrupted -> "СЛОТ ${slot.slot}  ·  ФАЙЛ ПОВРЕЖДЁН"
                !slot.exists -> "СЛОТ ${slot.slot}  ·  ПУСТО"
                else -> {
                    val time = "%02d:%02d".format(slot.hour ?: 0, slot.minute ?: 0)
                    val date = slot.savedAtMillis?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) } ?: ""
                    "СЛОТ ${slot.slot}  ·  ДЕНЬ ${slot.day ?: 1}, $time\\n$date"
                }
            }
            row.addView(label(info, 14f, Color.WHITE))
            val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(menuButton("ЗАГРУЗИТЬ", enabled = slot.exists && !slot.corrupted) {
                val loaded = runCatching { session.loadSlot(slot.slot) }.getOrDefault(false)
                if (loaded) enterGame() else Toast.makeText(this@MainActivity, "Не удалось загрузить слот", Toast.LENGTH_LONG).show()
            }, weightedParams())
            actions.addView(menuButton("СОХРАНИТЬ", enabled = allowSave) {
                AlertDialog.Builder(this)
                    .setTitle("Перезаписать слот ${slot.slot}?")
                    .setMessage("Старое сохранение в этом слоте будет заменено.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("СОХРАНИТЬ") { _, _ ->
                        runCatching { session.saveSlot(slot.slot) }
                            .onSuccess { Toast.makeText(this, "Игра сохранена", Toast.LENGTH_SHORT).show(); if (pausedGame) showPauseMenu() else showMainMenu() }
                            .onFailure { Toast.makeText(this, "Ошибка сохранения: ${it.message}", Toast.LENGTH_LONG).show() }
                    }.show()
            }, weightedParams())
            actions.addView(menuButton("УДАЛИТЬ", enabled = slot.exists) {
                AlertDialog.Builder(this)
                    .setTitle("Удалить слот ${slot.slot}?")
                    .setMessage("Это действие нельзя отменить.")
                    .setNegativeButton("ОТМЕНА", null)
                    .setPositiveButton("УДАЛИТЬ") { _, _ ->
                        session.saves.delete(slot.slot)
                        showSaveSlots(allowSave)
                    }.show()
            }, weightedParams())
            row.addView(actions)
            panel.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
        panel.addView(menuButton("НАЗАД") { if (pausedGame || allowSave) showPauseMenu() else showMainMenu() })
        showOverlay(panel)
    }

    private fun showSettings() {
        activeGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        val current = session.settings.load()
        val panel = createPanel("НАСТРОЙКИ", "Параметры управления и отображения")
        fun addSetting(name: String, initial: Float, max: Int, render: (Int) -> String, changed: (Int) -> Unit): SeekBar {
            panel.addView(label(name, 14f, Color.LTGRAY))
            val valueLabel = label(render(initial.toInt()), 12f, Color.rgb(190, 185, 160))
            panel.addView(valueLabel)
            val bar = SeekBar(this).apply {
                this.max = max
                progress = initial.toInt().coerceIn(0, max)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        valueLabel.text = render(progress)
                        changed(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                })
            }
            panel.addView(bar)
            return bar
        }
        var music = current.musicVolume
        var effects = current.effectsVolume
        var sensitivity = current.joystickSensitivity
        var scale = current.joystickScale
        var opacity = current.controlsOpacity
        var displayScale = current.displayScale
        addSetting("ГРОМКОСТЬ МУЗЫКИ", music * 100, 100, { "Уровень: $it%" }) { music = it / 100f }
        addSetting("ГРОМКОСТЬ ЭФФЕКТОВ", effects * 100, 100, { "Уровень: $it%" }) { effects = it / 100f }
        addSetting("ЧУВСТВИТЕЛЬНОСТЬ УПРАВЛЕНИЯ", (sensitivity * 50), 100, { "Множитель: ${(it / 50f).formatOneDecimal()}x" }) { sensitivity = it / 50f }
        addSetting("РАЗМЕР ДЖОЙСТИКА", ((scale - 0.8f) * 166.66f), 100, { "Размер: ${(0.8f + it / 166.66f).formatOneDecimal()}x" }) { scale = 0.8f + it / 166.66f }
        addSetting("ПРОЗРАЧНОСТЬ КНОПОК", ((opacity - 0.35f) / 0.65f * 100), 100, { "Прозрачность: ${(35 + it * 0.65f).toInt()}%" }) { opacity = 0.35f + it / 100f * 0.65f }
        addSetting("МАСШТАБ ИГРОВОГО ЭКРАНА", ((displayScale - 0.8f) * 250f), 100, { "Масштаб: ${80 + it / 100f * 40f}%" }) { displayScale = 0.8f + it / 250f }
        val quality = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, GraphicsQuality.values().map { it.name })
            setSelection(GraphicsQuality.values().indexOf(current.graphicsQuality).coerceAtLeast(0))
        }
        panel.addView(label("КАЧЕСТВО ГРАФИКИ", 14f, Color.LTGRAY))
        panel.addView(quality)
        val vibration = Switch(this).apply {
            text = "ВИБРАЦИЯ"
            setTextColor(Color.WHITE)
            isChecked = current.vibrationEnabled
        }
        panel.addView(vibration)
        panel.addView(menuButton("СОХРАНИТЬ НАСТРОЙКИ") {
            session.settings.save(GameSettings(music, effects, sensitivity, scale, opacity,
                GraphicsQuality.values()[quality.selectedItemPosition], vibration.isChecked, displayScale))
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            if (pausedGame) showPauseMenu() else showMainMenu()
        })
        panel.addView(menuButton("ОТМЕНА") { if (pausedGame) showPauseMenu() else showMainMenu() })
        showOverlay(panel)
    }

    private fun createPanel(title: String, subtitle: String): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(19, 27, 27))
        }
        content.addView(label(title, 28f, Color.rgb(207, 194, 155), true))
        content.addView(label(subtitle, 13f, Color.rgb(142, 155, 147)).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(16))
        })
        return content
    }

    private fun showOverlay(content: LinearLayout) {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(content)
        }
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(10, 15, 16))
            addView(scroll, FrameLayout.LayoutParams(dp(560), -1, Gravity.CENTER))
        }
        if (root.childCount > 1) root.removeViewAt(1)
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }

    private fun menuButton(text: String, enabled: Boolean = true, action: () -> Unit) =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            isEnabled = enabled
            textSize = 14f
            setTextColor(if (enabled) Color.WHITE else Color.GRAY)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (enabled) Color.rgb(65, 82, 74) else Color.rgb(42, 49, 47)
            )
            minHeight = dp(48)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        }

    private fun weightedParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        marginStart = dp(2); marginEnd = dp(2)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun Float.formatOneDecimal() = String.format("%.1f", this)
}
