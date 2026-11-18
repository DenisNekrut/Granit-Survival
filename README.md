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

The first engine prototype now contains:
- Android application shell and SurfaceView lifecycle;
- EGL14 context and a dedicated OpenGL ES 2.0 render thread;
- fixed-step simulation loop;
- a first block-based bunker scene;
- a controllable player state and touch joystick input.

This is an early technical prototype, not yet a complete game. The next work is to validate the build, improve input/rendering, and add world collision and tile-map support. Device testing is still required.
