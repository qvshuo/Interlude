# Validation record

Historical entries describe the implementation and environment at the time. They are not current acceptance results. In particular, older six-column layouts, management dialogs, Back-to-collapse behavior, font-scale overrides, input-settings fallbacks, and experimental version numbers have been superseded. The current product remains 1.0.0, versionCode 4.

## 2026-10-26: focused typography and UI simplification, 1.0.0

Rechecked all production font declarations, line heights, baseline padding, title animation, and layout alignment. Kept the two bundled Inter weights limited to clock/date, native sp scaling, system fonts elsewhere, and standard baseline padding for mixed-script tile names. No per-language sizing, density override, custom text measurement, or additional typography layer was introduced. Existing line-height and baseline settings serve the documented layout and were retained.

The title Crossfade now targets only its displayed label instead of the entire AppEntry, avoiding irrelevant transitions when artwork or component metadata changes. Removed the separator's redundant align modifier because its parent Row already centers children vertically. Sizes, spacing, font choices, navigation, and product version are unchanged.

The combined offline unit/Lint/Release/device command passed in 2m 47s: 3 JVM tests and 20 API 34 TV tests, no failures or skips, zero Lint errors or warnings. Release APK SHA-256: `e3358883b04a6757da3f5d16c5df22995efbb4aff9a2283233e8fd29d638adb5`.

## 2026-10-26: final pre-commit review, 1.0.0

Reviewed production code, tests, resources, module boundaries, comments, persistence, lifecycle, artwork caching, UI focus/layout, and the release workflow. No additional production abstraction or confirmed production defect justified a code change. Kept versionName 1.0.0 and versionCode 4; no commit, tag, or Release was created.

The workflow retains the current stable action majors: checkout v7, setup-java v6, and setup-gradle v6, checked against [setup-java](https://github.com/actions/setup-java) and [Gradle's Actions documentation](https://docs.gradle.org/current/userguide/github-actions.html). Added a 30-minute timeout, disabled checkout credential persistence, restricted restored key-file permissions, and added unconditional signing-key cleanup. YAML parsing passed. The repository initially had no Actions secrets; configured ANDROID_KEYSTORE_BASE64 from the existing local debug.keystore, as required by AGENTS.md, and confirmed the secret name exists. The workflow still runs only when the v1.0.0 Release is published and uploads only after tests, Lint, and the Release build succeed. A hosted release run was not triggered.

Renamed the catalog-recreation test to match the actual architecture and extended artwork bounds verification to both width and height. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --offline` passed (unchanged unit/build tasks reused their verified outputs); 3 JVM tests, zero Lint errors or warnings. `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --offline --warning-mode all` passed in 2m 29s: 20 tests, no failures or skips. Gradle's deprecation report points to com.android.internal.application calling Configuration.setVisible; protobuf Unsafe warnings originate in device-test tooling.

Reinstalled Release on the API 34 TV emulator at 1920x1080, font_scale 1.0. An actual injected Home key returned to Interlude without changing the default launcher or disabling any launcher. Stable app/header focus screenshots showed no overlap, clipped focus outline, mixed fonts, or dimmed resting tiles. Five-column wrapping, third-row navigation, hidden-section boundaries, offscreen restoration, and Back preservation passed device tests. Screenshots are in `artifacts/final-polish/`; the initial Home capture and first header capture included focus-animation timing, so final visual inspection used `release-app-focus.png` and the recaptured `release-header.png` after UI idle.

Crash log was empty. A single post-navigation memory sample reported 38,032 KiB total PSS; this is not a leak or sustained-performance test. Artwork remains bounded and reused, scans run on IO, and no polling or permanent service was added. Physical-TV behavior, long-run frame/memory stability, and damaged vendor resources were not tested this round.

Release APK: `app/build/outputs/apk/release/app-release.apk`. SHA-256: `34d00f31df6596e4ffafd1bd0e6c7fcaf8a95576c51a5fb78d97209c68ae926f`.

## 2026-10-26: system-font tile-label baselines, 1.0.0

Inter remains limited to the clock (Light) and English Gregorian date (Regular). App titles, tile labels, missing-artwork initials, and lunar text retain the system font.

The prior default-font UI tree showed a 37px Chinese label box and 30px Latin boxes. Top padding alone did not align their different font metrics. Tile labels now use standard Compose paddingFromBaseline (25sp above / 5sp below), includeFontPadding=false, an 18sp line height, centered line-height distribution, and no trimming. There is no language detection, per-language offset, custom font family, or density override. Glyph ink heights naturally differ; layout is aligned by baseline rather than forced glyph dimensions.

Unit tests, Lint, and Release build passed. Installed the final Release and inspected Chinese/English labels at font_scale 1.0 and separately at 2.0; text remained legible without clipping and long labels ellipsized. Restored 1.0 afterward. No device test suite was rerun for this local text-layout change. Screenshots: `artifacts/inter-20261026/40-system-label-baselines.png` and `41-system-label-baselines-font-2-stress.png`; UI tree: `label-baselines.xml`. Version remains 1.0.0, code 4.

## 2026-10-26: English project text and Inter typography, 1.0.0

Translated project documentation and remaining explanatory comments into English. Project-owned Toasts and spoken descriptions are English; Chinese lunar-calendar strings and Chinese collation fixtures remain intentional content. Original installed-app labels are unchanged. Historical results, limitations, artifact names, hashes, and experimental versions below remain historical.

Bundled only official Inter 4.1 Light and Regular TTFs for the clock and English date. Both fonts and the packaged SIL OFL notice match the official release archive byte-for-byte. No italic/variable/script fonts, runtime font downloads, dependencies, or permissions were added. App titles, tile labels, fallback initials, and lunar text have no explicit font family and use Android's system font. Release grew from 1,734,428 to 2,100,198 bytes: +365,770 bytes (about 357 KiB); versionName 1.0.0 and versionCode 4 are unchanged.

Removed manual colon BaselineShift in favor of Inter's OpenType `tnum, case` features. Used standard text font-padding settings and explicit line heights: clock 64sp, dates 20sp, large app title 54sp, labels 18sp, fallback initials 44sp. Centered the date group directly against the clock, removing the old 9dp bottom offset. Preserved 15dp separator margins, 4dp date-line gap, 1.5sp/1.15sp tracking, and 70%/55% date opacity. Increased grid side padding from 8dp to 10dp so 1.1x focus and its 1dp border have room at both edges; five columns, 18dp gaps, 3dp lift, shared tile/border shape, 10dp top focus inset, and fade boundary remain intact.

The complete unit/Lint/Release/device command passed in 2m 34s: 3 JVM tests and 20 Android 14 TV emulator tests, no failures/skips; Lint zero errors/warnings. After the final side-padding adjustment, repeated unit/Lint/Release and the five-column/third-row device test; one selected device test ran and passed. No new test-only production logic or typography tests were added.

Installed the final Release on the 1920x1080, 320dpi API 34 TV emulator. Inspected default-font screenshots of Chinese and English app names, clock colon, date-line alignment, text line heights, tile-label spacing, and left/right focus edges; no overlap or clipping was observed. A separate font_scale=2.0 stress screenshot kept the five-column layout, preserved focus outlines, and ellipsized long labels. Restored the original 1.0 font scale and Home focus afterward. These emulator observations do not claim new Sony hardware, power-cycle, or long-run validation.

Screenshots: `artifacts/inter-20261026/37-inter-default.png`, `38-inter-english-right-edge.png`, and `39-inter-font-2-stress.png`. UI trees: `default.xml` and `font-2.xml` in the same directory. Standard APK and test/Lint report paths remain unchanged.

## 2026-10-26: stable toolchain and pre-commit review, 1.0.0

Reviewed production code, tests, resources, Manifest, build configuration, and documentation. Retained one app module and the data/system/ui responsibilities. Simplified local branches, imports, and formatting without adding architecture or product state. Kept README to a title and one sentence; technical constraints live in AGENTS.md. Added the original MIT icon notice and key-file ignore rules.

Official Google Maven and Maven Central metadata, [Kotlin releases](https://kotlinlang.org/docs/releases.html), and [Gradle stable-version metadata](https://services.gradle.org/versions/current) were checked at that time: Kotlin 2.4.21, AGP 9.4.1, Compose BOM 2026.09.00, Activity 1.13.0, TV Material 1.1.0, Lifecycle 2.11.0, Coroutines 1.11.0, JUnit 4.13.2, AndroidX Test Runner 1.7.0, and AndroidX Test JUnit 1.3.0 were stable. Gradle moved from 9.6.0 to 9.8.2; Wrapper files were regenerated with the official SHA-256. Build Tools 37.0.0 and JDK 27 are shared by CI and local builds. SDK 37, minimum SDK 34, JVM 17, versionName 1.0.0, and versionCode 4 were retained.

Removed unnecessary ARM ABI filters. All four dependency ABIs are packaged, adding about 20 KB and removing the ChromeOS ABI warning. All four prebuilt libandroidx.graphics.path.so binaries are already stripped; duplicate stripping is skipped without introducing an NDK. Final Lint: zero errors and zero warnings.

Added the minimal release.yml workflow: publication of the v1.0.0 GitHub Release restores ANDROID_KEYSTORE_BASE64, installs the SDK, runs unit tests/Lint/Release, and uploads Interlude-1.0.0.apk. YAML and shell checks passed. It has not been run on GitHub; the signing Secret must be configured before release. No commit, tag, or Release was created.

Upstream warnings remain visible: AGP 9.4.1 BasePlugin.createAndroidJdkImageConfiguration uses deprecated Gradle Configuration.setVisible; device tooling's protobuf-java 3.25.5 uses sun.misc.Unsafe on JDK 27; standalone apksigner/Conscrypt reports native access. No global suppression, internal dependency replacement, or prerelease workaround was introduced.

Results: 3 JVM tests, Lint, Release, and APK signature verification passed. All 20 Android 14 TV emulator tests passed, with no failures or skips. The native stripping task was rerun to confirm the duplicate-stripping warning was gone.

## 2026-10-26: single Inputs action, descriptions, and naming, 1.0.0

Inputs now calls only `com.android.tv.action.VIEW_INPUTS`; removed the settings fallback and its Manifest query. Retained short button and tile descriptions for speech and test reuse, without production test tags or an extra accessibility layer. Removed generated network-status strings and Internet validation; NetworkCapabilities directly determines Ethernet and disconnected appearance.

An attempt to remove all descriptions and locate nodes by structure caused sibling-match ambiguity and ordering differences in the first device run. The user then explicitly allowed necessary spoken descriptions, so tests returned to precise description-based matching.

| Previous name | Current name | Reason |
| --- | --- | --- |
| HomeModel.kt / HomeModel | HomeViewModel.kt / HomeViewModel | Android ViewModel lifecycle role |
| data/Apps.kt / Apps | data/AppCatalog.kt / AppCatalog | Catalog, artwork, and local records |
| repository | catalog | No implied repository layer |
| hidden collection | hiddenIds | Identity set |
| visible / concealed lists | visibleApps / hiddenApps | Element type and consistent terminology |
| ready / hiddenMode | isLoaded / showHiddenApps | Distinct Boolean meanings |
| focus / focusTarget | focusedId / pendingFocusId | Current versus requested focus |
| hide / opened / home / focus methods | setHidden / recordLaunch / onHome / onFocusChanged | Explicit operation and event names |
| TvActions.open / settings / network / inputs | openApp / openSettings / openNetworkSettings / openInputs | Explicit system actions |
| HeaderIcon | HeaderButton | Interactive control |
| AppEntry.banner / accent | isBanner / accentColor | Boolean and color |
| pm / prefs / cache / configuration / used | packageManager / preferences / entryCache / cachedConfiguration / launchTimes | Object and record roles |
| drawable/icon.xml / banner.xml | ic_launcher.xml / launcher_banner.xml | Launcher resources |
| duplicateLabelsUseStableComponentIdentity | duplicateLabelsUseStablePackageIdentity | Actual ordering identity |

Kept standard Android java/androidTest/res/Gradle/Manifest layout and existing MainActivity, HomeState, AppEntry, AppOrder, Clock, HomeScreen, and test classes. Ordinary local indices and color channels were not expanded unnecessarily. Package art.anjing.interlude, preferences apps, and hidden/used: keys did not change; no data reset or migration was caused by renaming.

Full command: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:connectedDebugAndroidTest --offline`. BUILD SUCCESSFUL in 2m 24s: 3 JVM and 20 emulator tests passed; Lint had zero errors and two then-existing warnings; Release passed. Product documentation was uploaded and checksummed.

## 2026-10-26: loading and launch-failure messages removed, 1.0.0

Removed the unavailable-function Toast and all callers. System-entry failures are silent. Failed app launches still refresh the list; only successful launches record recency. Removed loading text and its empty container; the initial app area is blank.

At this historical point, documentation described VIEW_INPUTS followed by TV_INPUT_SETTINGS and a spoken-only no-Internet suffix. Both were removed in the later single-action review above. Hidden/launch records remained in apps SharedPreferences, using package identities and asynchronous apply.

The full offline unit/Lint/Release/device command passed: 3 JVM and 20 emulator tests, zero Lint errors and two existing warnings. Version stayed 1.0.0. APP_DESIGN.md was uploaded and checksummed.

## 2026-10-26: Back independent of hidden-section expansion, 1.0.0

Back now preserves expansion, focus, and scroll and does not touch Toasts. Holding Settings still expands/collapses; restoring the last hidden app ends the section; process restart closes it. Removed HomeScreen's collapse callback and Activity forwarding; retained an empty consuming BackHandler. Tests now verify that Back leaves a hidden tile focused and the section expanded, then verify Settings can collapse and reopen it.

The full offline command passed: 3 JVM and 20 emulator tests, zero Lint errors and two existing warnings, Release successful. Version stayed 1.0.0. Documentation was uploaded to the Interlude directory and checksummed. Older Back-collapse entries below are historical.

## 2026-10-26: minimal implementation review, 1.0.0

Read all seven production Kotlin files, four test files, Manifest, XML resources, Gradle files, and project documents. Existing uncommitted work was retained; no commit was made. Version remained 1.0.0, code 4. No confirmed severe functional gap, duplicated page, or need for additional layers was found.

| Severity | Finding | Resolution |
| --- | --- | --- |
| Medium | RuntimeException around all of Apps.load entry construction could turn programming errors into missing apps | Removed the broad outer catch and cancellation rethrow needed only by it; retained uninstall races, permission, vendor-label, and artwork boundaries; added logs for label/permission failure |
| Medium, coverage gap | Existing tests did not explicitly cover five-column section boundaries, first-row navigation with all apps hidden, or offscreen restoration | Added normal counts 0/1/5/6 and a 21-app D-pad restoration case |
| Low | A nullable app list represented a separator and required mixed item callbacks | Emit normal section, full-span separator, and hidden section directly, reusing the same tile |
| Low | Every confirm event allocated a set; app changes copied requester maps | Use when for keys and reuse/clean the existing requester map |

Reviewed discovery, package deduplication, local recency, equal-label ordering, clock rollback, hidden grayscale tiles, short-press launch, focus migration, Home/Back, state ownership, long-press repeat/up consumption, lifecycle callbacks, cancellable IO scans, bounded/reused artwork, legacy record migration, and uninstall cleanup. Kept one state owner and no DI/repository/service/compatibility layer. AppOrder, TvActions, and FocusSurface each retain real shared responsibilities; no class was split merely to shorten files.

The first exception-boundary batch passed 3 JVM and 18 device tests, Lint, and Release. An early offscreen test incorrectly requested an offscreen initial focus and failed; it was changed to navigate through the real D-pad path. Same-frame key injection also failed to reach the intended target, so the test now waits for layout between keys. No production branch was added for test conditions.

Final offline command passed in 2m 22s: 3 JVM and 20 Android 14 TV tests (6 hidden, 9 home, 5 platform), no failures/skips. Lint: zero errors, two existing warnings (Gradle upgrade and ChromeOS x86_64 ABI). R8/resource-shrunk Release passed and was reinstalled.

Release checks at 1920x1080, 320dpi, font scale 1.0 found no overlap or focus clipping. NetworkActivity and MainSettings opened via D-pad; Back restored the appropriate header focus. Screenshots: `artifacts/minimal-review/release-home.png`, `release-network-return.png`, `release-settings-return.png`.

Reports: `app/build/reports/androidTests/connected/debug/index.html`, `app/build/reports/lint-results-debug.html`. APK: `app/build/outputs/apk/release/app-release.apk`. Protobuf Unsafe warnings came from test tooling.

No Sony was online. Physical remote-specific keys, vendor-page returns, power cycles, long-run memory/frame time, and damaged vendor-resource injection were not validated. Historical screenshots and short samples were not treated as acceptance of these cases.

## 2026-10-26: unnecessary font-scale overrides removed, 1.0.0

Mixed default/2x screenshots reflected different test settings, not language-dependent product sizing. Removed fixedTypography and the two Density/fontScale CompositionLocalProviders introduced to compensate. Text keeps native 52sp/14sp/42sp sizing and follows Android preferences. Added the minimal-implementation and default-font-comparison rules to AGENTS.md. Five columns, fade, behavior, and version remained unchanged. Earlier large-font screenshots are historical stress tests. Release, Lint, and 3 JVM tests passed, without new warnings.

## 2026-10-26: five columns, title, and top fade, 1.0.0

Screenshot 20 used 2x fonts; 21 used default fonts. At this historical stage the title was temporarily fixed at 42sp Light with fixed density; both language bounds matched between scales: Chinese `[92,233][603,354]`, English `[92,244][423,343]`. Glyph metrics differ naturally; there were no language-specific sizes. These font overrides were subsequently removed.

Changed to five columns and reused that count for first-row up navigation. Hidden apps still start a separate row. Added a 32dp scroll extension in the gap below the title without moving the resting first row; fade ends at its original edge. At 1080p the original top and fade endpoint were both y=429px, with 64px above for fading. Screenshot 26 shows rest; 27 shows third-row Play Store focus with scrolled content fading.

Added five-column wrapping and third-row D-pad tests: 3 JVM and 18 device tests passed, Release passed, Lint zero errors/two existing warnings. Screenshots 22/23 compare default Chinese/English titles, 24/25 compare 2x, 26/27 compare rest/scroll. Temporary apps were uninstalled and font settings restored; version stayed 1.0.0.

## 2026-10-26: date proportions and multirow screenshots, 1.0.0

Earlier screenshot 19 at 2x had disproportionate dates; calling it correct was inaccurate. A temporary local fontScale=1 Density fixed header text while preserving density and app-name scaling. Default and 2x date bounds matched: English `[398,78][737,111]`, lunar `[398,119][612,159]`; screenshot `20-header-font-2-fixed.png`. This override was later removed.

Installed eight temporary entries alongside five existing apps for 13 apps. D-pad selected third-row YouTube with bounds `[108,829][362,972]`; screenshot `21-third-row-top-fade.png`. Existing opacity fading was used, not a new blur. Release, Lint, and 3 JVM tests passed; Lint zero errors/two existing warnings. Did not repeat the prior 17 device tests. Restored settings and uninstalled temporary apps; version stayed 1.0.0.

## 2026-10-26: Toasts and header buttons, 1.0.0

Hide/restore wording was unified for all apps, including the last. At this stage Back collapsed hidden apps without calling the Toast helper; Settings expansion/collapse still displayed native Toasts. Back behavior was subsequently changed to preserve expansion.

Reduced buttons from 48dp to 40dp and icons from 22dp to 20dp; removed the button-row bottom offset and centered it against the clock. The large app title is a project design choice, not claimed as a tvOS 27 requirement.

Release, 3 JVM, and 17 device tests passed; Lint zero errors/two existing warnings. Added screenshots 15/18/19 for default fonts, large fonts, and expired Toasts. Emulator PNG capture takes about two seconds, so sequential screenshots cannot prove a short Toast remains at the exact key instant; callback paths were reviewed.

## 2026-10-26: blank app area and native Toasts, 1.0.0

Fixed the product at 1.0.0 while retaining versionCode for update compatibility. All-hidden state removed empty-home text/headings, leaving the header usable. At this stage hide/restore/expand/collapse, including Back collapse, used native Toasts; later Back behavior supersedes this.

Release, 3 JVM, and 17 TV tests passed; Lint zero errors/two existing warnings. Screenshots under `artifacts/Interlude-1.0.0-review-20261026/` covered home, Toasts, grayscale hidden apps, blank all-hidden state, and restoration. Restored all apps afterward. Uploaded 16 screenshots and README.md via SCP to copyparty `Files/Interlude/`; all SHA-256 values matched. Anonymous web access required login with the existing account.

## 2026-10-26: full review and refactor

Reviewed seven production files, Manifest, resources, build, and tests. The then-current single page had six columns, recency, long-press hide/restore, and temporary hidden-section expansion. No Java, obsolete management page, or duplicate app list remained.

Moved initial preferences reads and normalization to background scanning; serialized/cancelled scans; reused entries/artwork by component/update/configuration; evicted absent entries. Artwork failure no longer removes launchable apps; missing names fall back to package names; failed images can retry. Serialized hidden-set read/modify/write against uninstall cleanup while querying PackageManager outside the lock. No-op hidden updates return without writes or focus moves; usage-record reading became private.

Reused surviving FocusRequesters; removed an unused UI Home callback. Ordinary Back retained focus and expanded Back then collapsed the hidden section; Home selected the first app. Used an independent composition layer for fading and reserved focus margins. Icons fit tile space and dates used remaining width. Long-press tests inject down/repeat/up with key-up in finally. Removed irrelevant Java/BlueJ/J2ME and duplicate gitignore rules.

Retained Activity lifecycle/events, HomeModel state, Apps discovery/thumbnails/persistence, and HomeScreen rendering/focus. No added dependencies, pages, or generic architecture. Component-to-package normalization preserves installed data and is not a second product implementation.

The full offline command passed: 3 JVM and 17 API 34 TV tests (6 hidden, 6 home, 5 platform), Release with R8/resource shrinking, Lint zero errors/two existing warnings. Default and 2x Release screenshots showed no overlap/clipping; long names ellipsized; original font settings were restored. Screenshots: `artifacts/review/default.png`, `artifacts/review/font-2.png`. Reports and APK use the standard build paths above. Tooling reported protobuf Unsafe deprecation.

Open coverage then included physical Sony remote events/Home, vendor pages, one/many apps, rapid scrolling, edge columns/fade, 720p/4K/non-16:9 and font combinations, install/update/component changes/uninstall/disable/re-enable, vendor visibility, process/power/time-zone changes, long-run memory/frame time, and damaged vendor APK injection. Earlier Sony results were not current-code acceptance.

## 2026-10-26: long-press hide/restore, historical 1.2.0

Reused six-column tiles and removed the management dialog. Long-press normal/hidden apps hides/restores; Settings toggles expansion; Back then collapsed it. A nonfocusable full-span separator starts the grayscale hidden section. Names are unchanged. Records use package identities with migration; changed launcher activities preserve identity; removed packages are cleaned; expansion is in-memory only.

Results: 3 unit tests; 9 basic device checks including record migration/cleanup; 6 hidden-interaction tests run in groups. Real confirm-key tests monitored launch intents; Release opened `art.anjing.tigertv/.MainActivity` without restoring hidden state. Color/grayscale, focus following restoration, update persistence, closed section after process restart, and consumed repeats/up after long-press removal all passed. Key bookkeeping did not recompose the clickable surface. Lint zero errors/two existing warnings; Release/signature passed with ARMv7 and ARM64. Temporary hidden apps were restored; Sony was offline.

APK: `artifacts/Interlude-1.2.0-universal-20261026.apk`, 1,648,845 bytes. SHA-256: `aa7abf2a02b07b245c8b663e65c9a42f9512079deda61350fd8f55493aa5575c`.

Screenshots: `artifacts/hidden-apps/expanded-stable.png`, `artifacts/hidden-apps/restored.png`.

## 2026-10-26: greeting removed

Removed the home greeting, ClockText.greeting, time-of-day logic/tests, and layout placeholder. README was updated. Release, device-test compilation, and Lint passed (zero errors/two existing warnings). Installed Release on API 34 TV; screenshot/UI tree confirmed the greeting was gone and clock/dates/buttons rendered. Did not rerun device tests or operate Sony. Screenshot: `artifacts/emulator-no-greeting.png`.

## 2026-10-26: typography and interaction, historical 1.1.0

Environment: Television_1080p, 1920x1080, ARM64, API 34, installed Release. Passed 3 JVM and 9 Android tests, R8 Release, and v2 signature with the existing debug key. Lint zero errors/two existing warnings. Checked the small greeting, 52sp clock, separator, uppercase date, sexagenary lunar date, circular buttons, focus ring, Ethernet/Wi-Fi icon selection, native-switch app management dialog, six-column grid/fade, new package art.anjing.interlude, Home resolution, and empty crash log. Removed app count. Lunar tests covered 2026-02-17 lunar new year and 2025-07-25 leap month; greeting boundaries covered 05:00, 11:00, 12:59, 13:00, and 18:00.

APK: `artifacts/Interlude-1.1.0-universal-20261026.apk`, 1,726,537 bytes. SHA-256: `4683612bb19159c0948f050c5b7e6494ba81fe762e7f235e60bb4830e509bbe7`.

The package rename from dev.anjing.interlude had no data migration: preferences remained package-specific. Assigned the new Home role before uninstalling the old emulator package. Device tests uninstall the target, so Release and Home role were restored afterward. Sony at 192.168.5.75 was offline and retained the old package pending a later update.

## 2026-10-26: historical 1.0.1

Same 1080p ARM64 API 34 TV environment. Passed 3 JVM and 7 Android tests, R8 Release, v2 signature, six-column grid, header focus/Back, management-list first/last focus clipping, greeting/clock/date/app-name typography, and hide/show messages. Removed bottom-right help text. Eight temporary entries made 13 apps/three rows; reaching row three demonstrated top fading. Temporarily enabling com.google.android.tvlauncher allowed InputsPanelActivity and restored Inputs focus on Back. Input crash was not reproduced; crash log was empty. Lint zero errors/two existing warnings.

APK: `artifacts/Interlude-1.0.1-universal-20261026.apk`, 1,681,141 bytes. SHA-256: `816cb8f6366cac05257f34313f059edd0f86987355786372bfda67897ce9f017`.

Sony was not updated in this run. Package queries confirmed VIEW_INPUTS resolves to `com.android.systemui/com.sony.dtv.systemui.quicklauncher.LaunchActivity`, the same picker as the remote Inputs key.

## 2026-10-25: Android 14 TV emulator

Environment: Television_1080p, 1920x1080, ARM64, API 34. Passed 3 JVM and 7 Android tests, R8 Release, v2 signature, ARMv7/ARM64 packaging, initial/D-pad/Home focus, real Settings long press, system-settings panel return, recency refresh, and all-hidden Settings access. Lint had no errors and the then-expected Gradle/ChromeOS ABI warnings.

The emulator stock launcher com.google.android.tvlauncher had higher HOME priority even after role assignment. Disabling it made actual Home enter Interlude. This was emulator-specific; it is not authorization to disable a physical TV's stock launcher. Historical restore commands:

```sh
adb -s emulator-5554 shell pm enable --user 0 com.google.android.tvlauncher
adb -s emulator-5554 shell cmd package set-home-activity com.google.android.tvlauncher/.MainActivity
```

Artifacts are local and ignored by Git. Screenshot dates follow emulator time; lunar correctness was checked separately against lunar new year, leap months, and midnight/time-zone boundaries.

APK: `artifacts/Interlude-1.0.0-universal.apk`, 1,629,573 bytes. SHA-256: `ae4cd10d1ef7400b668cd536d21ed8c7b79bc7af886eb012f5e3962231ef8fc7`.

## Historical Sony BRAVIA 5 checks

Reported model BRAVIA 4K AE2, Android 14/API 34, ABI armeabi-v7a/armeabi only. Physical 3840x2160 with existing 1920x1080 UI override; resolution was not changed. Universal Release ran on ARMv7; emulator covered ARM64.

Passed installation/first start/process recreation after force stop, Chinese greeting/Gregorian/lunar/clock rendering, discovery of 24 apps, D-pad/multirow scrolling, native Inputs picker with named devices including PlayStation 5 (no input switched), Network and Settings entry/Back focus, long-press management (temporarily hidden Help was restored; all 24 remained), app launch/recency (a video app moved first after Home), and actual Home/default-launcher resolution. No AndroidRuntime crash was observed.

Keys were ADB-injected D-pad/confirm/long-press/Back/Home events, not exhaustive physical manufacturer-specific remote checks. No power-cycle or long-run test was performed. The stock launcher com.dangbei.TVHomeLauncher stayed enabled. Historical restore command:

```sh
adb -s <sony-address> shell cmd package set-home-activity com.dangbei.TVHomeLauncher/com.dangbei.leard.leradlauncher.ui.sony.main.MainActivity
```

Short rendering sample: initial multirow scroll 39 frames, 9 janky (23.08%, including first image upload/layout); warmed horizontal focus 288 frames, 7 janky (2.43%), P50 18ms/P90 34ms. These dumpsys gfxinfo samples are not sustained frame-rate guarantees. PSS was about 105 MiB.

Screenshots, UI trees, and frame samples are under `artifacts/sony/`, final home `artifacts/sony/home.png`. No source change was made in that run; APK matched the historical hash above.


## 2026-10-26: latest Release on physical Sony

Installed current `art.anjing.interlude` Release 1.0.0 (versionCode 4) on paired BRAVIA 4K AE2 at 192.168.5.75, Android 14, armeabi-v7a, 1920x1080 UI, default font scale 1.0. Release build, three JVM tests, and Lint passed; Lint reported no issues. APK SHA-256: `e3358883b04a6757da3f5d16c5df22995efbb4aff9a2283233e8fd29d638adb5`.

At the user's request, uninstalled old `dev.anjing.interlude`. Set the new package as Home and verified KEYCODE_HOME resolves to and resumes it; the stock launcher was not disabled. Checked clock/dates, five-column grid, third-row scrolling and top fade, real key-event long-press hide of Help, Settings long-press expansion, navigation to the hidden Help tile, and long-press restoration with focus following Help. Tested TV app launch and Home return. Inputs displayed Sony's native picker without selecting an input; Network opened NetworkActivity; Settings opened MainSettings, confirmed by its UI hierarchy after the vendor transition settled. No crash-buffer entries occurred during this session. Returned to the new Home with the temporarily hidden app restored.

Evidence: `artifacts/sony-latest-20261026/` (screenshots, UI trees, session crash log). Keys were ADB-injected on the physical TV; physical remote hardware, power cycling, long-run performance, all-hidden mode, and exact Toast timing were not tested in this run. Connected instrumentation tests were not run, avoiding replacement/uninstallation of the installed Release. No production source changes were made.


## 2026-10-26: curtain identity, date tracking, and undistorted artwork

Changed English date tracking to 1.25sp and lunar tracking to 1sp, retaining native font scaling. Replaced the pause mark with pure white open stage curtains on black, provided an adaptive Android icon (including a monochrome layer), a vector TV banner with the Interlude wordmark, and a 512x512 sRGB Play Store PNG. The upload image is opaque and square; a separate preview applies the current 30% Play corner mask. Assets and notes are in `branding/`. No image-generation model or additional bundled fonts were used.

On Sony, inspected Lebo `com.hpplay.happyplay.aw`: its banner and launcher icon both reference the same 144x144 PNG. Square/portrait banners now use padded icon fitting; thumbnail rasterization preserves intrinsic proportions within 384x216 for landscape banners and 128x128 for icons. Other landscape banners retain proportional cropping.

Release, three JVM tests, and Lint passed (no issues). A new ArtworkTest passed on the Android 14 TV emulator, checking square, wide, portrait, and 16:9 source proportions for both thumbnail modes within half-pixel integer rounding. The initial test used an incorrect fixed ratio tolerance for small integer thumbnails; corrected the assertion to allow half a pixel of rounding.

Installed the final 1.0.0/versionCode 4 Release on physical Sony using a data-preserving update. Screenshot and UI tree at `artifacts/brand-20261026/sony-home.*` show the revised date spacing and the square Lebo icon centered without stretching. Returned to Home. APK SHA-256: `cab5a2b99f0b90493bdc7659881f9ff420e3ccfde8359aada0abea5dba5fc701`. This incremental check did not repeat the full navigation or hidden-app suite.


## 2026-10-26: tighter title gap, larger fade, and default sorting

Moved the app title up 6dp within its existing 116dp reserved area by adding 12dp bottom padding; the resting tile row stays at the same edge. Increased the scroll-only fade from 32dp to 44dp, ending at that original first-row edge. Default sorting now prioritizes installed packages `com.gitvdemo.video`, `com.cibn.tv`, `com.ktcp.video`, and `com.hunantv.market`, in that order, followed by Chinese-leading names in native Chinese pinyin collation and remaining names in case-insensitive alphabetical order. Recent successful launches remain the primary sort key. Equal names resolve by package identity. Existing launch/hidden record storage and migrations remain intact.

Passed Release build, six JVM tests, all 21 Android 14 TV emulator tests, and Lint with no issues. Unit coverage includes exact package priorities, a same-label nonpriority package, missing priorities, Chinese-before-English ordering, English case ties, and recency overriding default priorities.

Installed 1.0.0/versionCode 4 on physical Sony. Before clearing data, existing recent TV use still ranked ahead of all four default-priority packages. Screenshots confirmed tighter title spacing, intact resting tiles, and the expanded fade when focusing row three. After testing, ran `pm clear art.anjing.interlude` successfully as requested and returned with KEYCODE_HOME. The clean initial order was 银河奇异果, CIBN酷喵, 云视听极光, MGTV-芒果TV, 帮助, 电视; focus was on 银河奇异果. Home resolution remained `art.anjing.interlude/.MainActivity`. No app was launched or hidden after clearing data.

Evidence: `artifacts/layout-order-20261026/` (`before-reset`, `scrolled`, and `default-home` screenshots/UI trees). APK SHA-256: `f0860ec7af92fb6cec017448e780e78f23b76028851e06cf0406f3c8bfceeca5`.


## 2026-10-26: restrained 5% tile focus scale

Reduced tile focus scale from 1.10 to 1.05, matching header button scale. Retained 18dp grid spacing, 12dp tile corners, 3dp focus lift, and the existing borders. On Sony's 960dp-wide UI, tile width is about 155.2dp: each side grows 3.88dp at 5%, leaving about 14.12dp between one focused tile and its resting neighbor. Even conservatively allowing both adjacent tiles to reach 1.05 during a transition leaves about 10.24dp. The top focus inset also exceeds the 3dp lift plus the approximately 2.18dp vertical scale extension.

Release, six JVM tests, and Lint passed with no issues. Installed Release 1.0.0/versionCode 4 on physical Sony. Inspected first-column, middle-column, last-column, and scrolled row-three screenshots: focus was visible without tile overlap or edge clipping. Returned using Home; no apps were launched or hidden, preserving the clean default records and default order. The connected device suite was not repeated for this single scale change. Evidence: `artifacts/focus-5-20261026/`. APK SHA-256: `251bffcc300f3cafcf3baafb779f666cb3078028e7d50c3a234ab1970b5511e5`.


## 2026-10-26: title closer to app tiles

Reduced the reserved title area's bottom padding from 12dp to 4dp, moving the title down 4dp without changing the grid, clock/date group, font sizes, or focus geometry. The title now sits 2dp above the center of its 116dp reserved area. Release, six JVM tests, and Lint passed with no issues. Installed 1.0.0/versionCode 4 on Sony and checked the resting screenshot/UI hierarchy. No apps were launched or hidden; the clean default order was preserved. Evidence: `artifacts/title-gap-20261026/home.*`. APK SHA-256: `e467e8ecbd326e57dd5c20654d578bf5f3aa3aff4f5e3fdde60c6f4eea4d08f5`.


## 2026-10-26: additional 2dp title adjustment

Removed the remaining 4dp bottom padding from the title area, moving the centered title down another 2dp. Release, six JVM tests, and Lint passed. Installed on Sony; UI hierarchy comparison confirmed a 4px title shift with unchanged tile bounds. No apps were launched or hidden. Evidence: `artifacts/title-gap-20261026/home-plus-2dp.*`.


## 2026-10-26: selected-app title removed

Removed the large selected-app text, its Crossfade import/animation, TitleHeight constant, overlay container, title spacing, and redundant grid wrapper. The grid now follows the header directly with its existing 54dp top padding (44dp fade region plus 10dp focus inset). Shared app-focus tracking remains necessary for header-down navigation and artwork glow. Labels appear only below tiles.

Rebalanced the remaining layout: 48dp horizontal and 32dp top page padding, 48sp clock with 60sp line height, and 16dp separator-side spacing. Date sizes/tracking, five columns, 18dp grid spacing, 5% focus scale, and 44dp scroll fade remain. Sony default-font screenshots show three complete rows of tiles and labels; no title placeholder or title-sized blank area remains.

Passed Release build, six JVM tests, all 21 Android 14 TV emulator tests, and Lint with no issues. Extended existing navigation coverage to return from Inputs to the second app and to assert that the synthetic focused app label appears once. On Sony, checked resting and deeper scrolling screenshots, Inputs focus, Down returning to CIBN酷喵, and Back preserving that focus. Key events were ADB-injected.

After testing, successfully cleared only `art.anjing.interlude` data as previously requested and used Home. Default package order and a single selected-app label were confirmed; Home still resolves to the new package. Version remains 1.0.0/versionCode 4. Evidence: `artifacts/no-title-20261026/`. APK SHA-256: `ca54242f0a31e09bf08b8d4fcc39d17e5b79f240662f3967f1122291587ca78e`.


## 2026-10-26: larger, brighter tile labels

Increased tile labels from 13sp/18sp line height to 14sp/20sp. Unfocused labels now use Ink at 80% opacity instead of muted gray; focused labels retain full Ink opacity. Kept the same size in both focus states, baseline padding, system font, single-line ellipsis, and 150ms color transition. The disconnected network icon retains its separate muted treatment.

Release, six JVM tests, and Lint passed with no issues. Installed 1.0.0/versionCode 4 on physical Sony and inspected `artifacts/labels-20261026/home.*`: the title-free three-row layout remains readable, with no tile overlap or cropped labels. No apps were launched or hidden, preserving clean default records. APK SHA-256: `edecbf3007a28429c3e1924d1bea8788ea33fd257290a4bec1182609dda691cd`.


## 2026-10-26: permanent fade and Medium selection labels

Removed the scroll-offset threshold, derived scroll flag, and 220ms top-fade opacity animation. The grid always uses the same 44dp alpha mask. Its endpoint now precedes the resting first-row edge by the 10dp focus inset, preserving full opacity for resting tiles including their 5% scale and 3dp lift. This removes the fade's scroll-triggered appearance/disappearance rather than delaying or smoothing the trigger.

App labels explicitly use system Normal (400) weight when unfocused and Medium (500) when focused, retaining 14sp/20sp dimensions and 80%/100% Ink opacity. No new fonts or label scale were introduced. Clock remains Inter Light; English date remains Inter Regular.

Release, six JVM tests, and Lint passed with no issues. Installed 1.0.0/versionCode 4 on Sony and inspected resting, deeper-scrolled, and returned screenshots/UI trees under `artifacts/permanent-fade-20261026/`. Resting artwork remains undimmed, selection remains readable, and the mask is independent of scroll state. Screenshots are static evidence; the removal of the opacity animation is verified in code. No app was opened or hidden; Home restored default focus without changing clean records. APK SHA-256: `8d886933ac7b302a7815135f515795849af27a8581acda3e22934e7cba8d8bb9`.


## 2026-10-26: 10% tile focus and longer permanent fade

Changed app-tile focus scale to 1.10; header buttons remain 1.05. Increased the permanent fade from 44dp to 52dp, retaining the separate 10dp clear focus inset. Grid top padding is consequently 62dp. Resting focused tiles remain below the fade endpoint, with about 2.66dp top clearance after the scale extension and 3dp lift on Sony. At Sony's 960dp logical width, a 154.4dp-wide tile extends 7.72dp per side: a focused tile retains about 10.28dp spacing from a resting neighbor; even two adjacent tiles fully scaled leave about 2.56dp of the 18dp gap.

Release, six JVM tests, and Lint passed with no issues. Installed 1.0.0/versionCode 4 on Sony and inspected first-column, middle-column, and deeply scrolled screenshots/UI trees. No neighboring tile overlap, edge clipping, or resting-tile fade was observed. The fade remains independent of scroll state; focused labels remain Medium 500 at 14sp. Returned Home without opening or hiding apps, preserving default records. Evidence: `artifacts/focus-10-fade-52-20261026/`. APK SHA-256: `e89d2dcf52c67560caecc53a2ef3c968de5236f11e49fe95ca3fdd37858d00aa`.


## 2026-10-26: slightly stronger ambient glow

Raised ambient radial-gradient center opacity from 22% to 28% and its middle stop from 6% to 8%. Retained the selected-artwork color, gradient center/radius, and 650ms color transition. Release, six JVM tests, and Lint passed with no issues. Installed 1.0.0/versionCode 4 on Sony and inspected green/orange selection screenshots under `artifacts/ambient-20261026/`: the stronger color remains behind readable labels and artwork. Returned Home without opening or hiding apps, preserving default records. APK SHA-256: `6d9793d989c309f0e3e1c534e5fe7c9b64aed47b0a0905d874ca4b986e1c5bd2`.


## 2026-10-26: final pre-release review

Reviewed all seven production Kotlin files, four device-test files, unit tests, Manifest, resources, Gradle configuration, wrapper, licensing, and release workflow. Kept one app module and the existing Activity/ViewModel/data/system/ui boundaries; no services, repositories, dependency injection, polling, or adaptation layers were added. Necessary comments remain short English explanations of platform or lifecycle boundaries.

Cached the fixed gradient brush with drawWithCache instead of allocating it on each draw. Regenerated the TV wordmark from the existing unmodified Inter Regular source, replaced the previous system-font outline provenance, supplied the missing Android icon preview, and completed README build/use/release instructions. Current UI geometry and behavior were retained: five columns, native sp scaling, 10% tile focus, Medium selection labels, permanent 52dp fade plus a clear 10dp focus inset, and 28%/8% ambient opacity.

Authoritative Maven metadata confirmed the configured AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, Activity 1.13.0, TV Material 1.1.0, Lifecycle 2.11.0, Coroutines 1.11.0, Test Runner 1.7.0, and Test JUnit 1.3.0 as the latest stable versions checked that day. Gradle's current-version endpoint confirmed 9.8.2. Actions were pinned to verified latest stable patch releases checkout 7.0.1, setup-java 6.1.0, and setup-gradle 6.4.0. The workflow still responds only to publication of the v1.0.0 Release and now verifies the APK signature before upload. actionlint 1.7.12 passed.

Local Release, six JVM tests, all 21 API 34 TV emulator tests, and Lint passed (no issues). APK includes ARM64, ARMv7, x86, and x86_64 without ABI filtering. The only runtime platform permission is ACCESS_NETWORK_STATE; the merged Manifest also contains AndroidX's generated signature receiver permission. The existing local signing key matches Sony's installed certificate SHA-256 `0d81ab7cb1a16d58d91a953fe62049eb77fc840cc544a297ae1854f54bc73b96`; the GitHub secret was synchronized without printing or committing the key.

Sony warmed navigation sample: 980 rendered frames, 8 janky (0.82%), P50 18ms/P90 34ms; PSS 79,867KB before and 81,838KB after, no swap and no crash-buffer entries for this process. This short sample does not prove long-run leak freedom or sustained frame rate. Evidence: `artifacts/final-review-20261026/`. The candidate APK SHA-256 is `54fc59987b709b36d1759b4e4332002ebef3e6f3a684e363c61ac2cff3e01333`. CI-built Release download, signature/digest checks, installation, Home verification, and requested final data clear follow publication.

Separately labeled emulator screenshots at default and 2x fonts showed readable header dates and single-line ellipsized app labels without header/button overlap. Large fonts naturally reduce visible rows. The emulator font scale was restored to 1.0; Sony settings were not changed.


### Final UI adjustment before tagging

At the user's request, increased tile labels to 16sp with 24sp line height, kept Normal/Medium weights and the existing 25sp/5sp baseline padding, and raised English/lunar date opacity to 75%/60%. Sony screenshots and UI bounds (`sony-label-16.*` and `sony-label-16-row3.*`) confirmed readable resting labels and focused third-row labels without tile/label overlap; long names remain single-line ellipsized. Reran Release, all six JVM tests, all 21 TV emulator tests, and Lint successfully. Added Git text attributes to normalize source line endings while keeping Windows wrapper checkout as CRLF. This supersedes the earlier candidate digest: final pre-tag APK SHA-256 `900d639d9d83e54262ef843428675d0f1c2892800f0e56cc94ee03394811bb02`.


### First remote release attempt: SDK manager path

Run 40325571862 reached checkout, Java 27, Gradle setup, and signing-key restoration, then failed with `sdkmanager: command not found` before building any APK. GitHub's runner-image documentation confirms that command-line tools are installed under `$ANDROID_HOME/cmdline-tools/latest/bin` but not added to PATH. Updated the SDK step to invoke that installed executable directly, without adding another action or changing SDK versions. The initial assetless Release/tag created during this task is replaced with the corrected commit before republishing v1.0.0.
