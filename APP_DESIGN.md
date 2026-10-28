# Interlude 1.0.2 design and usage

Interlude is a single-screen Android TV launcher for checking the time, finding apps, and opening them. It has no advertising, recommendations, accounts, news, themes, manual sorting, Dock, screensaver, or automatic launch of other apps. Most of the screen belongs to apps.

## Layout

The upper left shows the clock, Gregorian date, and Chinese lunar date. Inputs, Network, and Settings sit at the upper right. A five-column app grid sits below the header. Each app name appears once below its tile, on one line with ellipsis when necessary. There is no large selected-app title or reserved title area.

Moving down from a header button returns to the last selected app. When there are no displayed apps, the app area remains empty and Settings remains reachable. Initial loading is also blank, with no placeholder or loading text.

## Appearance and typography

The background is black with white and gray text. A soft radial glow follows the selected artwork's accent color, transitioning over 650ms; its main opacity is 28%, fading through 8% to transparent.

App tiles use 16dp continuous corners with 0.6 smoothing. Focus scales a tile to 1.1, lifts it by 3dp, and draws a 1dp white border at 95% opacity directly along the same outline, without a gap. At rest, the glass rim catches light at the upper left and lower right, with dimmer sides. A fine 0.5dp inner shade keeps the edge readable over light banners. A restrained upper-left sheen and translucent dark substrate give tiles and header buttons a glass finish. Artwork stays sharp, and labels remain outside the glass. There is no live blur, refraction, wallpaper, or continuous shimmer. Tile names keep Normal (400) weight in both states, avoiding a change in glyph width; their opacity transitions from 78% to full white without a size change. Glass, lift, and label emphasis match the configured native TV Surface scale: 300ms on focus, 500ms on unfocus, using cubic-bezier (0, 0, 0.2, 1). Native pressed/released scaling remains under TV Surface control. Header buttons retain their circular shape, scale to 1.05, and use a 1dp white focus border at 65% opacity. Buttons are 40dp with 20dp icons and 10dp spacing.

The glass finish combines [CleanYourTV](https://github.com/Gathin23/cleanyourtv/tree/main/launcher)'s restrained sheen and rim treatment with [Aerial](https://github.com/pengchujin/aerial-tv)'s continuous-corner construction. Antialiased masks and rims are rasterized once per rendered control size and held by the drawing cache; focus only changes compositing strength. Native TV surfaces still handle focus, scaling, clicks, and long presses. No glass settings, theme framework, extra dependencies, or background work are added.

The page has 48dp horizontal padding and 32dp top padding. The grid uses 18dp horizontal and vertical spacing and 10dp horizontal content padding. A 10dp focus inset and a 52dp fade region form the grid's 62dp top padding directly below the header. This leaves room for focus rendering and fading without a separate spacer. The 52dp fade mask is always present, with no scroll threshold or fade-in animation. It ends 10dp above the original first-row edge, leaving the focus inset clear so resting tiles remain fully opaque even when scaled and lifted.

The clock uses bundled Inter Light at 48sp with a 60sp line height and -1.5sp tracking. Its OpenType tabular-number and case-sensitive punctuation features keep digits stable and center the colon without a manual baseline offset. The English date uses Inter Regular at 14sp, 20sp line height, 1sp tracking, and 75% white. Only these two unmodified font weights are bundled; no italic, variable font, or additional script font is included.

App names, fallback initials, and the Chinese lunar date use the Android system font without an explicit font family. The lunar date uses 14sp, 20sp line height, 0.5sp tracking, and 60% white. Date lines have a 4dp gap. The clock and date group are vertically centered; the 1dp by 26dp separator uses 16% white and has 16dp on either side. Font padding is disabled for clock/date text using the standard Compose text setting. Tile names use the system font at 16sp with a 24sp line height, centered line-height distribution, no extra font padding, and standard baseline padding of 25sp above / 5sp below; fallback initials use 36sp with a 44sp line height.

All sizes follow native Android sp scaling; there is no density or font-scale override. Screenshot comparisons use the default system font settings. Separate large-font checks are stress tests.

## Clock and calendar

Time uses 24-hour `HH:mm` without seconds. The Gregorian line shows an uppercase English weekday and date, such as `MONDAY 26 OCTOBER`. The lunar line retains traditional Chinese sexagenary-year, month, day, and leap-month notation. It handles lunar new year, month lengths, and leap months.

Time and date follow the system clock and time zone. Updates run once per minute while the home screen is STARTED and pause when it leaves that lifecycle state. Returning updates the display.

## App discovery, artwork, and sorting

The launcher discovers launchable apps, preferring TV entries over ordinary entries, deduplicating by package name, and excluding itself. On resume it checks the system's package-change sequence and reuses the catalog when apps and configuration are unchanged, while still updating local launch ordering. Package changes while visible and failed launches force a scan. Unreadable artwork is retried on the next refresh; it does not poll.

Landscape TV banners fill the tile using proportional cropping. Square or portrait artwork declared as a banner is displayed like an ordinary icon, with padding and proportional fitting. Artwork retains its original aspect ratio when reduced to a thumbnail. Missing or unreadable artwork does not remove a launchable app: the first character of its name is used instead. Artwork is read on IO threads, limited to 384x216, reused by component and package update time, and invalidated on configuration changes.

Apps opened from Interlude sort by most recent successful launch. For unused apps (or equal timestamps), package priority comes first: `com.gitvdemo.video` (银河奇异果), `com.cibn.tv` (酷喵), `com.ktcp.video` (云视听极光), then `com.hunantv.market` (芒果TV). Remaining names beginning with a Chinese character sort by Chinese pinyin collation, followed by other names in case-insensitive alphabetical order. Package identity resolves equal names deterministically. This is local launch history, not system-wide usage history. Apps removed from Home use the same ordering. Clock rollback and rapid launches cannot reverse recorded recency.

## Navigation and focus

Use the D-pad to move focus and short-press OK to open. Initial focus goes to the first visible app, or Settings if none exists. Moving up from the first app row reaches Inputs; moving down from a header button returns to the last selected app.

The list scrolls with focus. Scrolled-out tiles fade only above the first row's original top edge. Focus follows app identity across refreshes and reordering. If an app disappears, focus moves to an available app or Settings.

Home refreshes ordering and selects the first displayed app. It does not close an already expanded hidden section. Back preserves focus, scroll position, and expansion of all apps, consumes the event without exiting, and does not manipulate Toasts. Actual Home delivery depends on the TV's default-launcher settings and must be checked with its Home key.

## Home apps and all apps

Hold OK on a Home app to remove it from Home immediately, without a menu or confirmation. The app remains installed. Focus moves to the next Home app, then the previous app if necessary, or Settings when none remain. Remaining repeats and key-up from that long press cannot open or remove the next app.

Hold Settings to show all apps and focus the first app removed from Home in the existing sort order. Removed apps begin on a new row after Home apps, even if the preceding row is incomplete. Their artwork is grayscale; their names remain readable. Short-press still opens them without adding them back to Home. Hold OK to add one back; focus follows it to its position among Home apps. Adding the last removed app ends the expanded section.

Hold Settings again to return to Home apps and focus the first Home app, or Settings if Home is empty. Switching the displayed range is silent and does not cancel, replace, or retrigger any existing Toast. Explicit focus targets take priority over initial scrolling and transient focus events; an offscreen target is scrolled into layout before requesting focus.

When all apps are already on Home, holding Settings preserves the view and Settings focus and shows a native short Toast. Expansion is temporary and resets on process restart. Back preserves expansion, focus, and scroll position.

Project-owned action messages and spoken descriptions are English; app labels and the lunar calendar keep their original language. The only Toasts are `Removed from Home: <app>`, `Added to Home: <app>`, and `All apps are already on Home`. Header descriptions are Inputs, Network, and Settings; tiles use `Open <app>`.

Toasts are dismissed by Android. A subsequent operation that produces a Toast replaces the prior Toast; Back and range switching do not manipulate it. The last app uses the same wording as any other app. Opening failures are silent; failed app launches refresh the catalog and do not record successful use.

## System buttons

- Inputs calls only `com.android.tv.action.VIEW_INPUTS`. There is no fallback to an input configuration page.
- Network opens system network settings, falling back to general settings when unavailable. Ethernet uses its own icon; otherwise Wi-Fi is shown. Disconnection dims the icon. There is no generated network-status text or Internet validation.
- Settings opens system settings on short press and manages expansion of all apps on long press.

System pages belong to the TV and vary by manufacturer. Returning preserves the relevant header focus. Icon buttons and app tiles retain short spoken descriptions that tests can reuse; no extra accessibility layer is added.

## Persistence, lifecycle, and permissions

`apps` SharedPreferences stores hidden package names in `hidden` and launch timestamps in `used:<package>`. Writes use asynchronous apply. Legacy component-name keys migrate to package identities; app updates do not reset records. Uninstalled packages are removed from local records. Clearing app data removes this history. There is no account sync, database, or network backup.

The Activity handles system events and lifecycle, the ViewModel owns home state, AppCatalog owns discovery and records, and HomeScreen renders the page. Scans are serialized and cancellable; images are bounded and reused. No permanent service, app/network polling, Internet permission, usage-access permission, or accessibility permission is required. The stock launcher stays enabled.

## Licensing

Interlude, the reused CleanYourTV icons, and the continuous-corner construction adapted from Aerial are MIT licensed; see `LICENSE`, also bundled as `app/src/main/assets/LICENSE.txt`. The two bundled Inter 4.1 font files are licensed separately under SIL Open Font License 1.1; their full notice is packaged in `app/src/main/assets/INTER-LICENSE.txt`.

## App identity

The launcher mark shows two open stage curtains surrounding a quiet central opening, referring to an interlude between viewing sessions. The identity uses pure white on black. Android uses an adaptive icon with the mark inside its safe area; the system supplies the outer mask. The 16:9 TV banner pairs the same mark with an outlined Interlude wordmark. Both use native vector resources; no additional font files are bundled.

The Play Store asset is a full-square, opaque 512x512 sRGB PNG without a baked shadow or rounded corners. Google Play applies its own corner mask (currently 30% of the icon size). A separate rounded preview illustrates that treatment and is not the upload asset.
