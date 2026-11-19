package com.granit.survival

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
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

    private fun showRadioPanel() {
        val engine = session.engine
        if (!engine.bunkerPower.hasPower) {
            showMessageWindow("РАДИОПРИЁМНИК", "Нет питания. Для работы радиостанции запустите генератор.")
            return
        }
        val event = engine.listenToRadio()
        if (event != null) showMessageWindow(event.title, event.description)
        else showMessageWindow("РАДИОПРИЁМНИК", "Сегодня эфир остаётся пустым.")
    }

    private fun showBedPanel() {
        val engine = session.engine
        if (!engine.canSleep()) {
            showMessageWindow("СПАЛЬНЯ", "Сейчас нельзя лечь спать.")
            return
        }
        val panel = createPanel("СПАЛЬНЯ", "Можно завершить день и восстановить силы")
        panel.addView(label("Сон завершит текущий день. После пробуждения будет создана контрольная точка.", 13f, Color.rgb(206, 215, 202)))
        panel.addView(menuButton("СПАТЬ ДО 07:00") {
            runCatching { session.sleepAndAutosave() }
                .onSuccess { slept -> if (slept) { enterGame(); Toast.makeText(this, "Новый день. Автосохранение создано.", Toast.LENGTH_SHORT).show() } }
                .onFailure { error -> Toast.makeText(this, "Не удалось сохранить игру: " + error.message, Toast.LENGTH_LONG).show() }
        })
        panel.addView(menuButton("ЗАКРЫТЬ") { closeStationPanel() })
        showOverlay(panel)
    }

    private fun showGeneratorPanel() {
        val engine = session.engine
        val panel = createPanel("ГЕНЕРАТОРНАЯ", "Энергосистема убежища")
        val powered = engine.bunkerPower.hasPower
        panel.addView(label(if (powered) "ПИТАНИЕ: ОСНОВНАЯ СЕТЬ" else "ПИТАНИЕ: АВАРИЙНЫЙ РЕЖИМ",
            15f, if (powered) Color.rgb(164, 210, 170) else Color.rgb(230, 165, 116)))
        panel.addView(label("Запас топлива: " + engine.bunkerPower.fuelHoursRemaining + " ч", 14f, Color.WHITE))
        panel.addView(label("Генератор: " + if (engine.bunkerPower.generatorRunning) "работает" else "остановлен",
            13f, Color.rgb(190, 198, 190)))
        panel.addView(menuButton(if (engine.bunkerPower.generatorRunning) "ОСТАНОВИТЬ ГЕНЕРАТОР" else "ЗАПУСТИТЬ ГЕНЕРАТОР",
            enabled = engine.bunkerPower.generatorRunning || engine.bunkerPower.fuelMinutesRemaining > 0) {
            engine.toggleGenerator()
            showGeneratorPanel()
        })
        panel.addView(menuButton("ЗАГРУЗИТЬ ДРОВА (+4 ЧАСА)", enabled = engine.inventory.count(ItemType.WOOD) > 0 &&
            engine.bunkerPower.fuelMinutesRemaining < BunkerPowerSystem.MAX_FUEL_MINUTES) {
            if (engine.refuelGenerator()) showGeneratorPanel()
        })
        panel.addView(label("При отключении генератора остаётся слабое аварийное освещение.", 12f,
            Color.rgb(151, 170, 157)))
        panel.addView(menuButton("ЗАКРЫТЬ") { enterGame() })
        showOverlay(panel)
    }
    private fun showDiningPanel() {
        val engine = session.engine
        val panel = createPanel("СТОЛОВАЯ", "Запасы экспедиции")
        panel.addView(label("Консервы: ${engine.inventory.count(ItemType.CANNED_FOOD)}", 15f, Color.WHITE))
        panel.addView(label("Вода: ${engine.inventory.count(ItemType.WATER)}", 15f, Color.WHITE))
        panel.addView(menuButton("СЪЕСТЬ КОНСЕРВЫ", enabled = engine.inventory.count(ItemType.CANNED_FOOD) > 0) {
            if (session.consumeFood()) showDiningPanel()
        })
        panel.addView(menuButton("ВЫПИТЬ ВОДУ", enabled = engine.inventory.count(ItemType.WATER) > 0) {
            if (session.drinkWater()) showDiningPanel()
        })
        panel.addView(menuButton("ЗАКРЫТЬ") { enterGame() })
        showOverlay(panel)
    }

    private fun showMedicalPanel() {
        val engine = session.engine
        val panel = createPanel("МЕДЧАСТЬ", "Медицинская станция и расходные материалы")
        val hurt = engine.needs.health < 100f
        panel.addView(label("Здоровье: ${engine.needs.health.toInt()}%", 16f, Color.WHITE))
        panel.addView(label(if (engine.bunkerPower.hasPower) "Станция запитана" else "Нет питания: доступна только ручная помощь",
            12f, if (engine.bunkerPower.hasPower) Color.rgb(164, 210, 170) else Color.rgb(230, 165, 116)))
        panel.addView(menuButton("ПРОВЕСТИ ОБРАБОТКУ (+50 ЗДОРОВЬЯ, 12 МИН.)",
            enabled = engine.canUseMedicalStation() && hurt && engine.inventory.count(ItemType.MEDICAL_KIT) > 0) {
            if (engine.treatAtMedicalStation()) showMedicalPanel()
        })
        panel.addView(menuButton("ИСПОЛЬЗОВАТЬ БИНТ (+15)", enabled = engine.inventory.count(ItemType.BANDAGE) > 0 && hurt) {
            if (engine.useBandage()) showMedicalPanel()
        })
        panel.addView(menuButton("ИСПОЛЬЗОВАТЬ АПТЕЧКУ (+35)", enabled = engine.inventory.count(ItemType.MEDICAL_KIT) > 0 && hurt) {
            if (engine.useMedicalKit()) showMedicalPanel()
        })
        panel.addView(label("Обработка на станции требует генератор и одну аптечку. Ручная помощь работает и без питания.", 11f,
            Color.rgb(151, 170, 157)))
        panel.addView(menuButton("ЗАКРЫТЬ") { enterGame() })
        showOverlay(panel)
    }

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
        panel.addView(menuButton("ОБНОВИТЬ") { openLatestUpdate() })
        panel.addView(menuButton("ВЫХОД") { finish() })
        showOverlay(panel)
    }

    private fun openLatestUpdate() {
        val releasesUrl = "https://github.com/DenisNekrut/Granit-Survival/releases/latest"
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releasesUrl))) }
            .onFailure { Toast.makeText(this, "Не удалось открыть страницу обновлений", Toast.LENGTH_LONG).show() }
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
        val panel = createPanel("ПАУЗА", "Состояние игры сохранено в памяти")
        panel.addView(label("Психологическое состояние", 16f, Color.WHITE))
        panel.addView(label("Стресс: " + session.engine.psychology.stress.toInt() + "  ·  Одиночество: " + session.engine.psychology.loneliness.toInt(), 13f, Color.LTGRAY))
        panel.addView(label("Усталость: " + session.engine.psychology.fatigue.toInt() + "  ·  Мораль: " + session.engine.psychology.morale.toInt(), 13f, Color.LTGRAY))
        panel.addView(menuButton("ПРОДОЛЖИТЬ") { enterGame() })
        panel.addView(menuButton("СТАТИСТИКА ПЕРСОНАЖА") { showCharacterStatistics() })
        panel.addView(menuButton("ГЛАВНОЕ МЕНЮ") { confirmReturnToMainMenu() })
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
        showOverlay(panel)
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
        panel.addView(menuButton("ЗАКРЫТЬ") { closeStationPanel() })
        showOverlay(panel)
    }

    private fun showCrafting() {
        val panel = createPanel("МАСТЕРСКАЯ", "Рецепты доступны рядом с верстаком")
        if (!session.engine.bunkerPower.hasPower) panel.addView(label("Нет питания: генератор необходимо запустить.", 13f, Color.rgb(230, 165, 116)))
        CraftingRecipe.all.forEach { recipe ->
            val costs = recipe.ingredients.entries.joinToString(" · ") { entry -> entry.key.displayName + ": " + entry.value }
            panel.addView(label(recipe.title + " — " + recipe.output.displayName, 16f, Color.WHITE))
            panel.addView(label("Материалы: " + costs + " · Время: " + recipe.craftMinutes + " мин.", 13f, Color.LTGRAY))
            panel.addView(menuButton("СОЗДАТЬ", enabled = session.engine.canUseWorkbench() && session.engine.canCraft(recipe.id)) {
                if (session.engine.craft(recipe.id)) {
                    Toast.makeText(this, recipe.title + ": готово", Toast.LENGTH_SHORT).show()
                    showCrafting()
                } else Toast.makeText(this, if (!session.engine.bunkerPower.hasPower) "Нет питания: запустите генератор" else "Не хватает материалов или места", Toast.LENGTH_SHORT).show()
            })
        }
        panel.addView(menuButton("ЗАКРЫТЬ") { closeStationPanel() })
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
        panel.addView(menuButton("ЗАКРЫТЬ") { closeStationPanel() })
        showOverlay(panel)
    }

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
            dp = ::dp,
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
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(8), dp(8), dp(8), dp(8))
            isClickable = true
            setOnClickListener { /* Consume taps inside the card; buttons handle their own actions. */ }
            addView(content)
        }
        val overlay = FrameLayout(this).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { if (activeGame) closeStationPanel() }
        }
        if (activeGame) {
            // The simulation and ambient audio continue while the interaction card is open.
            // The interaction card is a compact HUD layer, not a replacement screen.
            overlay.setBackgroundColor(Color.argb(72, 3, 9, 10))
            val cardWidth = minOf(dp(520), (resources.displayMetrics.widthPixels * 0.58f).toInt())
            val cardHeight = minOf(dp(680), (resources.displayMetrics.heightPixels * 0.90f).toInt())
            overlay.addView(scroll, FrameLayout.LayoutParams(cardWidth, cardHeight, Gravity.CENTER))
        } else {
            overlay.setBackgroundColor(Color.rgb(8, 14, 15))
            overlay.addView(scroll, FrameLayout.LayoutParams(dp(560), -1, Gravity.CENTER))
        }
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

    private fun Float.formatOneDecimal() = String.format("%.1f", this)
}
