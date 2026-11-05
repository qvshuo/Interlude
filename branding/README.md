# Interlude curtain identity

Pale open curtains on deep navy suggest a quiet interval between viewing sessions. The Android adaptive mark and TV banner share this silhouette and use only the supplied artwork, without vector redraws or added text.

- `play-store-icon.png`: 512x512, opaque, sRGB, full square for upload.
- `play-store-rounded-preview.png`: transparent outside the 30% corner mask; preview only.
- `android-icon.png`: adaptive foreground placement on the banner background; preview only, device launchers supply their masks.
- `tv-banner.png`: 1280x720 preview of the 16:9 banner.

Production files: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `mipmap-*/ic_launcher_foreground.png`, `mipmap-*/banner.png`, and `values/colors.xml`. The source PNGs are not stored in the repository; the packaged bitmaps are only downscaled copies, and no crop, recolour, or recompression of the artwork was applied.

Design brief: the supplied mark keeps its central opening and an adaptive-icon safe area margin. The TV banner keeps its original 16:9 composition without cropping.

References: [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [TV app icon guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
