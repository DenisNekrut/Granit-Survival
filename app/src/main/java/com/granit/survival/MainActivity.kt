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
import android.view.ViewGroup
import android.view.WindowManager
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/** Native Android menus wrap the custom GLES game surface; no UI framework/engine dependency. */
class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var gameSurface: GameSurface
    private lateinit var session: GameSession
    private var activeGame = false
    private var pausedGame = false
    private var introSequenceActive = false
    private var introSequenceIndex = 0
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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
        activityScope.cancel()
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
        // Authored landmark caches are not map-blocking tiles; resolve them by proximity first.
        if (engine.nearestWorldContainer(1.35f) != null) {
            showWorldContainerPanel()
            return
        }
        val hasNearbyResource = engine.tileMap.nearestResource(
            engine.player.x, engine.player.y, 1.35f
        ) != null
        val target = engine.tileMap.nearestInteractable(
            engine.player.x, engine.player.y, 1.35f
        )
        if (target == TileType.LOOT_CACHE) { showWorldContainerPanel(); return }
        stationPanelRouter.route(target, hasNearbyResource)
    }

    private fun showWorldContainerPanel() = bunkerStationPanels.showWorldContainer()

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
        if (introSequenceActive) {
            introSequenceIndex++
            showOpeningScenePage()
        } else if (activeGame) enterGame() else showPauseMenu()
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
                session.saves.hasAutoSaveFile() ||
                    session.saves.listSlots().any { it.exists }
            },
            startNewGame = ::startNewGame,
            loadMostRecent = session::loadMostRecent,
            loadError = { session.lastLoadError },
            enterGame = ::enterGame,
            showSaveSlots = { showSaveSlots(false) },
            showSettings = ::showSettings,
            exit = ::finish
        ).showMainMenu()
    }

    private fun startNewGame() {
        session.startNewGame()
        enterGame()
        introSequenceActive = true
        introSequenceIndex = 0
        showOpeningScenePage()
    }

    /** Short presentation-only opening; reading it does not consume in-game time. */
    private fun showOpeningScenePage() {
        val pages = listOf(
            "Сначала пришёл звук. Ровный низкий гул где-то за стеной. Он не становился громче и не стихал — просто был.",
            "Андрей открыл глаза. Над ним — низкий потолок убежища, тусклая лампа и следы конденсата на металлической панели. Воздух пах пылью и холодным железом.",
            "Он попытался вспомнить, как оказался здесь. В памяти остались обрывки, но ни один не складывался в ответ. Даже собственные последние дни казались чужими.",
            "На стене мерцал индикатор питания. Где-то рядом находились генераторная, запасы и радиостанция. Прежде чем выйти наружу, стоило проверить убежище."
        )
        if (introSequenceIndex >= pages.size) {
            introSequenceActive = false
            showChapterOneBriefing()
            return
        }
        val panel = createPanel("ГЛАВА I · ТИХОЕ УБЕЖИЩЕ", "ПРОБУЖДЕНИЕ · ${introSequenceIndex + 1}/${pages.size}")
        panel.addView(label(pages[introSequenceIndex], 15f, GranitPalette.textPrimary))
        panel.addView(menuButton(if (introSequenceIndex == pages.lastIndex) "НАЧАТЬ ОСМОТР" else "ДАЛЕЕ") {
            introSequenceIndex++
            showOpeningScenePage()
        })
        showOverlay(panel)
    }

    private fun showChapterOneBriefing() {
        val panel = createPanel("ПЕРВЫЕ ШАГИ", "ЗАДАЧА ОБНОВЛЕНА")
        panel.addView(label(
            "Убежище пока даёт укрытие. Осмотри генераторную, проверь запасы и радиоприёмник. Решение о выходе наружу останется за тобой.",
            14f, GranitPalette.textPrimary
        ))
        panel.addView(menuButton("ПОНЯТНО") {
            introSequenceActive = false
            enterGame()
        })
        showOverlay(panel)
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
        val story = session.engine.storyProgress
        val panel = createPanel("ЖУРНАЛ ИСТОРИИ И ЭКСПЕДИЦИЙ", discovered.size.toString() + " / " + PointOfInterestCatalog.all().size + " мест обнаружено")
        panel.addView(label("ГЛАВА I · ${StoryProgressSystem.CHAPTER_ONE_TITLE.uppercase()}", 16f, Color.rgb(207, 194, 155), true))
        panel.addView(label("Начальный этап: ${story.completedObjectiveCount} / ${story.objectives.size} задач выполнено", 13f, Color.LTGRAY))
        story.currentObjective?.let { objective ->
            panel.addView(label("ТЕКУЩАЯ ЗАДАЧА · ${objective.title}", 14f, Color.WHITE, true))
            panel.addView(label(objective.description, 13f, Color.LTGRAY))
        } ?: panel.addView(label("Начальный этап убежища завершён. Продолжение главы будет открываться по мере реализации следующих сюжетных сцен.", 13f, Color.LTGRAY))
        story.objectives.filter { story.isCompleted(it) }.forEach { objective ->
            panel.addView(label("✓ ${objective.title}", 12f, Color.rgb(151, 190, 151)))
        }
        panel.addView(label("ЗАМЕТКИ АНДРЕЯ", 15f, Color.rgb(207, 194, 155), true))
        val observations = story.observations()
        if (observations.isEmpty()) {
            panel.addView(label("Пока нет личных заметок. Осматривай помещения убежища — важные детали будут сохраняться здесь.", 13f, Color.LTGRAY))
        } else {
            observations.forEach { observation ->
                panel.addView(label(observation.title.uppercase(), 13f, Color.WHITE, true))
                panel.addView(label(observation.note, 12f, Color.LTGRAY))
            }
        }
        if (story.hasEvent(StoryEventIds.RADIO_NOTES_CROSS_CHECKED)) {
            panel.addView(label("ПЕРВАЯ ГИПОТЕЗА АНДРЕЯ", 15f, Color.rgb(207, 194, 155), true))
            val theoryRadio = story.hasEvent(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE)
            val theoryEquipment = story.hasEvent(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST)
            when {
                theoryRadio -> panel.addView(label("Выбрано: проверить связь в другом месте. Андрей пока не считает эту версию доказанной.", 13f, Color.LTGRAY))
                theoryEquipment -> panel.addView(label("Выбрано: сначала проверить оборудование. Андрей пока не считает эту версию доказанной.", 13f, Color.LTGRAY))
                else -> {
                    panel.addView(label("Что проверить в первую очередь? Это предварительная версия, а не окончательный вывод.", 13f, Color.LTGRAY))
                    panel.addView(menuButton("ПРОВЕРИТЬ СВЯЗЬ В ДРУГОМ МЕСТЕ") {
                        story.chooseFirstTheory(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE)
                        showJournal()
                    })
                    panel.addView(menuButton("СНАЧАЛА ПРОВЕРИТЬ ОБОРУДОВАНИЕ") {
                        story.chooseFirstTheory(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST)
                        showJournal()
                    })
                }
            }
        }
        if (session.engine.isPoiDiscovered("forest_radio_cache")) {
            panel.addView(menuButton("ИЗУЧИТЬ СТАРЫЙ РЕТРАНСЛЯТОР") {
                showForestRelayInspection()
            })
        }
        if (story.hasEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)) {
            panel.addView(label("НОВОЕ НАПРАВЛЕНИЕ · СТАРАЯ ДОРОГА", 14f, Color.rgb(207, 194, 155), true))
            panel.addView(label("На вышке направление сигнала указывало за пределы леса. Проверь брошенный автомобиль на старой дороге: в нём может сохраниться карта с отметками.", 13f, Color.LTGRAY))
            if (session.engine.isPoiDiscovered("road_vehicle")) {
                if (story.hasEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)) {
                    panel.addView(label("Карта изучена. Новая точка для экспедиции отмечена в журнале.", 12f, Color.rgb(151, 190, 151)))
                } else {
                    panel.addView(menuButton("ИЗУЧИТЬ КАРТУ В БРОШЕННОМ АВТОМОБИЛЕ") {
                        session.engine.inspectRoadSignalMap()
                        showJournal()
                    })
                }
            } else {
                panel.addView(label("Цель: добраться до зоны «Дорога» и обнаружить брошенный автомобиль.", 12f, Color.LTGRAY))
            }
        }
        if (story.hasEvent(StoryEventIds.ROAD_MAP_SIGNAL_TRACE_FOUND)) {
            panel.addView(label("СЛЕДУЮЩАЯ ЦЕЛЬ · РАЗБИТЫЙ ПОСТ В РУИНАХ", 14f, Color.rgb(207, 194, 155), true))
            if (story.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_REACHED)) {
                if (story.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED)) {
                    panel.addView(label("Отметка изучена. Дополнительное направление сохранено в журнале.", 12f, Color.rgb(151, 190, 151)))
                } else {
                    panel.addView(label("Разбитый пост найден. Осмотри табличку и сверь её маркировку с картой.", 13f, Color.LTGRAY))
                    panel.addView(menuButton("ИЗУЧИТЬ ОТМЕТКУ НА ПОСТУ") {
                        showRuinsMarkerInspection()
                    })
                }
            } else {
                panel.addView(label("Отправляйся в зону «Руины» и найди разбитый пост. Его отметка совпадает с условным знаком на карте.", 13f, Color.LTGRAY))
            }
        }
        if (story.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED)) {
            panel.addView(label("НОВАЯ ТОЧКА ЭКСПЕДИЦИИ · ВНЕШНИЙ МАРШРУТ", 14f, Color.rgb(207, 194, 155), true))
            when {
                story.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_FOUND) -> {
                    if (story.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED)) {
                        panel.addView(label("Указатель изучен. Схема технического проезда сохранена на карте.", 12f, Color.rgb(151, 190, 151)))
                    } else {
                        panel.addView(label("Указатель линии связи найден. Осмотри его и проверь, куда ведёт технический проезд.", 13f, Color.LTGRAY))
                        panel.addView(menuButton("ИЗУЧИТЬ УКАЗАТЕЛЬ ЛИНИИ СВЯЗИ") {
                            showRuinsOuterRouteInspection()
                        })
                    }
                }
                else ->
                    panel.addView(label("Дополнительная маркировка указывает за разбитый пост. Исследуй участок руин и найди указатель старой линии связи.", 13f, Color.LTGRAY))
            }
        }
        if (story.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED)) {
            panel.addView(label("СЛЕДУЮЩАЯ ЦЕЛЬ · СЛУЖЕБНЫЙ ВХОД", 14f, Color.rgb(207, 194, 155), true))
            if (story.hasEvent(StoryEventIds.RUINS_SERVICE_ACCESS_FOUND)) {
                panel.addView(label("Служебный вход найден. На двери видна маркировка коммуникационного узла, но проход закрыт.", 12f, Color.rgb(151, 190, 151)))
            } else {
                panel.addView(label("Следуй по техническому проезду к дальней окраине руин и найди служебный вход старого коммуникационного узла.", 13f, Color.LTGRAY))
            }
        }
        panel.addView(label("ЗАПИСИ ОБ ИССЛЕДОВАНИИ", 15f, Color.rgb(207, 194, 155), true))
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

    private fun showRuinsMarkerInspection() {
        val story = session.engine.storyProgress
        val panel = createPanel("РАЗБИТЫЙ ПОСТ", "Руины · сюжетная отметка")
        panel.addView(label("Андрей подходит к табличке и сверяет её знак с отметкой на найденной карте.", 14f, Color.WHITE))
        panel.addView(label("Под основным символом заметна дополнительная маркировка. Она указывает на следующий участок маршрута, но сама по себе не объясняет, кто и когда оставил эти обозначения.", 13f, Color.LTGRAY))
        if (story.hasEvent(StoryEventIds.RUINS_SIGNAL_MARKER_INSPECTED)) {
            panel.addView(label("РЕЗУЛЬТАТ ОСМОТРА СОХРАНЁН", 13f, Color.rgb(151, 190, 151), true))
        } else {
            panel.addView(menuButton("ЗАПИСАТЬ МАРКИРОВКУ В ЖУРНАЛ") {
                session.engine.inspectRuinsSignalMarker()
                showRuinsMarkerInspection()
            })
        }
        panel.addView(menuButton("НАЗАД К ЖУРНАЛУ") { showJournal() })
        showOverlay(panel)
    }

    private fun showRuinsOuterRouteInspection() {
        val story = session.engine.storyProgress
        val panel = createPanel("УКАЗАТЕЛЬ ЛИНИИ СВЯЗИ", "Руины · новая точка маршрута")
        panel.addView(label("Андрей осматривает стойку указателя. Под слоем пыли проступает маркировка, совпадающая с дополнительным знаком на разбитом посту.", 14f, Color.WHITE))
        panel.addView(label("На обратной стороне сохранилась схема технического проезда к дальней окраине руин. Часть линий стёрта, но направление читается достаточно ясно, чтобы отметить его на карте.", 13f, Color.LTGRAY))
        if (story.hasEvent(StoryEventIds.RUINS_OUTER_ROUTE_INSPECTED)) {
            panel.addView(label("СХЕМА ТЕХНИЧЕСКОГО ПРОЕЗДА СОХРАНЕНА", 13f, Color.rgb(151, 190, 151), true))
        } else {
            panel.addView(menuButton("СОХРАНИТЬ СХЕМУ НА КАРТЕ") {
                session.engine.inspectRuinsOuterRoute()
                showRuinsOuterRouteInspection()
            })
        }
        panel.addView(menuButton("НАЗАД К ЖУРНАЛУ") { showJournal() })
        showOverlay(panel)
    }

    private fun showForestRelayInspection() {
        val story = session.engine.storyProgress
        val panel = createPanel("СТАРЫЙ РЕТРАНСЛЯТОР", "Лес · обнаруженная точка интереса")
        panel.addView(label("Андрей возвращается к записям об обнаруженном устройстве.", 14f, Color.WHITE))
        when {
            story.hasEvent(StoryEventIds.THEORY_CHECK_RADIO_ELSEWHERE) -> {
                panel.addView(label("Выбранная версия: проверить связь в другом месте.", 13f, Color.rgb(207, 194, 155), true))
                panel.addView(label("В памяти всплывает характер помех из убежища. У ретранслятора шум звучал иначе, словно приём менялся вместе с местом. Андрей не может пока определить, почему это происходит.", 13f, Color.LTGRAY))
            }
            story.hasEvent(StoryEventIds.THEORY_CHECK_EQUIPMENT_FIRST) -> {
                panel.addView(label("Выбранная версия: сначала проверить оборудование.", 13f, Color.rgb(207, 194, 155), true))
                panel.addView(label("Андрей сосредотачивается на корпусе и старых соединениях. На устройстве заметны следы прежнего ремонта, но внешний осмотр не позволяет установить, исправен ли ретранслятор.", 13f, Color.LTGRAY))
            }
            else -> {
                panel.addView(label("Андрей пока не выбрал, с какой версии начинать. Он отмечает только то, что устройство давно не обслуживали.", 13f, Color.LTGRAY))
            }
        }
        if (story.hasEvent(StoryEventIds.FOREST_RELAY_REPAIRED)) {
            panel.addView(label("РЕТРАНСЛЯТОР РАБОТАЕТ", 14f, Color.rgb(151, 190, 151), true))
            panel.addView(label("Питание восстановлено. Устройство передало короткий сигнал, но устойчивой связи пока нет.", 13f, Color.LTGRAY))
            if (session.engine.isPoiDiscovered("forest_watch_post")) {
                if (story.hasEvent(StoryEventIds.FOREST_WATCH_POST_SIGNAL_CHECKED)) {
                    panel.addView(label("Показания на лесной вышке уже сверены. Запись добавлена в журнал.", 12f, Color.rgb(151, 190, 151)))
                } else {
                    panel.addView(menuButton("СВЕРИТЬ СИГНАЛ НА ЛЕСНОЙ ВЫШКЕ") {
                        session.engine.inspectForestWatchPostSignal()
                        showForestRelayInspection()
                    })
                }
            } else {
                panel.addView(label("Для следующей проверки найдите старую лесную вышку.", 12f, Color.LTGRAY))
            }
        } else if (story.hasEvent(StoryEventIds.FOREST_RELAY_INSPECTED)) {
            panel.addView(label("Результат осмотра уже записан в журнал.", 13f, Color.rgb(151, 190, 151)))
            panel.addView(label("Для ремонта нужны: металл ×2, ткань ×1. Работа займёт 20 игровых минут.", 13f, Color.LTGRAY))
            if (session.engine.canRepairDiscoveredForestRelay()) {
                panel.addView(menuButton("ВОССТАНОВИТЬ РЕТРАНСЛЯТОР · 20 МИН.") {
                    session.engine.repairDiscoveredForestRelay()
                    showForestRelayInspection()
                })
            } else {
                panel.addView(label("Пока недостаточно материалов для ремонта.", 12f, Color.rgb(207, 170, 130)))
            }
        } else {
            panel.addView(menuButton("ЗАПИСАТЬ РЕЗУЛЬТАТ ОСМОТРА") {
                session.engine.inspectDiscoveredForestRelay()
                showForestRelayInspection()
            })
        }
        panel.addView(menuButton("НАЗАД К ЖУРНАЛУ") { showJournal() })
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
            scope = activityScope,
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
                intArrayOf(GranitPalette.panelTop, GranitPalette.panelBottom)
            ).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), GranitPalette.panelStroke)
            }
        }
        content.addView(label(title, 22f, GranitPalette.title, true).apply {
            letterSpacing = 0.045f
        })
        content.addView(label(subtitle, 12f, GranitPalette.subtitle).apply {
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
            addView(content, ViewGroup.LayoutParams(-1, -2))
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
            setBackgroundColor(if (activeGame) GranitPalette.overlay else Color.rgb(8, 14, 15))
        }

        val availableWidth = (root.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels).coerceAtLeast(1)
        val availableHeight = (root.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels).coerceAtLeast(1)
        val widthFraction = if (activeGame) 0.76f else 0.88f
        val heightFraction = if (activeGame) 0.82f else 0.90f
        val maxWidth = dp(if (activeGame) 720 else 760)
        val maxHeight = dp(if (activeGame) 560 else 680)
        val minimumWidth = minOf(dp(220), availableWidth)
        val minimumHeight = minOf(dp(200), availableHeight)
        val cardWidth = minOf(availableWidth, maxWidth, (availableWidth * widthFraction).toInt().coerceAtLeast(minimumWidth))
        val cardHeight = minOf(availableHeight, maxHeight, (availableHeight * heightFraction).toInt().coerceAtLeast(minimumHeight))
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
            setTextColor(if (enabled) GranitPalette.textPrimary else Color.GRAY)
            background = (if (enabled) {
                GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(GranitPalette.buttonTop, GranitPalette.buttonBottom)
                )
            } else {
                // A one-colour GradientDrawable is invalid on Android: use a solid fill.
                GradientDrawable().apply { setColor(GranitPalette.buttonDisabled) }
            }).apply {
                cornerRadius = dp(8).toFloat()
                setStroke(dp(1), if (enabled) GranitPalette.buttonStroke else GranitPalette.buttonDisabledStroke)
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
