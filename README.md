# Granit Survival

2D pixel-art survival game for Android, built on **Granit Engine**, a custom engine developed specifically for this project.

## Current technical direction

- Platform: Android (landscape)
- Language: Kotlin/JVM
- Rendering backend: EGL14 + OpenGL ES 2.0
- Engine: custom implementation; no Unity, Godot, or MonoGame
- Architecture: Android Platform / Granit Engine / Granit Survival game layer

## Documentation

- [Granit Engine architecture v2.0](docs/architecture/GRANIT_ENGINE_ARCHITECTURE.md)

The legacy Unity-based plan is retained for history only and is superseded by the architecture document above.

## Development status

The current prototype contains:
- Android application shell, SurfaceView lifecycle and EGL14 render thread;
- fixed-step simulation loop and OpenGL ES 2.0 renderer;
- 40×24 tile map with bunker furniture, resource nodes and collision;
- Camera2D follow behavior;
- touch joystick and independent action pointer;
- 4×4 inventory with stack merging, capacity checks and item removal;
- resource pickup for wood, metal, fabric, canned food and water;
- a separate bunker storage container with atomic deposit/withdraw operations;
- an in-game clock, hunger/thirst/energy/health simulation and sleep-to-morning cycle;
- in-memory wake-up checkpoints that restore player, inventory, bunker storage, needs, clock and collected world resources;
- versioned JSON save snapshots for player state, inventory, bunker storage, survival needs, world clock, resource nodes and wake-up checkpoint, written through Android AtomicFile;
- HUD bars connected to live health, hunger, thirst, energy and day/time values;
- first detailed pixel-art player sprite and additional wall/furniture tile details.

This is an early technical prototype, not yet a complete game. The JSON repository and snapshot serialization are implemented but still need integration with the menu/session lifecycle and device verification. The HUD is functional but still uses simple pixel bars; the art pass has started and the remaining world objects still need replacement sprites and animation.

## Downloading a test APK

Every successful GitHub Actions build uploads an artifact named **GranitSurvival-debug-apk**. Open the latest successful [Android Debug Build](https://github.com/DenisNekrut/Granit-Survival/actions), find the **Artifacts** section at the bottom of the run page, and download the ZIP. It contains `app-debug.apk`.

The APK is a debug build for testing, not a signed production release.
