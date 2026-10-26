package art.anjing.interlude

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import art.anjing.interlude.data.toArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtworkTest {
    @Test fun artworkPreservesSourceProportionsWithinThumbnailBounds() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        for ((width, height) in listOf(144 to 144, 600 to 200, 200 to 600, 1920 to 1080)) {
            for (banner in listOf(false, true)) {
                val source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    val artwork = BitmapDrawable(resources, source).toArtwork(banner)
                    assertTrue(artwork.width <= if (banner) 384 else 128)
                    assertTrue(artwork.height <= if (banner) 216 else 128)
                    if (width >= height) {
                        assertEquals(artwork.width.toDouble() * height / width, artwork.height.toDouble(), 0.5)
                    } else {
                        assertEquals(artwork.height.toDouble() * width / height, artwork.width.toDouble(), 0.5)
                    }
                    artwork.recycle()
                } finally { source.recycle() }
            }
        }
    }
}
