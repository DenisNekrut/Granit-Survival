# Granit Survival

[![Android Debug Build](https://github.com/DenisNekrut/Granit-Survival/actions/workflows/android-build.yml/badge.svg?branch=main)](https://github.com/DenisNekrut/Granit-Survival/actions/workflows/android-build.yml)

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
- Main menu, pause menu with a prominent return-to-menu action, expedition journal, character statistics, save-slot manager and settings. Contextual station panels appear as compact in-game cards over the still-visible world instead of replacing the game screen; simulation and ambient audio continue while a card is open.
- Three manual save slots and a separate atomic autosave; JSON schema v9 reads older versions 1–8.
- Workbench recipes for bandage and medical kit; medical kit recipe unlocks after reading a technical manual.
- Bunker library with fiction, science, technical manuals, diaries and magazines. Reading consumes game time and may affect morale/stress.
- Bunker radio with one daily broadcast: weather, maintenance notes, archival broadcast or weak signal. Listening consumes 10 in-game minutes.
- Six discoverable landmarks in forest, ruins and road. Full Russian descriptions are recorded in the expedition journal.
- Read books, discovered landmarks, radio state, equipment and psychological indicators are included in save/checkpoint rollback.
- Character statistics track survived days, expedition minutes, distance travelled, collected resources, crafted items, books read and visited zones; they are displayed in a dedicated screen and revert with the wake checkpoint.
- A fictional pulse carbine is automatically drawn outside the bunker, holstered on return, and shown beside the character/HUD. Its effect and hit rules are abstract game mechanics.
- Psychological indicators: stress, loneliness, fatigue and morale. Sleep/return aid recovery; fatigue and stress gently reduce movement speed.

### Graphics and settings
- PixelSpriteAtlas + GeneratedPixelSprites with original project artwork: 16×24 character frames in four directions (four walk frames per direction), plus 16×16 hatches, bunker furniture, control console, generator, medical station, trees, bushes, rubble, streetlamp and crate.
- Distinct hatch artwork for bunker ladder entry and outdoor transitions; hatch sprites are used in the world renderer.
- Zone-specific floor and obstacle visuals for bunker, forest, ruins and road.
- Time-of-day lighting is applied to outdoor zones with smooth interpolation. In the bunker, ceiling fixtures are hidden; only warm floor/object light pools are visible, with darker unlit areas between them. Generator-linked power, abstract fuel consumption and dim emergency lighting are implemented; the generator panel supports switching and refueling with wood.
- Bunker is divided into connected bedroom, dining room, generator room, medical room, workshop and utility storage room; the bunker footprint is compact (34×20 tiles) while expedition zones remain 40×24. Each room has contextual access.
- HUD displays live survival meters, day/time, current zone and weather.
- Joystick sensitivity, scale and controls opacity are applied. Audio volume, vibration and graphics quality are stored; further device-level tuning is still needed.

## Documentation
- [Granit Engine architecture v2.0](docs/architecture/GRANIT_ENGINE_ARCHITECTURE.md)
- [Project requirements and progress tracker](docs/PROJECT_BACKLOG.md)
- [Android debug APK installation](docs/ANDROID_INSTALL.md)
- [Android device smoke tests](docs/ANDROID_DEVICE_SMOKE_TEST.md)
- [Contribution guide](CONTRIBUTING.md)
- [Security policy](SECURITY.md)

The older Unity-oriented plan is retained for historical reference and is superseded by the custom-engine architecture above.

## Remaining work

This is still a playable prototype, not a finished game. The next work includes visual refinement of light falloff and shadows, a full art pass for remaining tiles, richer environment animations and visual QA on Android, richer procedural map variety and points of interest, a expanded generator-powered stations, medical and dining systems, more recipes and bunker upgrades, richer weather/story events, NPCs and expedition objectives, a broader equipment system, and physical Android device testing.

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


### Continuous world and outdoor threats

- Interaction cards remain over the visible world while the simulation and ambient audio continue. Reading, crafting, consuming supplies, medical actions, radio, resource gathering, travel and storage operations consume in-game time.
- Outdoor maps (forest, ruins and road) now populate with original stylized infected NPCs. They approach the player, navigate around blocked tiles and create an abstract contact threat. The bunker remains safe.
- The starter outdoor equipment is a fictional pulse carbine with a dedicated touch control and a cyan pulse effect. It uses abstract game-only hit rules and keeps the legacy SIGNAL_EMITTER save value readable.


### Infected variants
Outdoor expeditions now feature three original infected archetypes: shamblers, runners, and brutes. They have distinct silhouettes, movement speeds, durability, and abstract contact pressure. Bunker interiors remain safe from infected spawns. The pulse-carbine remains a fictional energy effect with simplified hit detection.


### Medical station
- Powered treatment at the bunker medical station consumes one medical kit and 12 in-game minutes, restoring 50 health.
- Manual bandage and medical-kit use remains available during a generator outage.
- Timed medical actions advance world time and survival needs before consuming supplies or applying healing, so a checkpoint rollback does not silently spend an item.
- Medical treatment is covered by unit tests for power requirements, inventory, time/fuel cost and rollback behavior.


## Backpack and bunker layout

- The character inventory is a 4×4 backpack. Its contents are hidden during normal play and open from the on-screen backpack icon.
- Food, water, bandages and medical kits can be used directly from the backpack; resources and crafting materials remain stored there for other interactions.
- The bunker map is 17×10 tiles, with six compact rooms joined by one-tile corridors. Each room has its own floor palette and material pattern; corridors use dark steel flooring.
- After 15 seconds without movement, interaction or firing, the character begins a room-to-room walk inside the bunker. Any direct player input cancels the autonomous walk and resets the idle timer.

## Auto-aim and interaction panels

- In outdoor zones, the nearest infected actor within seven tiles is selected only when there is a clear line of sight. A small reticle is drawn over the selected actor and disappears when no valid target is visible.
- The abstract pulse action prioritizes the currently selected visible target.
- The context action routes to the nearest matching bunker station panel. The workshop remains inspectable without power, while crafting controls are disabled until power is restored.
- During gameplay, tapping the dimmed area outside an open interaction card closes it. Taps inside the card remain available to its controls.


## Environmental art pass

- Zone dressing is deterministic and sparse, so a saved coordinate keeps the same visual details on every visit.
- The forest adds grass patches and fallen logs alongside trees and bushes.
- Ruins add scattered metal scrap and small puddles alongside barrels and debris.
- Roads add signs and puddles alongside streetlights and wreckage silhouettes.
- The bunker uses occasional floor grates and supply crates; decorations are drawn only on floor tiles and do not alter collision or navigation.
- Atlas regression tests verify every selected prop has a renderable sprite entry.


## Android updates from GitHub

The in-game **ОБНОВИТЬ** entry opens the repository's latest GitHub Releases page. It is currently a navigation link only: signed production releases and automatic in-place APK updates are intentionally deferred.

For now, GitHub Actions builds a **Debug APK** for pull requests. Download and install it using the [Android installation guide](docs/ANDROID_INSTALL.md). A debug APK from a different CI runner may not be accepted as an in-place update because its debug signing certificate can differ; preserve local saves before uninstalling an older test build.

No keystore, passwords or GitHub Actions secrets are required for current development. The release-signing setup will be revisited later, when the project is ready for a stable production update path.
