# Interlude curtain identity

Pale open curtains on deep navy suggest a quiet interval between viewing sessions. The launcher artwork is packaged as bitmaps instead of vectors, and every packaged size is a lossless downscale of the original artwork without crop, recolour, or recompression.

`logo.png` is the transparent 256x256 mark used by the README. Everything the app needs is packaged under `app/src/main/res`: `mipmap-anydpi/ic_launcher.xml`, `mipmap-*/ic_launcher_foreground.png`, `mipmap-*/banner.png`, and `values/colors.xml`. The adaptive foreground keeps the mark's largest opaque radius inside the 66dp safe circle, so device launcher masks cannot clip it.

The app is sideloaded and is not published to any app store, so no store upload asset is kept.

References: [TV app icon guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines).
