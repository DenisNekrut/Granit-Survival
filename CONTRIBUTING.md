# Contributing to Granit Survival

Granit Survival is an Android-first 2D survival game built on the project's own **Granit Engine**.

## Development requirements

- JDK 17
- Android SDK Platform 36
- Android Build Tools compatible with the installed Android Gradle Plugin
- Gradle 8.13
- Kotlin/JVM

Unity, Godot and MonoGame are not part of the current architecture.

## Build and test

From the repository root:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is created at:

```
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs the same unit-test and debug-APK build for pull requests targeting `main`.

## Change workflow

1. Create a focused branch from the current development base.
2. Keep changes scoped to one subsystem where practical.
3. Add or update unit tests for gameplay, persistence and rendering-independent logic.
4. Run the build and tests before opening a pull request.
5. Describe behavior changes and include reproduction/verification steps.
6. Do not merge a change that needs physical-device validation until that validation is complete.

## Architecture rules

- Keep gameplay simulation independent of Android UI classes.
- Keep the render loop and world simulation deterministic where possible.
- Route persistence through `SaveCodec`, `SaveMigration` and `SaveValidator`.
- Preserve save compatibility; increment `SaveVersion.CURRENT` only with a migration and tests.
- Never add Unity/Godot/MonoGame runtime dependencies.
- Do not commit generated APKs, local SDK paths, signing keys, passwords or other credentials.

## Releases

Only the debug APK workflow is currently active. Signed production releases and in-place update testing are intentionally deferred. No signing secrets or local keystore setup are required for normal development.
