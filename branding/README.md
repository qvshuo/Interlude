# Interlude curtain identity

White open curtains on black suggest a quiet interval between viewing sessions. The Android adaptive mark and TV wordmark banner share this silhouette. These assets were drawn as native vectors; no image-generation model was used. The wordmark uses outlines from the bundled, unmodified Inter 4.1 Regular font under the [SIL Open Font License](../app/src/main/assets/INTER-LICENSE.txt); it adds no font files or runtime text rendering.

- `play-store-icon.png`: 512x512, opaque, sRGB, full square for upload.
- `play-store-rounded-preview.png`: transparent outside the 30% corner mask; preview only.
- `android-icon.png`: adaptive foreground placement on its black background; preview only, device launchers supply their masks.
- `tv-banner.png`: 1280x720 preview of the scalable 16:9 banner.

Production files: `app/src/main/res/drawable/ic_launcher.xml`, `drawable/ic_launcher_foreground.xml`, `drawable-anydpi/ic_launcher.xml`, `drawable/launcher_banner.xml`, and `values/colors.xml`.

Design brief: pure white on black; abstract open stage curtains, no pause symbol, no gradient, no shadow; preserve the central opening and ample adaptive-icon safe area. TV banner uses the same mark plus the name Interlude.

References: [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [TV app icon guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
