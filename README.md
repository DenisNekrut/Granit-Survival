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

The repository is being migrated from the original planning-only Unity proposal to the custom Android engine. The first milestone is the Android bootstrap: application shell, surface lifecycle, EGL context, render thread, and touch input.

No milestone is considered complete until its build and acceptance checks have actually passed.
