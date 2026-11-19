# Installing a Granit Survival test build

The project currently publishes **debug APK** artifacts through GitHub Actions. There is no signed production release pipeline yet.

## Minimum device requirements

- Android 8.0 (API 26) or newer
- OpenGL ES 2.0 support
- Landscape display; the game locks to landscape orientation

## Download

1. Open [GitHub Actions](https://github.com/DenisNekrut/Granit-Survival/actions).
2. Open the latest successful **Android Debug Build** run for the desired commit.
3. Find **Artifacts** near the bottom of the run page.
4. Download `GranitSurvival-debug-apk` and extract the ZIP.
5. Copy `app-debug.apk` to the Android device and open it.

Android may ask you to allow installation from the app used to open the APK. Only enable this for a trusted source and turn it off again if you no longer need it.

## Updating a test build

Debug builds are for testing. A debug APK from a different CI runner may have a different debug signing certificate, so Android can refuse to install it over an earlier APK. If Android reports a signature conflict, uninstall the old test build and install the new one.

**Warning:** uninstalling the app can delete local game saves. Copy or otherwise preserve important saves before uninstalling.

The project will return to signed, in-place APK updates after a stable release-signing setup is agreed and tested. No key creation or GitHub secret configuration is required for current debug testing.

## Device validation

After installation, follow [Android device smoke tests](ANDROID_DEVICE_SMOKE_TEST.md) to check startup, pause/resume, orientation changes, menu navigation and Surface/EGL lifecycle.
