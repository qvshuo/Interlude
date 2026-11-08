# Project contract

## Scope

- Follow the product, interaction, sorting, and visual specifications in [APP_DESIGN.md](APP_DESIGN.md). User installation and operation belong in [README.md](README.md); dated verification and release evidence belong in [VALIDATION.md](VALIDATION.md). Maintain each rule only in its owning document.
- Keep `versionName = "1.0.5"` and `versionCode = 9` until the user requests a version upgrade.
- Preserve original app labels and lunar-calendar content. Keep project documentation, identifiers, and explanatory comments in English.

## Code and data

- Keep one `app` module. `data` owns app discovery, ordering, artwork, and local records; `system` wraps system entry points; `ui` renders the interface. Activity and ViewModel coordinate lifecycle and home state.
- Keep the implementation to the smallest sufficient design. Use no additional architecture layers without a demonstrated product need.
- Store hidden package names and successful launch timestamps in `apps` SharedPreferences under `hidden` and `used:<package>`. Migrate legacy component-name records and preserve user records across updates. Keep expanded all-apps state in memory.
- Read artwork on IO threads at no more than 384×216. Cache the directory independently of artwork success; reuse successful artwork by component and package update time, retry failures on later refreshes, and invalidate resources on configuration changes.
- Serialize catalog loads with the existing mutex. Ordinary refreshes reuse the directory; forced invalidation survives cancellation. Commit only active loads and retain package changes that arrive during a scan.
- Prefer TV entry activities, then ordinary Launcher activities; within a category select the lexicographically smallest activity name per package.
- ViewModel owns reported focus, last selected app, and explicit focus targets. UI executes requests and reports focus; explicit targets take priority over incidental focus events.
- Glass textures belong to each control's draw cache. Read animated focus only in the draw callback; avoid global texture caches without measured hardware evidence.
- Use app launch recency first, then the documented package priority and name ordering in `APP_DESIGN.md`. Remove records for uninstalled apps.

## Build and release

- Use stable toolchain and dependency releases. The current toolchain is Gradle 9.8.2, JDK 27, and Android SDK / Build Tools 37. Keep Java and JVM bytecode at 17.
- Build Release with `./gradlew :app:assembleRelease`; output is `app/build/outputs/apk/release/app-release.apk`. Keep R8 and resource shrinking enabled, targeting `armeabi-v7a` and `arm64-v8a`.
- Validate with `./gradlew :app:testDebugUnitTest :app:lintDebug`. Device tests use `ANDROID_SERIAL=<device> ./gradlew :app:connectedDebugAndroidTest` on an Android TV environment with launchable apps.
- Sign personal Release builds with the existing local debug keystore so updates retain the installed signing identity. Keep keystore material out of the repository.
- The `v1.0.5` GitHub Release publication triggers `.github/workflows/release.yml`, which runs unit tests, Lint, and Release build before uploading `Interlude-1.0.5.apk`. Release notes describe user-facing changes and platform requirements; validation details belong in `VALIDATION.md`.

## Documentation and licenses

- Preserve the MIT notices for Interlude, CleanYourTV-derived icons, and Aerial-derived continuous corners. Keep `LICENSE` and `app/src/main/assets/LICENSE.txt` identical.
- Bundle only the unmodified Inter 4.1 Light and Regular TTF files and preserve the SIL Open Font License in `app/src/main/assets/INTER-LICENSE.txt`.
