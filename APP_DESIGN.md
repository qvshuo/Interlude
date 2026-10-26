# Interlude 1.0.0 design and usage

Interlude is a single-screen Android TV launcher for checking the time, finding apps, and opening them. It has no advertising, recommendations, accounts, news, themes, manual sorting, Dock, screensaver, or automatic launch of other apps. Most of the screen belongs to apps.

## Layout

The upper left shows the clock, Gregorian date, and Chinese lunar date. Inputs, Network, and Settings sit at the upper right. A five-column app grid sits below the header. Each app name appears once below its tile, on one line with ellipsis when necessary. There is no large selected-app title or reserved title area.

Moving down from a header button returns to the last selected app. When there are no displayed apps, the app area remains empty and Settings remains reachable. Initial loading is also blank, with no placeholder or loading text.

## Appearance and typography

The background is black with white and gray text. A soft radial glow follows the selected artwork's accent color, transitioning over 650ms; its main opacity is 28%, fading through 8% to transparent.

App tiles use a 12dp rounded shape. Focus scales a tile to 1.1, lifts it by 3dp over 180ms, and draws a 1dp white border at 95% opacity directly along the same shape, without a gap. Unfocused tiles use a 1dp white border at 7% opacity. Tile names use Normal (400) weight and the main text color at 80% opacity when unfocused, and Medium (500) weight with full opacity when focused. Color transitions over 150ms; font size stays unchanged. Header buttons retain their circular shape, scale to 1.05, and use a 1dp white focus border at 50% opacity. Buttons are 40dp with 20dp icons and 10dp spacing.

The page has 48dp horizontal padding and 32dp top padding. The grid uses 18dp horizontal and vertical spacing and 10dp horizontal content padding. A 10dp focus inset and a 52dp fade region form the grid's 62dp top padding directly below the header. This leaves room for focus rendering and fading without a separate spacer. The 52dp fade mask is always present, with no scroll threshold or fade-in animation. It ends 10dp above the original first-row edge, leaving the focus inset clear so resting tiles remain fully opaque even when scaled and lifted.

The clock uses bundled Inter Light at 48sp with a 60sp line height and -1.5sp tracking. Its OpenType tabular-number and case-sensitive punctuation features keep digits stable and center the colon without a manual baseline offset. The English date uses Inter Regular at 14sp, 20sp line height, 1.25sp tracking, and 75% white. Only these two unmodified font weights are bundled; no italic, variable font, or additional script font is included.

App names, fallback initials, and the Chinese lunar date use the Android system font without an explicit font family. The lunar date uses 14sp, 20sp line height, 1sp tracking, and 60% white. Date lines have a 4dp gap. The clock and date group are vertically centered; the 1dp by 26dp separator has 16dp on either side. Font padding is disabled for clock/date text using the standard Compose text setting. Tile names use the system font at 16sp with a 24sp line height, centered line-height distribution, no extra font padding, and standard baseline padding of 25sp above / 5sp below; fallback initials use 36sp with a 44sp line height.

All sizes follow native Android sp scaling; there is no density or font-scale override. Screenshot comparisons use the default system font settings. Separate large-font checks are stress tests.

## Clock and calendar

Time uses 24-hour `HH:mm` without seconds. The Gregorian line shows an uppercase English weekday and date, such as `MONDAY 26 OCTOBER`. The lunar line retains traditional Chinese sexagenary-year, month, day, and leap-month notation. It handles lunar new year, month lengths, and leap months.

Time and date follow the system clock and time zone. Updates run once per minute while the home screen is STARTED and pause when it leaves that lifecycle state. Returning updates the display.

## App discovery, artwork, and sorting

The launcher discovers launchable apps, preferring TV entries over ordinary entries, deduplicating by package name, and excluding itself. It refreshes on resume and package changes while visible; it does not poll.

Landscape TV banners fill the tile using proportional cropping. Square or portrait artwork declared as a banner is displayed like an ordinary icon, with padding and proportional fitting. Artwork retains its original aspect ratio when reduced to a thumbnail. Missing or unreadable artwork does not remove a launchable app: the first character of its name is used instead. Artwork is read on IO threads, limited to 384x216, reused by component and package update time, and invalidated on configuration changes.

Apps opened from Interlude sort by most recent successful launch. For unused apps (or equal timestamps), package priority comes first: `com.gitvdemo.video` (银河奇异果), `com.cibn.tv` (酷喵), `com.ktcp.video` (云视听极光), then `com.hunantv.market` (芒果TV). Remaining names beginning with a Chinese character sort by Chinese pinyin collation, followed by other names in case-insensitive alphabetical order. Package identity resolves equal names deterministically. This is local launch history, not system-wide usage history. Hidden apps use the same ordering. Clock rollback and rapid launches cannot reverse recorded recency.

## Navigation and focus

Use the D-pad to move focus and short-press OK to open. Initial focus goes to the first visible app, or Settings if none exists. Moving up from the first app row reaches Inputs; moving down from a header button returns to the last selected app.

The list scrolls with focus. Scrolled-out tiles fade only above the first row's original top edge. Focus follows app identity across refreshes and reordering. If an app disappears, focus moves to an available app or Settings.

Home refreshes ordering and selects the first displayed app. It does not close an already expanded hidden section. Back preserves focus, scroll position, and hidden-section expansion, consumes the event without exiting, and does not manipulate Toasts. Actual Home delivery depends on the TV's default-launcher settings and must be checked with its Home key.

## Hiding and restoring

Hold OK on a visible app to hide it immediately, without a menu or confirmation. The app remains installed. Focus moves to the next visible app, then the previous app if necessary, or Settings when none remain. Remaining repeats and key-up from that long press cannot open or hide the next app.

Hold Settings to expand or collapse hidden apps. Hidden apps begin on a new row after normal apps, even if the preceding row is incomplete. Their artwork is grayscale; their names remain readable. Short-press still opens them without restoring them. Hold OK to restore an app; focus follows it back to the normal section according to the same sorting rules. Restoring the last hidden app ends the hidden section.

With no hidden apps, holding Settings only shows a native short Toast. Expansion is temporary and resets on process restart. Back does not collapse it.

Project-owned action messages and spoken descriptions are English; app labels and the lunar calendar keep their original language. Toasts use `Hidden: <app>`, `Restored: <app>`, `Hidden apps shown`, `Hidden apps collapsed`, and `No hidden apps`. Header descriptions are Inputs, Network, and Settings; tiles use `Open <app>`.

Hiding, restoring, expanding, and collapsing use Android's native short Toasts, dismissed by the system. A subsequent operation replaces the prior Toast; Back neither replaces nor retriggers it. The last app uses the same wording as any other app. Opening failures are silent; failed app launches refresh the catalog and do not record successful use.

## System buttons

- Inputs calls only `com.android.tv.action.VIEW_INPUTS`. There is no fallback to an input configuration page.
- Network opens system network settings, falling back to general settings when unavailable. Ethernet uses its own icon; otherwise Wi-Fi is shown. Disconnection dims the icon. There is no generated network-status text or Internet validation.
- Settings opens system settings on short press and manages hidden-section expansion on long press.

System pages belong to the TV and vary by manufacturer. Returning preserves the relevant header focus. Icon buttons and app tiles retain short spoken descriptions that tests can reuse; no extra accessibility layer is added.

## Persistence, lifecycle, and permissions

`apps` SharedPreferences stores hidden package names in `hidden` and launch timestamps in `used:<package>`. Writes use asynchronous apply. Legacy component-name keys migrate to package identities; app updates do not reset records. Uninstalled packages are removed from local records. Clearing app data removes this history. There is no account sync, database, or network backup.

The Activity handles system events and lifecycle, the ViewModel owns home state, AppCatalog owns discovery and records, and HomeScreen renders the page. Scans are serialized and cancellable; images are bounded and reused. No permanent service, app/network polling, Internet permission, usage-access permission, or accessibility permission is required. The stock launcher stays enabled.

## Licensing

Interlude and the reused CleanYourTV icons are MIT licensed; see `LICENSE`. The two bundled Inter 4.1 font files are licensed separately under SIL Open Font License 1.1; their full notice is packaged in `app/src/main/assets/INTER-LICENSE.txt`.

## App identity

The launcher mark shows two open stage curtains surrounding a quiet central opening, referring to an interlude between viewing sessions. The identity uses pure white on black. Android uses an adaptive icon with the mark inside its safe area; the system supplies the outer mask. The 16:9 TV banner pairs the same mark with an outlined Interlude wordmark. Both use native vector resources; no additional font files are bundled.

The Play Store asset is a full-square, opaque 512x512 sRGB PNG without a baked shadow or rounded corners. Google Play applies its own corner mask (currently 30% of the icon size). A separate rounded preview illustrates that treatment and is not the upload asset.
