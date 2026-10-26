# Interlude

A minimal Android TV launcher built with Kotlin and Compose for TV. Requires Android 14 or later.

The home screen shows a clock, English and Chinese lunar dates, system shortcuts, and a five-column app grid. Artwork keeps its original proportions; a soft background glow follows the selection. App names use the system font, with a brighter Medium weight on focus.

- Use the D-pad to navigate and OK to open an app.
- Hold OK to hide an app or restore a hidden app.
- Hold Settings to show or collapse hidden apps.
- Home selects the first displayed app. Back preserves focus, scroll position, and hidden-section expansion.
- Recently opened apps come first. Unused apps follow package priorities, Chinese pinyin order, then English alphabetical order; see [APP_DESIGN.md](APP_DESIGN.md).

Hidden apps remain installed. Records stay on the device; Interlude has no accounts, advertising, Internet permission, usage-access permission, or accessibility service.

## Build and validate

Use JDK 27 and Android SDK / Build Tools 37. The Gradle wrapper and build files pin the stable toolchain and dependencies; Android bytecode targets Java 17.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
ANDROID_SERIAL=<android-tv-device> ./gradlew :app:connectedDebugAndroidTest
```

Device tests require an Android TV environment with launchable apps and may replace the installed build. The Release APK is `app/build/outputs/apk/release/app-release.apk`, with R8 and resource shrinking enabled.

```sh
adb -s <device> install -r app/build/outputs/apk/release/app-release.apk
adb -s <device> shell cmd package set-home-activity art.anjing.interlude/.MainActivity
adb -s <device> shell input keyevent KEYCODE_HOME
```

Verify Home on the target TV. Keep the stock launcher enabled. Reuse the same signing key for updates; uninstalling or clearing app data removes Interlude's hidden-app and launch records.

## Release

Version 1.0.0 uses versionCode 4 and the existing personal debug keystore for sideloading. The keystore is never committed. Repository secret `ANDROID_KEYSTORE_BASE64` must contain Base64 of that same `~/.android/debug.keystore` (alias `androiddebugkey`, password `android`).

Push the reviewed commit and tag `v1.0.0`, then publish its GitHub Release. [The workflow](.github/workflows/release.yml) runs only when that Release is published, checks unit tests and Lint, builds and verifies the signed APK, then uploads `Interlude-1.0.0.apk`. Failed runs can be rerun. The workflow does not commit, tag, bump versions, or create a Release.

## Project

One `app` module contains Activity/ViewModel coordination, `data` for the app catalog and local records, `system` for Android entry points, and `ui` for Compose rendering. There are no services, dependency injection, or repository layer.

- [Product behavior](APP_DESIGN.md)
- [Validation history and limitations](VALIDATION.md)
- [Technical constraints](AGENTS.md)
- [Brand assets](branding/README.md)

## License and credits

Interlude is [MIT licensed](LICENSE). The lightweight launcher design references [CleanYourTV](https://github.com/Gathin23/cleanyourtv/tree/main/launcher); its Inputs, Wi-Fi, Ethernet, and Settings icons retain Copyright (c) 2026 Gathin under the MIT License.

The unmodified Inter 4.1 Light and Regular fonts come from [the official release](https://github.com/rsms/inter/releases/tag/v4.1). Their [SIL Open Font License](app/src/main/assets/INTER-LICENSE.txt) is included in the app. The TV banner wordmark uses outlines from Inter Regular.
