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
- Four connected areas: bunker, forest, ruins and road; exits are reachable from each zone's spawn.
- Separate maps and resource states for each area; collected resources remain collected when revisiting.
- Gathering and travelling consume in-game time.
- 4×4 inventory, resource collection, bunker storage and item transfer.
- Health, hunger, thirst, energy, food and water.
- Sleep until 07:00, wake-up checkpoint and rollback of the reversible run state.

### Bunker, knowledge and events
- Main menu, pause menu, save-slot manager, settings, storage and workbench screens.
- Three manual save slots and a separate atomic autosave; JSON schema v5 reads older versions 1–4.
- Workbench recipes for bandage and medical kit; medical kit recipe unlocks after reading a technical manual.
- Bunker library with fiction, science, technical manuals, diaries and magazines. Reading consumes game time and may affect morale/stress.
- Bunker radio with one daily broadcast: weather, maintenance notes, archival broadcast or weak signal. Listening consumes 10 in-game minutes.
- Read books, unlocked knowledge and radio state are included in save/checkpoint rollback.
- Psychological indicators: stress, loneliness, fatigue and morale. Sleep/return aid recovery; fatigue and stress gently reduce movement speed.

### Graphics and settings
- PixelSpriteAtlas with player idle/walk frames, resource and medical-item sprites.
- Zone-specific floor and obstacle visuals for bunker, forest, ruins and road.
- Pixel-art bed, storage, workbench, bookshelf and radio.
- HUD displays live survival meters, day/time and current zone.
- Joystick sensitivity, scale and controls opacity are applied. Audio volume, vibration and graphics quality are stored but still need integration.

## Documentation
- [Granit Engine architecture v2.0](docs/architecture/GRANIT_ENGINE_ARCHITECTURE.md)
- [Project requirements and progress tracker](docs/PROJECT_BACKLOG.md)

The older Unity-oriented plan is retained for historical reference and is superseded by the custom-engine architecture above.

## Remaining work

This is still a playable prototype, not a finished game. The next work includes a complete art pass and external sprite sheets, more recipes and bunker upgrades, richer weather/story events, NPCs and expedition objectives, full audio/settings integration, and physical Android device testing.

## Downloading a test APK

Every successful GitHub Actions build uploads an artifact named **GranitSurvival-debug-apk**. Open the latest successful [Android Debug Build](https://github.com/DenisNekrut/Granit-Survival/actions), find the **Artifacts** section at the bottom of the run page, and download the ZIP. It contains `app-debug.apk`.

The APK is a debug build for testing, not a signed production release. A successful CI build does not mean the game has been tested on a physical phone.
