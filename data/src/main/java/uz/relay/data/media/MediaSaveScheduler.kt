package uz.relay.data.media

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.relay.domain.model.MessageMedia
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** [MediaSaveWorker]ni navbatga qo'yadi. */
@Singleton
class MediaSaveScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * - Unique + KEEP: bitta faylni ketma-ket ikki marta bosish ikkita yuklab olish boshlamaydi.
     * - Expedited: foydalanuvchi o'zi bosdi — ish darhol boshlanishi kerak (kvota tugasa oddiy ish sifatida).
     * - Internet sharti faqat fayl qurilmada yo'q bo'lsa (o'zim yuborgan rasm tarmoqsiz ham saqlanadi).
     */
    fun schedule(media: MessageMedia, fileName: String) {
        val hasLocalCopy = media.localPath?.let { File(it).exists() } == true
        val request = OneTimeWorkRequestBuilder<MediaSaveWorker>()
            .setInputData(MediaSaveWorker.inputData(media, fileName))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(Constraints(requiredNetworkType = if (hasLocalCopy) NetworkType.NOT_REQUIRED else NetworkType.CONNECTED))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("save_${media.mediaId ?: media.localPath}", ExistingWorkPolicy.KEEP, request)
    }
}
