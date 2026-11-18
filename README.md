# Granit Survival

2D pixel-art survival game for Android, built on **Granit Engine**, a custom engine developed specifically for this project.

## Current technical direction

- Platform: Android (landscape)
- Language: Kotlin/JVM
- Rendering backend: EGL14 + OpenGL ES 2.0
- Engine: custom implementation; no Unity, Godot, or MonoGame
- Architecture: Android Platform / Granit Engine / Granit Survival game layer

## Implemented prototype

- Android application shell, SurfaceView lifecycle and EGL14 render thread.
- Fixed-step simulation loop, camera, collision and multitouch virtual joystick.
- Four connected areas: bunker, forest, ruins and road.
- Separate resource layouts for each area; collected resources persist when travelling between zones.
- 4×4 inventory, resource collection, bunker storage, item transfer, canned food and water consumption.
- Survival clock, health, hunger, thirst, energy and sleep until 07:00.
- Wake-up checkpoint with rollback of player, inventory, bunker storage, needs, time and resources in every zone.
- Three manual save slots plus a separate atomic autosave file, versioned JSON format and backward reading of version 1 saves.
- Main menu, pause screen, save-slot manager, storage screen and settings screen.
- Settings persist through SharedPreferences. Joystick sensitivity and scale are applied; audio, vibration and graphics quality still need integration.
- PixelSpriteAtlas with a shared palette, player idle/walk frames, resource sprites and atlas validation.
- HUD with live survival meters, day/time and current zone label.

## Documentation

- [Granit Engine architecture v2.0](docs/architecture/GRANIT_ENGINE_ARCHITECTURE.md)
- [Project requirements and progress tracker](docs/PROJECT_BACKLOG.md)

The legacy Unity-based plan is retained for history only and is superseded by the custom-engine architecture above.

## Development status

The prototype now has the first connected gameplay loop across four areas, but it is not yet a complete game. The maps and sprite art are early placeholders. There are no finished NPC/event systems, story content, book library, crafting progression or full audio layer. Physical Android device testing remains outstanding.

## Downloading a test APK

Every successful GitHub Actions build uploads an artifact named **GranitSurvival-debug-apk**. Open the latest successful [Android Debug Build](https://github.com/DenisNekrut/Granit-Survival/actions), find the **Artifacts** section at the bottom of the run page, and download the ZIP. It contains `app-debug.apk`.

The APK is a debug build for testing, not a signed production release. A successful CI build does not mean the game has been tested on a physical phone.
