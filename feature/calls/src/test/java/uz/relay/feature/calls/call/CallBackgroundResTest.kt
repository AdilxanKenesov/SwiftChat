package uz.relay.feature.calls.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Orqa fon tanlovi: rasmsiz variantlarda resurs yo'q, rasmli variantlarning har biri o'z rasmiga ega. */
class CallBackgroundResTest {

    @Test
    fun `none and blur have no image`() {
        assertNull(CallBackground.NONE.imageResOrNull())
        assertNull(CallBackground.BLUR.imageResOrNull())
    }

    @Test
    fun `every image background has its own drawable`() {
        val images = CallBackground.entries.filter { it != CallBackground.NONE && it != CallBackground.BLUR }
        images.forEach { assertNotNull(it.name, it.imageResOrNull()) }
        assertEquals(images.size, images.map { it.imageRes() }.toSet().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `asking image of blur fails loudly`() {
        CallBackground.BLUR.imageRes()
    }
}
