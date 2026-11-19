package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Native Android menus wrap the custom GLES game surface; no UI framework/engine dependency. */
class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var gameSurface: GameSurface
    private lateinit var session: GameSession
    private var activeGame = false
    private var pausedGame = false
    private val bunkerStationPanels by lazy {
        BunkerStationPanels(
            activity = this,
            session = session,
            createPanel = ::createPanel,
            label = ::label,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            enterGame = ::enterGame,
            closeStationPanel = ::closeStationPanel,
            showMessage = ::showMessageWindow,
            weightedParams = ::weightedParams
        )
    }
    private val stationPanelRouter by lazy {
        StationPanelRouter(
            mapOf(
                TileType.WORKBENCH to ::showCrafting,
                TileType.STORAGE to ::showStorage,
                TileType.BOOKSHELF to ::showBookLibrary,
                TileType.DINING_TABLE to ::showDiningPanel,
                TileType.MEDICAL_STATION to ::showMedicalPanel,
                TileType.GENERATOR to ::showGeneratorPanel,
                TileType.RADIO to ::showRadioPanel,
                TileType.BED to ::showBedPanel
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        session = GameSession(this)
        root = FrameLayout(this)
        gameSurface = GameSurface(this, session)
        gameSurface.onInteractionProcessed = { handleContextAction() }
        gameSurface.onRenderFailure = { failure ->
            if (activeGame && !isFinishing) {
                activeGame = false
                AlertDialog.Builder(this)
                    .setTitle("Не удалось запустить игровой экран")
                    .setMessage("Рендер остановился: ${failure.javaClass.simpleName}: ${failure.message ?: "без подробностей"}\n\nПодробности доступны в Android Logcat по тегу GranitEngine.")
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.i("GranitLifecycle", "configuration changed: orientation=${newConfig.orientation} screen=${newConfig.screenWidthDp}x${newConfig.screenHeightDp}dp")
        if (::gameSurface.isInitialized) gameSurface.syncViewportFromView()
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

    /**
     * Route the on-screen action button to the nearby station's native panel.
     * World collection and exits are still processed by GameEngine on the render thread.
     */
    private fun handleContextAction() {
        if (!activeGame || isFinishing) return
        val engine = session.engine
        val hasNearbyResource = engine.tileMap.nearestResource(
            engine.player.x, engine.player.y, 1.35f
        ) != null
        val target = engine.tileMap.nearestInteractable(
            engine.player.x, engine.player.y, 1.35f
        )
        stationPanelRouter.route(target, hasNearbyResource)
    }

    private fun showRadioPanel() = bunkerStationPanels.showRadio()

    private fun showBedPanel() = bunkerStationPanels.showBed()

    private fun showGeneratorPanel() = bunkerStationPanels.showGenerator()

    private fun showDiningPanel() = bunkerStationPanels.showDining()

    private fun showMedicalPanel() = bunkerStationPanels.showMedical()

    private fun showMessageWindow(title: String, message: String) {
        val panel = createPanel(title, "СООБЩЕНИЕ")
        panel.addView(label(message, 14f, Color.rgb(214, 221, 207)))
        panel.addView(menuButton("ЗАКРЫТЬ") { enterGame() })
        showOverlay(panel)
    }

    private fun closeStationPanel() {
        if (activeGame) enterGame() else showPauseMenu()
    }

    private fun showMainMenu() {
        activeGame = false
        pausedGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        MenuCoordinator(
            activity = this,
            createPanel = ::createPanel,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            hasSavedGame = {
                session.saves.autoSaveTimestamp() != null ||
                    session.saves.listSlots().any { it.exists && !it.corrupted }
            },
            startNewGame = ::startNewGame,
            loadMostRecent = session::loadMostRecent,
            enterGame = ::enterGame,
            showSaveSlots = { showSaveSlots(false) },
            showSettings = ::showSettings,
            exit = ::finish
        ).showMainMenu()
    }

    private fun startNewGame() {
        session.startNewGame()
        enterGame()
    }

    private fun enterGame() {
        pausedGame = false
        activeGame = true
        if (root.childCount > 1) root.removeViews(1, root.childCount - 1)
        gameSurface.visibility = View.VISIBLE
        addBackpackButton()
        gameSurface.resumeEngine()
        val audioSettings = session.settings.load()
        session.audio.setVolumes(audioSettings.musicVolume, audioSettings.effectsVolume)
        session.audio.start()
    }

    private fun addBackpackButton() {
        if (root.childCount > 1) root.removeViews(1, root.childCount - 1)
        val pauseButton = menuButton("МЕНЮ", action = { confirmReturnToMainMenu() }).apply {
            textSize = 12f
            minWidth = dp(72)
            minHeight = dp(54)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = GradientDrawable().apply {
                setColor(Color.argb(230, 28, 43, 39))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), Color.rgb(170, 153, 111))
            }
        }
        root.addView(pauseButton, FrameLayout.LayoutParams(dp(78), dp(58), Gravity.TOP or Gravity.START).apply {
            topMargin = dp(14)
            marginStart = dp(14)
        })
        val button = menuButton("🎒", action = { showBackpack() }).apply {
            textSize = 23f
            minWidth = dp(58)
            minHeight = dp(54)
            setPadding(dp(8), dp(2), dp(8), dp(2))
            background = GradientDrawable().apply {
                setColor(Color.argb(220, 28, 43, 39))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(2), Color.rgb(170, 153, 111))
            }
        }
        root.addView(button, FrameLayout.LayoutParams(dp(62), dp(58), Gravity.TOP or Gravity.END).apply {
            topMargin = dp(14)
            marginEnd = dp(14)
        })
    }

    private fun showBackpack() {
        InventoryScreen(
            activity = this,
            createPanel = ::createPanel,
            label = ::label,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            close = ::enterGame,
            refresh = ::showBackpack,
            consumeFood = session::consumeFood,
            drinkWater = session::drinkWater,
            useBandage = session.engine::useBandage,
            useMedicalKit = session.engine::useMedicalKit,
            dp = ::dp
        ).show(session.engine)
    }

    private fun showPauseMenu() {
        activeGame = false
        pausedGame = true
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        PauseMenuScreen(
            activity = this,
            session = session,
            createPanel = ::createPanel,
            label = ::label,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            enterGame = ::enterGame,
            showCharacterStatistics = ::showCharacterStatistics,
            confirmReturnToMainMenu = ::confirmReturnToMainMenu,
            showJournal = ::showJournal,
            showBookLibrary = ::showBookLibrary,
            showDiscoveryJournal = ::showDiscoveryJournal,
            showCrafting = ::showCrafting,
            showStorage = ::showStorage,
            showSaveSlots = ::showSaveSlots,
            showSettings = ::showSettings,
            refresh = ::showPauseMenu
        ).show()
    }

    private fun confirmReturnToMainMenu() {
        AlertDialog.Builder(this)
            .setTitle("Вернуться в главное меню?")
            .setMessage("Несохранённый прогресс текущей вылазки будет потерян. Сохранённые слоты и контрольная точка останутся без изменений.")
            .setNegativeButton("ОСТАТЬСЯ", null)
            .setPositiveButton("В МЕНЮ") { _, _ -> showMainMenu() }
            .show()
    }

    private fun showCharacterStatistics() {
        val stats = session.engine.statistics.snapshot()
        val expeditionHours = stats.expeditionMinutes / 60L
        val expeditionMinutes = stats.expeditionMinutes % 60L
        val panel = createPanel("СТАТИСТИКА ПЕРСОНАЖА", "Показатели текущей временной линии")
        val engine = session.engine
        panel.addView(label("ТЕКУЩЕЕ СОСТОЯНИЕ", 15f, Color.rgb(207, 194, 155), true))
        panel.addView(label("Здоровье: ${engine.needs.health.toInt()}  ·  Голод: ${engine.needs.hunger.toInt()}  ·  Жажда: ${engine.needs.thirst.toInt()}", 13f, Color.LTGRAY))
        panel.addView(label("Энергия: ${engine.needs.energy.toInt()}  ·  Стресс: ${engine.psychology.stress.toInt()}  ·  Мораль: ${engine.psychology.morale.toInt()}", 13f, Color.LTGRAY))
        panel.addView(label("Экипировка: " + (if (engine.equipment.weaponDrawn) "сигнальный излучатель в руках" else "убрана"), 13f, Color.LTGRAY))
        panel.addView(label("ИСТОРИЯ ВЫЖИВАНИЯ", 15f, Color.rgb(207, 194, 155), true))
        panel.addView(label("Дней выживания: ${stats.daysSurvived}", 15f, Color.WHITE))
        panel.addView(label("Время в экспедициях: ${expeditionHours} ч ${expeditionMinutes} мин", 14f, Color.LTGRAY))
        panel.addView(label("Пройдено: ${String.format(java.util.Locale.ROOT, "%.1f", stats.distanceTravelled)} м", 14f, Color.LTGRAY))
        panel.addView(label("Собрано ресурсов: ${stats.resourcesCollected}", 14f, Color.LTGRAY))
        panel.addView(label("Создано предметов: ${stats.itemsCrafted}", 14f, Color.LTGRAY))
        panel.addView(label("Прочитано книг: ${stats.booksRead}", 14f, Color.LTGRAY))
        panel.addView(label("Посещённые зоны", 15f, Color.WHITE, true))
        WorldZone.values().filter { it in stats.visitedZones }.forEach { zone ->
            panel.addView(label("• ${zone.displayName}", 13f, Color.LTGRAY))
        }
        panel.addView(label("Статистика откатывается вместе с контрольной точкой при гибели.", 12f, Color.rgb(190, 177, 145)))
        panel.addView(menuButton("ЗАКРЫТЬ") { closeStationPanel() })
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

    private fun showBookLibrary() = bunkerStationPanels.showLibrary()

    private fun showCrafting() = bunkerStationPanels.showWorkshop()

    private fun showStorage() = bunkerStationPanels.showStorage()

    private fun showSaveSlots(allowSave: Boolean) {
        activeGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        SaveSlotsScreen(
            activity = this,
            saves = session.saves,
            createPanel = ::createPanel,
            label = ::label,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            weightedParams = ::weightedParams,
            dp = ::dp,
            loadSlot = session::loadSlot,
            saveSlot = { slot -> session.saveSlot(slot) },
            onLoaded = ::enterGame,
            onSaved = { if (pausedGame) showPauseMenu() else showMainMenu() },
            onBack = { if (pausedGame || allowSave) showPauseMenu() else showMainMenu() }
        ).show(allowSave)
    }

    private fun showSettings() {
        activeGame = false
        gameSurface.pauseEngine()
        session.audio.stop()
        gameSurface.visibility = View.GONE
        SettingsScreen(
            activity = this,
            current = session.settings.load(),
            settings = session.settings,
            createPanel = ::createPanel,
            label = ::label,
            menuButton = ::menuButton,
            showOverlay = ::showOverlay,
            onSaved = { if (pausedGame) showPauseMenu() else showMainMenu() },
            onCancel = { if (pausedGame) showPauseMenu() else showMainMenu() }
        ).show()
    }

    private fun createPanel(title: String, subtitle: String): LinearLayout {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(16))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.rgb(30, 43, 41), Color.rgb(17, 27, 28))
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(104, 126, 105))
            }
        }
        content.addView(label(title, 22f, Color.rgb(230, 205, 151), true).apply {
            letterSpacing = 0.045f
        })
        content.addView(label(subtitle, 12f, Color.rgb(165, 184, 169)).apply {
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(3), dp(4), dp(12))
        })
        return content
    }

    private fun showOverlay(content: LinearLayout) {
        // Keep the title visible while only the actual controls list can scroll.
        val panelBackground = content.background
        content.background = null
        content.setPadding(dp(4), dp(4), dp(4), dp(8))

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        repeat(minOf(2, content.childCount)) {
            val child = content.getChildAt(0)
            content.removeViewAt(0)
            header.addView(child, LinearLayout.LayoutParams(-1, -2))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setPadding(0, dp(4), 0, dp(4))
            isClickable = true
            setOnClickListener { /* Keep taps inside the card from closing the window. */ }
            addView(content, ScrollView.LayoutParams(-1, -2))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(10))
            background = panelBackground
            isClickable = true
            setOnClickListener { /* Consume taps inside the card. */ }
            addView(header, LinearLayout.LayoutParams(-1, -2))
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        val overlay = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { if (activeGame) closeStationPanel() }
            setBackgroundColor(if (activeGame) Color.argb(72, 3, 9, 10) else Color.rgb(8, 14, 15))
        }

        val availableWidth = (root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).coerceAtLeast(1)
        val availableHeight = (root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels).coerceAtLeast(1)
        val widthFraction = if (activeGame) 0.76f else 0.88f
        val heightFraction = if (activeGame) 0.82f else 0.90f
        val maxWidth = dp(if (activeGame) 720 else 760)
        val maxHeight = dp(if (activeGame) 560 else 680)
        val cardWidth = minOf(maxWidth, (availableWidth * widthFraction).toInt()).coerceAtLeast(dp(220))
        val cardHeight = minOf(maxHeight, (availableHeight * heightFraction).toInt()).coerceAtLeast(dp(200))
        overlay.addView(card, FrameLayout.LayoutParams(cardWidth, cardHeight, Gravity.CENTER))

        if (root.childCount > 1) root.removeViews(1, root.childCount - 1)
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
            background = GradientDrawable().apply {
                setColor(if (enabled) Color.rgb(57, 82, 68) else Color.rgb(39, 48, 45))
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), if (enabled) Color.rgb(112, 143, 112) else Color.rgb(65, 75, 68))
            }
            minHeight = dp(48)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
        }

    private fun weightedParams() = LinearLayout.LayoutParams(0, -2, 1f).apply {
        marginStart = dp(2); marginEnd = dp(2)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

}
