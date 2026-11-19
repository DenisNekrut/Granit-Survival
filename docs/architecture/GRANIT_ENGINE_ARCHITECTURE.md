# Granit Engine и Granit Survival — план разработки v2.1

**Дата ревизии:** 2026-11-19  
**Статус:** актуальная дорожная карта; заменяет Unity-план версии 1.0  
**Основная платформа:** Android, альбомная ориентация  
**Движок:** собственный Granit Engine с нуля  
**Стек:** Kotlin/JVM, Android SDK как платформенный слой, EGL14 и OpenGL ES 2.0  
**Репозиторий:** https://github.com/DenisNekrut/Granit-Survival

> Unity, Godot и MonoGame не использовать. Не менять `main` напрямую. Разработка ведётся в feature-ветках и через Pull Request. Ни один этап не объявляется завершённым без проверяемого критерия.

## 1. Решения, которые считаются утверждёнными

### 1.1 Движок и платформа
- Granit Engine разрабатывается самостоятельно, без готового игрового движка.
- Android — первая и основная платформа, альбомная ориентация и сенсорное управление.
- Kotlin/JVM — основной язык.
- Android SDK отвечает за Activity/lifecycle, поверхность, ввод и файловый доступ.
- EGL14/OpenGL ES 2.0 — низкоуровневый графический backend, а не игровой движок.
- Игровая логика отделяется от Android-слоя, чтобы не блокировать возможное расширение платформ в будущем.

### 1.2 Концепция игры
- Жанр: top-down 2D survival с пиксельной графикой.
- Игрок живёт в убежище «Гранит», исследует внешний мир, собирает припасы, возвращается, хранит ресурсы, читает, создаёт предметы и развивает убежище.
- Основной цикл: убежище → подготовка → вылазка → исследование/сбор → возвращение → хранение/крафт/чтение → сон → следующий день.
- Игрок сам решает, когда отправиться в вылазку и когда лечь спать. Можно провести день в убежище; при этом продолжается расход запасов и могут происходить внешние события.
- Основные зоны: убежище, лес, руины и дорога.
- Мир и события подаются через окружение, находки, книги и радио.

### 1.3 Персонаж и выживание
- Показатели: здоровье, голод, жажда и энергия.
- Инвентарь: сетка 4×4 со стеками и ограничением вместимости.
- Основные предметы/ресурсы прототипа: древесина, металл, ткань, консервы и вода.
- Действия и переходы расходуют игровое время.
- Сон начинается по решению игрока у кровати, переводит игру к 07:00 следующего дня и восстанавливает энергию.
- HUD должен показывать актуальные показатели, день, время, зону и важную обратную связь.

### 1.4 Контрольная точка и смерть
- Контрольная точка создаётся автоматически при пробуждении перед вылазкой.
- При гибели персонаж возвращается к состоянию именно этого пробуждения.
- Откатываются все изменения после контрольной точки: игровое время, инвентарь, хранилище, ресурсы и состояние всех зон, показатели выживания, психологическое состояние, открытия и знания.
- Не использовать прежнее правило потери 50% ресурсов: оно устарело и противоречит утверждённому пользователем откату к пробуждению.
- После успешного возвращения прогресс закрепляется следующим сохранением/контрольной точкой.
- Ручные сохранения доступны отдельно от автоматической контрольной точки.

### 1.5 Психология и библиотека
- Независимые показатели: стресс, одиночество, усталость и мораль.
- Эффекты умеренные и ограниченные; не вводить постоянные наказания за неудачную вылазку.
- Категории книг: художественные, научные, технические руководства, дневники, журналы и учебники.
- Чтение расходует игровое время и может влиять на мораль/стресс.
- Книги могут открывать знания, навыки и рецепты.
- Прогресс чтения, знания и рецепты входят в сохранение и checkpoint, поэтому незакреплённый прогресс откатывается при гибели.

## 2. Архитектура Granit Engine

### 2.1 Слои
- **Android Platform:** MainActivity, GameSurface, lifecycle, сенсорный ввод, AndroidStorage и SettingsRepository.
- **Engine Core:** запуск/остановка, игровой цикл, fixed timestep, время, события и конфигурация.
- **Graphics:** Renderer2D, Camera2D, TileMapRenderer, Sprite/Atlas, pixel-perfect sampling, HUD и debug overlay.
- **Input:** нормализованные pointer-события, multi-touch, виртуальный джойстик и экранные кнопки.
- **World/Physics:** сцены, сущности, Transform2D, коллизии, tile map, поиск взаимодействий и переходы.
- **UI:** главное меню, HUD, пауза, сохранения, настройки, инвентарь, библиотека, журнал, хранилище и мастерская.
- **Assets/Audio:** загрузка и кеширование спрайтов/звуков, атласы и аудио backend.
- **Save:** версионированный JSON, атомарная запись, ручные слоты и отдельная контрольная точка.
- **Game layer:** Player, Inventory, Survival, WorldTime, Psychology, Library, Crafting, Bunker, Expedition, Weather и Checkpoint.

### 2.2 Технические правила
- Все OpenGL-вызовы выполняются в render thread при активном EGL-контексте.
- Обрабатывать pause/resume, потерю и пересоздание Surface/EGL context.
- Симуляция использует fixed timestep; render rate не должен менять игровые правила.
- Координаты мира отделены от физических пикселей экрана.
- Сохранения имеют версию схемы и записываются атомарно.
- UI не должен менять игровое состояние в обход игровых систем.
- Системы игры по возможности остаются platform-independent.
- Не заявлять о проверке на устройстве по одному лишь успешному CI.

## 3. Текущее состояние реализации

### 3.1 Подтверждено в main (PR #1 объединён)
- [x] Android shell, Kotlin/JVM, альбомная ориентация.
- [x] SurfaceView, отдельный render thread, EGL14/OpenGL ES 2.0.
- [x] Базовый fixed-step loop и первый renderer.
- [x] TileMap убежища, состояние игрока, перемещение и коллизии.
- [x] Camera2D, сенсорный джойстик и multi-touch action pointer.
- [x] Базовые типы тайлов и точки взаимодействия.
- [x] Инвентарь 4×4, стеки, ограничения вместимости и подбор пяти видов ресурсов.
- [x] GitHub Actions для unit-тестов, Debug APK и публикации артефакта.

### 3.2 Реализовано и объединено (PR #2, PR #5)
- [x] Отдельное хранилище убежища и перенос предметов.
- [x] Игровые часы, потребности и сон до 07:00.
- [x] Wake-up checkpoint и откат состояния.
- [x] Версионированные JSON-сохранения, три ручных слота и отдельный autosave.
- [x] Главное меню, продолжение/новая игра, пауза, менеджер сохранений и настройки.
- [x] Четыре связанные зоны: убежище, лес, руины и дорога.
- [x] Переходы между зонами, сохранение ресурсов зон и тесты достижимости выходов.
- [x] HUD реальных показателей выживания, дня/времени, зоны и погоды.
- [x] Психологические показатели и их включение в save/checkpoint.
- [x] Библиотека из пяти категорий книг, чтение и открытие рецепта аптечки техническим руководством.
- [x] Радио с ежедневными сообщениями и прогнозом погоды.
- [x] Пять погодных состояний и умеренное влияние погоды на передвижение снаружи.
- [x] Шесть точек интереса, автоматическое обнаружение и журнал экспедиций.
- [x] Рецепты бинта и аптечки, применение медицинских предметов.
- [x] PixelSpriteAtlas, idle/walk персонажа, спрайты ресурсов, медицинских предметов и мебели, визуальные варианты четырёх зон.
- [x] Unit-тесты игровых систем, сохранений, отката, переходов, библиотеки, радио, погоды, журнала и атласа.

**Статус интеграции:** PR #2 объединён в `main` (2026-11-18), PR #5 (адаптивный экран, управление по краям и ambient-аудио) объединён 2026-11-19. Открытым остаётся PR #6, его CI красный; PR #3 устарел и конфликтует по merge. Успешный CI подтверждает unit tests и Debug APK, но не физическое Android-тестирование.

## 4. Обновлённая дорожная карта

Статусы относятся к репозиторию на момент ревизии 2026-11-19. Реализация в открытом PR обозначается REVIEW, а не DONE.

| Этап | Содержание | Статус | Критерий закрытия |
|---|---|---|---|
| 0. Engine bootstrap | Android shell, EGL/GLES2, render thread, lifecycle | DONE — PR #1 merged | CI и код в main |
| 1. Базовый мир | TileMap, игрок, камера, коллизии, touch/multi-touch | DONE — PR #1 merged | Перемещение и столкновения; CI |
| 2. Инвентарь и сбор | 4×4, стеки, ресурсы, вместимость | DONE — main | Unit-тесты и CI |
| 3. Убежище и базовое выживание | Хранилище, перенос, еда/вода, потребности, время и сон | DONE — merged (PR #2) | CI, review/merge и ручная проверка |
| 4. Сохранения и checkpoint | JSON, три слота, autosave при пробуждении, полный rollback | DONE — merged (PR #2) | Тесты round-trip/rollback, CI и проверка на устройстве |
| 5. Меню и настройки | Главное меню, новая игра/продолжить, пауза, сохранения, настройки | DONE — merged (PR #2, PR #5) | Проверить все переходы UI и восстановление состояния |
| 6. Карты и экспедиции | Четыре зоны, выходы, ресурсы зон, POI и журнал | DONE — merged (PR #2) | Все выходы доступны, ресурсы/открытия корректно сохраняются и откатываются |
| 7. Психология, библиотека и радио | Четыре показателя, шесть категорий книг, чтение, знания, ежедневные эфиры | DONE — merged (PR #2); часть библиотеки требует расширения | Все категории представлены, эффекты/время/checkpoint покрыты тестами |
| 8. Погода и события | Пять погодных состояний, прогноз, события в убежище и дни без вылазки | DONE — merged (PR #2); расширение BACKLOG | День в убежище имеет понятные последствия; события воспроизводимы и откатываются |
| 9. Графика и анимация | Полный art pass, внешние sprite sheets, GPU texture atlas, анимации окружения и персонажа | IN PROGRESS | Единый стиль, читаемые спрайты, корректное масштабирование, визуальная проверка на телефоне |
| 10. Крафт и развитие убежища | Дополнительные рецепты, мастерская, улучшения и потребление ресурсов | BACKLOG / частично реализовано | Рецепты имеют условия, стоимость, результат, UI и тесты |
| 11. NPC и динамика экспедиций | NPC, абстрактные угрозы, цели, сюжетные находки и события зон | BACKLOG | Неграфичные игровые состояния, понятная логика, тесты и связь с checkpoint |
| 12. Аудио и настройки | Реальная громкость музыки/эффектов, вибрация, качество графики | IN PROGRESS | Каждый параметр действительно влияет на соответствующую подсистему и переживает перезапуск |
| 13. QA Android | Установка, lifecycle, управление, меню, сон, rollback, сохранения, FPS и энергопотребление | DEVICE TEST | Протокол проверки на физическом Android-устройстве без блокирующих дефектов |
| 14. Релиз | Подпись, release APK/AAB, версия, описание, финальная оптимизация | BACKLOG | Успешная release-сборка и проверенная установка |

## 5. Подробные требования, которые нельзя потерять

### 5.1 UI и управление
- Главное меню с новой игрой, продолжением, сохранениями и настройками.
- В менеджере сохранений: просмотр, загрузка, перезапись и удаление слотов с подтверждением.
- Пауза и возврат в игру без сброса сессии.
- Сенсорный джойстик и отдельная кнопка действия; одновременные касания не должны конфликтовать.
- Настройки: чувствительность, масштаб и прозрачность управления, громкость музыки/эффектов, качество графики и вибрация. Сохранять параметры и подключать их к реальным системам.
- HUD: здоровье, голод, жажда, энергия, день, время, текущая зона, погода и найденные ориентиры.

### 5.2 Игровой цикл и предметы
- Убежище содержит кровать, хранилище, верстак, книжный шкаф и радио.
- Сбор, переходы, чтение, радио и крафт расходуют игровое время.
- Еда и вода восстанавливают соответствующие потребности.
- Предметы можно переносить между инвентарём и хранилищем с проверкой вместимости.
- Состояние каждой зоны и её ресурсы не должны сбрасываться при обычном переходе между зонами.
- Сон доступен у кровати; контрольная точка создаётся после пробуждения.
- Смерть — возврат к checkpoint, а не штраф с потерей половины инвентаря.

### 5.3 Психология и книги
- Стресс, одиночество, усталость и мораль изменяются от времени, сна, перемещений, чтения и событий.
- Не делать психологические эффекты чрезмерными или постоянными наказаниями.
- Поддержать шесть заявленных категорий: художественная литература, научные книги, технические руководства, дневники, журналы и учебники.
- У каждой книги должны быть время чтения, эффект/содержимое и правила сохранения результата.
- Новые знания и рецепты, полученные после пробуждения, откатываются при гибели до успешного закрепления.

### 5.4 Мир и графика
- Основные зоны: убежище, лес, руины и дорога; переходы должны быть физически достижимы.
- Точки интереса имеют русские названия и описания в журнале; повторное обнаружение не создаёт дубликаты.
- Пиксельная графика должна иметь единый стиль и палитру; placeholder-прямоугольники постепенно заменяются спрайтами.
- Персонаж должен иметь idle/walk анимации; мебель, ресурсы, предметы и окружение — отдельные читаемые модели/спрайты.
- Визуальные изменения не должны ломать коллизии, доступность выходов и тесты связности карт.

## 6. Правила выполнения и отслеживания
1. Каждое новое пожелание пользователя сначала добавляется в `docs/PROJECT_BACKLOG.md` с ID, приоритетом, статусом и критерием готовности.
2. Перед началом работы сверяются эта дорожная карта, backlog, текущая ветка, открытые PR и последние Actions.
3. Каждая логическая задача выполняется в feature-ветке и проходит CI через PR.
4. Не объединять PR и не менять `main` без явного разрешения пользователя.
5. После каждого этапа обновлять статусы и историю, не удаляя выполненные требования.
6. Статусы разделять: код написан, unit-тесты/CI прошли, PR объединён, проверено на Android-устройстве.
7. При провале CI изучать конкретный job/log, исправлять причину и повторно запускать проверки.
8. Не объявлять этап готовым только по наличию файла или кнопки в интерфейсе.
9. Следующий этап можно начинать после фиксации результата предыдущего; пользователь просил продолжать разработку последовательно, не ожидая отдельной команды после каждого небольшого этапа.

## 7. Ближайшая последовательность работ
1. Разблокировать PR #6: его CI падает на `Camera2DTest.cameraStopsAtWorldEdges` — тест ожидает мгновенного выхода камеры на границу мира, хотя `follow()` выполняет экспоненциальное сглаживание за один шаг.
2. После интеграции провести ручной Android QA главного меню, настроек, сохранений, хранилища, сна и rollback.
3. Завершить внешний sprite-sheet/texture-atlas pipeline и полный art pass всех четырёх зон.
4. Расширить библиотеку до шести категорий и завершить контент/эффекты чтения.
5. Расширить крафт и развитие убежища.
6. Добавить события дней без вылазки, сюжетные находки, цели экспедиций и NPC/абстрактные угрозы.
7. Подключить звук, вибрацию и качество графики к реальным подсистемам.
8. Провести балансировку, производительность и подготовить релизную сборку.

## 8. Проверки и ограничения статуса
- CI включает `:app:testDebugUnitTest`, `:app:assembleDebug` и загрузку APK artifact.
- Локальная проверка ветки `main` (JDK 17, Gradle 8.13, Android SDK 36): 136 unit-тестов и `:app:assembleDebug` проходят, APK ≈ 1.06 МБ.
- Физическое Android-тестирование не подтверждено.
- PR #2 и PR #5 объединены; PR #6 открыт и красный по CI; PR #3 закрыт и заменён PR #4.
- Старый файл `GRANIT_SURVIVAL_MASTER_PLAN` сохранён как исторический Unity-план; актуальные источники истины — этот документ и `docs/PROJECT_BACKLOG.md`.


## Дополнение: внешний sprite-atlas pipeline (2026-11-19)

Введена первая часть конвейера ресурсов:

- `SpriteAtlasManifest` — platform-independent описание версии, размеров текстуры, фильтра, кадров и pivot.
- Координаты кадров хранятся в пикселях с началом в левом верхнем углу; `uvRect` преобразует их в нормализованные координаты.
- `SpriteAtlasManifestValidator` проверяет версию, путь к texture asset, размеры, уникальность идентификаторов, pivot и границы каждого кадра.
- `AndroidSpriteAtlasLoader` читает JSON из Android assets и декодирует PNG в Bitmap, сверяя реальные размеры с manifest.
- Пример формата находится в `app/src/main/assets/sprites/atlas-manifest.example.json`.

Следующая часть должна выполняться в render thread: загрузка Bitmap в GLES2 texture, nearest/linear sampler, textured shader, UV batching и повторная загрузка после потери EGL context. До реализации этой части `PixelSpriteAtlas` остаётся действующим процедурным fallback; пример manifest не является готовым игровым PNG-атласом.


## GLES2 textured sprite rendering (2026-11-19)

`PixelRenderer` now owns two shader programs: the original flat-color shader and a textured shader using position, UV and `sampler2D`. `PixelSpriteTextureAtlas` packs the existing pixel-art definitions into a transparent RGBA bitmap and produces a matching `SpriteAtlasManifest`. The bitmap is uploaded on the EGL render thread through `GlesTexture` with nearest-neighbour sampling.

Atlas sprite calls resolve their manifest frame, convert pixel coordinates to normalized UVs and draw a textured quad. Horizontal facing is implemented by reversing the U coordinates. Pending flat geometry is flushed before a textured quad, preserving painter's order; the flat shader is restored after each sprite. Small direct buffers are reused instead of allocating new ones per sprite. Renderer GPU resources are released while the EGL context is still current.

Current limitation: the GPU path is live, but the atlas source is generated from the existing code-defined sprites. The Android JSON/PNG loader is implemented but not yet selected as the production source. Per-sprite textured draws are also still immediate; a future UV batcher must preserve interleaved flat/textured painter order before it can reduce those calls.


### Textured sprite batching update

Consecutive textured quads from the active atlas are now accumulated in reusable position and UV buffers and submitted together with one `glDrawArrays`. The renderer flushes pending flat geometry before adding textured quads, and flushes pending textured quads before appending flat geometry. This boundary-based approach preserves painter's order for overlapping scene objects. It batches only adjacent textured draws; interleaved flat geometry intentionally splits batches.


### External atlas runtime selection

At renderer initialization, the EGL render thread passes the Android `AssetManager` to `PixelRenderer.create`. The renderer attempts to load `sprites/atlas-manifest.json` and its referenced PNG through `AndroidSpriteAtlasLoader`, validates dimensions and frame bounds, then uploads the bitmap to GLES2. If the external manifest/PNG is not yet present or fails validation/decoding, the renderer logs the reason and builds the existing code-defined atlas as a fallback. The production art PNG and manifest still need to be authored and added to app assets.


### Sprite quad geometry tests

Quad vertex and UV emission is extracted into the platform-independent `SpriteQuadGeometry` helper. It writes two triangles into caller-owned buffers, avoiding per-sprite allocation. Unit tests cover vertex ordering, top-left atlas UV mapping and horizontal mirroring. Renderer batching continues to own buffer lifecycle and draw submission.


### Zone-specific environment sprite pass

The generated atlas now includes distinct 16×16 environment props for bunker bulkhead panels, forest trees, ruin masonry rubble and roadside barriers. `PixelRenderer.drawZoneWall` selects the sprite by active `WorldZone`, with a small contact shadow. This replaces the previous generic colored wall blocks while keeping TileMap collision semantics unchanged. Atlas validation and unit tests enforce the generated 16×16 cell limit. These are procedural pixel-art placeholders; final authored art and device visual review remain open.


The four zones also use deterministic floor-decoration sprites: bunker floor grates, forest bushes, ruin debris and roadside lamps. Placement is derived from tile coordinates, so decorations remain stable across save/load without introducing additional mutable world state.


### World sprite depth ordering

The renderer now collects zone obstacle sprites, furniture sprites and the player into `WorldSpriteCommand` records and sorts them by ground-contact Y before drawing. A stable secondary order places the player after world objects at an equal depth. Zone obstacles receive a translucent contact shadow. Flat tile/floor details remain in the base pass, while tall sprites participate in the depth pass. This improves north/south occlusion without changing collision or save-state logic.


### Deterministic zone props and depth pass

The atlas includes additional bunker crates, industrial barrels and a roadside vehicle wreck. Floor-tile decorations are emitted as `WorldSpriteCommand` records and share the same ground-contact Y sort as walls, furniture and the player. Placement is derived from tile coordinates and the active zone, so it is stable across save/load and does not alter world snapshots. Low-profile floor markings remain in the base tile pass.


Decoration selection is isolated in the pure `ZonePropSelector` helper. It derives a zone-specific sprite ID from the zone and tile coordinates; unit tests cover determinism, zone-specific prop sets and the roadside wreck pattern. This keeps decoration placement reproducible without expanding checkpoint/save data.


### Atlas cell sizing from actual frame bounds (2026-11-20)

`PixelSpriteTextureAtlas` previously hardcoded a 16×16 cell while the authored character frames are 16×24. `build()` therefore threw at the first player sprite; `PixelRenderer.create` caught the failure, logged it and left `spriteManifest` empty, so the GPU texture path was silently inactive and every sprite was rasterized as per-pixel CPU quads.

The layout is now computed by `PixelSpriteTextureAtlas.plan()`: a platform-independent `SpriteAtlasPackPlan` derives the cell size from the largest authored frame, verifies that all frames of a sprite share dimensions, assigns every frame to a distinct cell and reports texture width/height. `build()` only rasterizes the resulting plan, so manifest creation and bitmap filling can no longer disagree about cell size.

`PixelSpriteTextureAtlasTest` asserts that the cell covers the tallest frame (≥24), that no frame crosses its cell, that no cell is reused, that the texture fits every packed frame, that the derived manifest passes `SpriteAtlasManifestValidator` and that UV conversion stays inside 0..1 for the character frames. These checks run on the JVM without Android bitmap APIs, so a future hardcoded cell size fails CI instead of degrading rendering at runtime.
