# Android device smoke test — startup and lifecycle

Run this checklist on a physical Android phone before merging the black-screen/startup work. CI unit tests cannot prove that a device's native Surface and EGL driver present frames correctly.

## Capture diagnostics

Connect the phone with USB debugging enabled and run:

```bash
adb logcat -c
adb logcat -v time -s GranitSurface:I GranitEGL:I GranitLifecycle:I GranitEngine:E AndroidRuntime:E
```

The startup log should include SurfaceView dimensions, EGL vendor/version, chosen RGBA/depth/stencil sizes, GLES renderer/version, and the viewport. There should be no `Surface/viewport size mismatch` warning and no GL error after `glViewport`.

## Scenarios

1. **Fresh launch:** open the app, start a new game, confirm the first frame is visible and controls respond.
2. **Background/resume:** move for several seconds, switch to another app, wait 10 seconds, return. Confirm there is one render thread, no frozen frame, and no duplicated movement/input.
3. **Screen lock:** lock and unlock the phone while in-game. Confirm the render thread stops while paused and restarts on resume.
4. **Landscape reversal:** rotate between the two landscape orientations if device auto-rotate permits it. Confirm the viewport dimensions update and touch controls remain aligned.
5. **Menu transition:** open pause/menu, return to the main menu, start another session. Confirm no old render thread remains and the new session renders.
6. **Repeated lifecycle:** repeat background/resume and menu transitions five times. Check for EGL errors, crashes, black frames, and input that remains latched.
7. **Surface recreation:** use Android's split-screen or display-size change if available; confirm a new `surfaceChanged` updates the GLES viewport.

## Pass criteria

- No black screen on fresh game start.
- Every `surfaceChanged` size is propagated to `glViewport`.
- No EGL/GLES initialization or swap errors.
- At most one live `Granit-RenderThread`.
- Input is cleared on pause/surface destruction.
- Game simulation is paused while the Activity is backgrounded and resumes normally.

Record device model, Android version, pass/fail for each scenario, and attach the filtered Logcat output to PR #3 before merging it.
