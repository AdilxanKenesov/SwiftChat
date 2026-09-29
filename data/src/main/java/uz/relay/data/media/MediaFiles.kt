package uz.relay.data.media

import android.content.Context
import coil3.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media fayllar papkalarini bitta joyda boshqaradi — yo'llar ilova bo'ylab tarqalib ketmasligi va
 * logout'da hammasini ishonchli tozalash mumkin bo'lishi uchun. [MediaPreparer], MediaRepositoryImpl
 * va AuthRepositoryImpl (logout) ishlatadi.
 *
 * Media fayllari qayerda turadi:
 *  - [outboxDir] — yuborilayotgan fayllarning nusxasi (filesDir: tizim uni o'zi o'chirmaydi, yuklash
 *    ilova qayta ochilganda davom etishi kerak);
 *  - [downloadsDir] — yuklab olingan hujjatlar (cacheDir: joy kam bo'lsa tizim tozalashi mumkin — qayta
 *    yuklab olinadi).
 */
@Singleton
class MediaFiles @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader
) {
    // Getter har safar `mkdirs()` qiladi: papka clearAll()dan keyin ham qayta yaratiladi.
    val outboxDir: File get() = File(context.filesDir, "media_outbox").apply { mkdirs() }

    /** Bitta media uchun alohida papka — yuklab olingan fayl asl nomi bilan saqlanadi, nomlar to'qnashmaydi. */
    fun downloadsDir(mediaId: String): File = File(context.cacheDir, "media/$mediaId").apply { mkdirs() }

    /**
     * Logout / boshqa hisobga kirish: oldingi hisobning fayllari va rasm keshi qolmasin (keyingi foydalanuvchi
     * ularni ko'rmasligi kerak).
     */
    fun clearAll() {
        File(context.filesDir, "media_outbox").deleteRecursively()
        File(context.cacheDir, "media").deleteRecursively()
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
