# Project conventions

- Keep the product at `1.0.2` until the user explicitly requests a version upgrade or commit. Preserve `versionName = "1.0.2"` and `versionCode = 6`.
- Leave the app area empty during initial loading and when all apps are hidden. Do not show loading text, empty-list messages, or placeholder headings.
- Fail silently when an app or system entry cannot open.
- Use native Android Toasts for removing apps from Home, adding them back, and reporting that all apps are already on Home. Expanding or collapsing all apps is silent and moves focus to the first removed or Home app respectively, falling back to Settings when Home is empty. Use the same messages for the last app as for any other app.
- Let Android dismiss Toasts automatically. Back preserves hidden-section expansion, focus, and scroll position; it must not cancel, replace, or retrigger Toasts.
- Follow Occam's razor: use the smallest sufficient implementation. Do not add abstractions, state, or adaptation layers for hypothetical requirements or test conditions.
- Use native Android sp sizes; do not override LocalDensity or font scaling. Use default system font settings for screenshot comparisons and label separate stress tests explicitly.
- Use bundled Inter Light and Regular only for the clock and English date. App names, fallback initials, and the Chinese lunar date use the system font. Do not bundle additional font weights or scripts without a demonstrated need.
- Use English for project documentation, identifiers, and explanatory comments. Preserve original app labels and Chinese lunar-calendar content.
- Keep five apps per grid row. The top fade applies only to content scrolling above the first row's original top edge, never over tiles at rest.
- Inputs calls only `com.android.tv.action.VIEW_INPUTS`; do not fall back to TV input settings.
- Keep short, necessary spoken descriptions for icon buttons and app tiles; tests may reuse them. Do not add extra accessibility layers or spoken-only network-status logic.

## Structure and build

- Target Android 14+ with one `app` module. `data` handles the app catalog and local records, `system` wraps system entry points, and `ui` renders the interface. Activity and ViewModel coordinate lifecycle and home state. Do not introduce services, dependency injection, or a repository layer.
- Use the latest stable toolchain and dependencies, never Alpha, Beta, RC, or snapshots. The current toolchain is Gradle 9.8.2, JDK 27, Android SDK / Build Tools 37. Gradle configuration is authoritative for dependency versions. Keep Java / JVM bytecode at 17 for Android 14 compatibility.
- Build with `./gradlew :app:assembleRelease`. Output: `app/build/outputs/apk/release/app-release.apk`. Keep R8 and resource shrinking enabled; package only `armeabi-v7a` and `arm64-v8a`.
- Validate with `./gradlew :app:testDebugUnitTest :app:lintDebug`. Run device tests with `ANDROID_SERIAL=<device> ./gradlew :app:connectedDebugAndroidTest`; these require an Android TV environment with launchable apps.
- Read app artwork on IO threads at no more than 384x216. Reuse it by component and package update time; invalidate on configuration changes. Do not retain full-resolution images or poll apps/network state.
- Store hidden and launch records by package name in `apps` SharedPreferences, using `hidden` and `used:<package>`. Preserve migration from legacy component-name records and never reset data during updates. Keep hidden-section expansion in memory only.
- Home resets app focus; Back preserves state. Do not request INTERNET, usage-access, or accessibility permissions. Do not disable the stock launcher. Confirm the default launcher with the actual Home key on the target TV.
- Comments must be short English explanations of necessary reasons, platform limitations, or boundaries, not restatements of code.
- Product behavior is documented in `APP_DESIGN.md`; historical checks are in `VALIDATION.md`. This file is authoritative for technical constraints.

## Release

- Sign Release with the existing local debug keystore for personal sideloading. Future updates must use the same key. Never commit the key.
- Configure GitHub Secret `ANDROID_KEYSTORE_BASE64` with Base64 of the existing `~/.android/debug.keystore` (alias `androiddebugkey`, password `android`). Reuse the key that signed installed APKs.
- Create tag `v1.0.2` on a commit containing the workflow, then publish its GitHub Release to trigger `.github/workflows/release.yml`. After unit tests, Lint, and Release build pass, it uploads `Interlude-1.0.2.apk` to that Release.
- Release notes contain only user-facing changes and platform requirements; keep validation, hashes, signing, and acceptance details in VALIDATION.md.
- The workflow responds only to publication of the `v1.0.2` Release. It does not commit, tag, bump versions, or publish a Release itself. Failed builds can be rerun.

## Sources and licensing

- The lightweight launcher design references [CleanYourTV launcher](https://github.com/Gathin23/cleanyourtv/tree/main/launcher), reimplemented in Kotlin and Compose for TV.
- Inputs, Wi-Fi, Ethernet, and Settings icons originate in that project's `launcher/app/src/main/res/drawable`, under MIT License, Copyright (c) 2026 Gathin. Preserve the original notice. Interlude uses MIT License; see `LICENSE`.
- Continuous-corner construction is adapted from [Aerial](https://github.com/pengchujin/aerial-tv) under MIT License, Copyright (c) 2026 pengchujin. Keep the combined notice in `LICENSE` and `app/src/main/assets/LICENSE.txt` identical.
- Inter 4.1 comes from [the official Inter release](https://github.com/rsms/inter/releases/tag/v4.1). Bundle only the unmodified Light and Regular TTF files and retain the SIL Open Font License in `app/src/main/assets/INTER-LICENSE.txt`.
