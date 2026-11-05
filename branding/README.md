# Interlude curtain identity

Pale open curtains on deep navy suggest a quiet interval between viewing sessions. The launcher artwork is packaged as bitmaps instead of vectors, and every packaged size is a lossless downscale of the original artwork without crop, recolour, or recompression.

Store and documentation images live here; anything the app itself needs is packaged under `app/src/main/res`:

- `logo.png`: 256x256 transparent mark for the README.
- `play-store-icon.png`: 512x512, opaque, sRGB, full square for store upload.

Android production resources: `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `mipmap-*/ic_launcher_foreground.png`, `mipmap-*/banner.png`, and `values/colors.xml`. The adaptive foreground keeps the mark's largest opaque radius inside the 66dp safe circle, so device launcher masks cannot clip it.

References: [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications), [TV app icon guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
