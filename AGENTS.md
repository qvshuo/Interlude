# Project contract

## Product

- Interlude is a single-screen Android TV launcher for Android 14 and later. Keep `versionName = "1.0.4"` and `versionCode = 8` until the user requests a version upgrade.
- Show the clock, Gregorian date, Chinese lunar date, Inputs, Network, Settings, and a five-column app grid. Keep the app area blank during initial loading and when Home has no visible apps.
- Keep app labels below their tiles on one line with ellipsis. Fade only content scrolled above the first row's original top edge.
- Use bundled Inter Light and Regular for the clock and English date. Use the system font for app names, fallback initials, and Chinese lunar date. Respect native `sp` scaling and system font settings.
- Preserve original app labels and lunar-calendar content. Keep project documentation, identifiers, and explanatory comments in English.
- Provide short spoken descriptions for icon buttons and app tiles.

## Interaction and system behavior

- Open apps on short OK. Hold OK on a Home app to remove it; hold OK on a removed app to add it back. Keep removed apps installed.
- Use native Android Toasts with these messages: `Removed from Home: <app>`, `Added to Home: <app>`, and `All apps are already on Home`. Let Android manage their display duration.
- Hold Settings to show all apps or return to Home apps. Keep range changes silent; focus the first removed app when expanding, and the first Home app when collapsing, falling back to Settings when Home is empty.
- Home refreshes ordering and resets app focus without collapsing an expanded all-apps view. Back preserves focus, scroll position, and expansion.
- Fail silently when an app or system entry cannot open. Inputs opens `com.android.tv.action.VIEW_INPUTS`; Network opens network settings with general Settings as fallback.
- Confirm default-launcher behavior with the physical TV's Home key.

## Code and data

- Keep one `app` module. `data` owns app discovery, ordering, artwork, and local records; `system` wraps system entry points; `ui` renders the interface. Activity and ViewModel coordinate lifecycle and home state.
- Keep the implementation to the smallest sufficient design. Use no additional architecture layers without a demonstrated product need.
- Store hidden package names and successful launch timestamps in `apps` SharedPreferences under `hidden` and `used:<package>`. Migrate legacy component-name records and preserve user records across updates. Keep expanded all-apps state in memory.
- Read artwork on IO threads at no more than 384×216. Reuse by component and package update time, and invalidate on configuration changes.
- Use app launch recency first, then the documented package priority and name ordering in `APP_DESIGN.md`. Remove records for uninstalled apps.

## Build and release

- Use stable toolchain and dependency releases. The current toolchain is Gradle 9.8.2, JDK 27, and Android SDK / Build Tools 37. Keep Java and JVM bytecode at 17.
- Build Release with `./gradlew :app:assembleRelease`; output is `app/build/outputs/apk/release/app-release.apk`. Keep R8 and resource shrinking enabled, targeting `armeabi-v7a` and `arm64-v8a`.
- Validate with `./gradlew :app:testDebugUnitTest :app:lintDebug`. Device tests use `ANDROID_SERIAL=<device> ./gradlew :app:connectedDebugAndroidTest` on an Android TV environment with launchable apps.
- Sign personal Release builds with the existing local debug keystore so updates retain the installed signing identity. Keep keystore material out of the repository.
- The `v1.0.4` GitHub Release publication triggers `.github/workflows/release.yml`, which runs unit tests, Lint, and Release build before uploading `Interlude-1.0.4.apk`. Release notes describe user-facing changes and platform requirements; validation details belong in `VALIDATION.md`.

## Documentation and licenses

- `APP_DESIGN.md` describes product behavior and visual design. `VALIDATION.md` records dated checks and release evidence.
- Preserve the MIT notices for Interlude, CleanYourTV-derived icons, and Aerial-derived continuous corners. Keep `LICENSE` and `app/src/main/assets/LICENSE.txt` identical.
- Bundle only the unmodified Inter 4.1 Light and Regular TTF files and preserve the SIL Open Font License in `app/src/main/assets/INTER-LICENSE.txt`.
