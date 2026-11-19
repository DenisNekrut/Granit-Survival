# Granit Survival

2D pixel-art survival game for Android, built on **Granit Engine**, a custom engine developed specifically for this project.

## Technical direction

- Platform: Android, landscape orientation
- Language: Kotlin/JVM
- Rendering: EGL14 + OpenGL ES 2.0
- Engine: custom implementation; Unity, Godot and MonoGame are not used
- Architecture: Android platform layer / Granit Engine / Granit Survival game layer

## Implemented prototype

### Engine and gameplay
- Android application shell, SurfaceView lifecycle and EGL render thread.
- Fixed-step simulation, camera, collision, virtual joystick and multitouch input.
- Four connected areas: bunker, fixed first forest area, ruins and road; when crossing a hatch, the player arrives on the safe walkable cell immediately inside the matching exit in the destination zone (not at the map centre). Save/load and checkpoint restore preserve the exact player coordinates.
- Ruins and road are regenerated from a new deterministic seed on every new entry; bunker and first forest map stay fixed. The current map seed and remaining resources are preserved by save/checkpoint restore.
- Gathering and travelling consume in-game time.
- 4×4 inventory, resource collection, bunker storage and item transfer.
- Health, hunger, thirst, energy, food and water.
- Sleep until 07:00, wake-up checkpoint and rollback of the reversible run state, including equipment, statistics and generated map state.
- Deterministic five-state weather cycle; outdoor conditions gently affect movement, and radio forecasts the next day.
- Dynamic outdoor lighting follows the in-game clock using smoothstep transitions: cold dark nights, gradual dawn, bright daytime, warm dusk and a smooth return to night. Unit tests cover every minute and adjacent-minute continuity.

### Bunker, knowledge and events
- Main menu, pause menu with a prominent return-to-menu action, expedition journal, character statistics, save-slot manager and settings. Contextual station panels appear as compact in-game cards over the live world instead of replacing the game screen.
- Three manual save slots and a separate atomic autosave; JSON schema v8 reads older versions 1–7.
- Workbench recipes for bandage and medical kit; medical kit recipe unlocks after reading a technical manual.
- Bunker library with fiction, science, technical manuals, diaries and magazines. Reading consumes game time and may affect morale/stress.
- Bunker radio with one daily broadcast: weather, maintenance notes, archival broadcast or weak signal. Listening consumes 10 in-game minutes.
- Six discoverable landmarks in forest, ruins and road. Full Russian descriptions are recorded in the expedition journal.
- Read books, discovered landmarks, radio state, equipment and psychological indicators are included in save/checkpoint rollback.
- Character statistics track survived days, expedition minutes, distance travelled, collected resources, crafted items, books read and visited zones; they are displayed in a dedicated screen and revert with the wake checkpoint.
- A fictional non-lethal signal emitter is automatically drawn outside the bunker, holstered on return, and shown beside the character/HUD. A wider equipment/combat system is not yet implemented.
- Psychological indicators: stress, loneliness, fatigue and morale. Sleep/return aid recovery; fatigue and stress gently reduce movement speed.

### Graphics and settings
- PixelSpriteAtlas + GeneratedPixelSprites with original project artwork: 16×24 character frames in four directions (four walk frames per direction), plus 16×16 hatches, bunker furniture, control console, generator, medical station, trees, bushes, rubble, streetlamp and crate.
- Distinct hatch artwork for bunker ladder entry and outdoor transitions; hatch sprites are used in the world renderer.
- Zone-specific floor and obstacle visuals for bunker, forest, ruins and road.
- Time-of-day lighting is applied to outdoor zones with smooth interpolation. In the bunker, ceiling fixtures are hidden; only warm floor/object light pools are visible, with darker unlit areas between them. Generator-linked power and emergency lighting are the next planned systems.
- Bunker is divided into connected bedroom, dining room, generator room, medical room, workshop and utility storage room; the bunker footprint is compact (34×20 tiles) while expedition zones remain 40×24. Each room has contextual access.
- HUD displays live survival meters, day/time, current zone and weather.
- Joystick sensitivity, scale and controls opacity are applied. Audio volume, vibration and graphics quality are stored but still need integration.

## Documentation
- [Granit Engine architecture v2.0](docs/architecture/GRANIT_ENGINE_ARCHITECTURE.md)
- [Project requirements and progress tracker](docs/PROJECT_BACKLOG.md)

The older Unity-oriented plan is retained for historical reference and is superseded by the custom-engine architecture above.

## Remaining work

This is still a playable prototype, not a finished game. The next work includes visual refinement of light falloff and shadows, a full art pass for remaining tiles, richer environment animations and visual QA on Android, richer procedural map variety and points of interest, a fully functional generator/medical/dining system, more recipes and bunker upgrades, richer weather/story events, NPCs and expedition objectives, a broader equipment system, and physical Android device testing.

## Downloading a test APK

Every successful GitHub Actions build uploads an artifact named **GranitSurvival-debug-apk**. Open the latest successful [Android Debug Build](https://github.com/DenisNekrut/Granit-Survival/actions), find the **Artifacts** section at the bottom of the run page, and download the ZIP. It contains `app-debug.apk`.

The APK is a debug build for testing, not a signed production release. A successful CI build does not mean the game has been tested on a physical phone.


## Адаптивное управление и звук

- Игровая камера подстраивает видимую область под фактическое соотношение сторон экрана, не растягивая пиксельную графику.
- Масштаб игрового мира настраивается в диапазоне 80–120% в меню настроек.
- Виртуальный джойстик закреплён у левого края экрана, кнопка действия — у правого; позиции рассчитываются относительно реального размера SurfaceView.
- Фоновая ambient-музыка и короткие звуки шагов/взаимодействия синтезируются локально. Громкости музыки и эффектов регулируются независимо; аудио приостанавливается вместе с игрой.


### Visual interface pass

- Compact bunker map: 34×20 tiles; outdoor expedition maps remain 40×24.
- Hidden ceiling fixtures with warm light reflected onto floors and nearby objects; unlit bunker areas remain darker.
- Contextual station windows are compact overlays over the active game, with a refreshed industrial field-console visual theme.
- Original sprite palette and zone floor colors have been brightened for clearer material separation.
