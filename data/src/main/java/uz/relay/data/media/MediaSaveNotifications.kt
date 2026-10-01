package uz.relay.data.media

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.relay.data.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Galereyaga saqlash bildirishnomalari:
 *  - jarayon paytida — foreground service'ning doimiy (o'chirib bo'lmaydigan) bildirishnomasi, progress bilan;
 *  - tugaganda — "saqlandi" (bosilsa rasm/video ochiladi) yoki "saqlab bo'lmadi".
 *
 * Nega alohida kanal: foydalanuvchi telefon sozlamalarida faqat "Yuklab olishlar"ni o'chirib qo'yishi mumkin
 * (keyinchalik keladigan xabar push'lariga tegmasdan). Muhimlik LOW — progress ovoz chiqarib bezovta qilmasin.
 */
@Singleton
class MediaSaveNotifications @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Worker'ning foreground ma'lumoti. [progress] `null` — hajm hali noma'lum (cheksiz chiziq). */
    fun foregroundInfo(notificationId: Int, isVideo: Boolean, fileName: String, progress: Int?): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(context.getString(if (isVideo) R.string.media_save_video else R.string.media_save_image))
            .setContentText(fileName)
            .setProgress(100, progress ?: 0, progress == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        // Android 10+ da tur ko'rsatiladi (manifest'dagi dataSync bilan bir xil bo'lishi shart).
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    /**
     * Foreground bildirishnoma servis to'xtashi bilan yo'qoladi — shuning uchun natija ALOHIDA id bilan
     * ko'rsatiladi. Bosilganda galereyadagi fayl mos ilovada ochiladi.
     */
    fun showDone(notificationId: Int, isVideo: Boolean, uri: Uri, mimeType: String) {
        val open = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeType)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(context, notificationId, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        notify(
            notificationId,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(context.getString(if (isVideo) R.string.media_save_done_video else R.string.media_save_done_image))
                .setContentText(context.getString(R.string.media_save_done_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    fun showFailed(notificationId: Int) {
        notify(
            notificationId,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(context.getString(R.string.media_save_failed))
                .setContentText(context.getString(R.string.media_save_failed_text))
                .setAutoCancel(true)
                .build()
        )
    }

    /** Android 13+ da ruxsat berilmagan bo'lsa jim o'tkaziladi — saqlashning o'zi baribir bajariladi. */
    private fun notify(id: Int, notification: android.app.Notification) {
        ensureChannel()
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) NotificationManagerCompat.from(context).notify(id, notification)
    }

    /** Kanal bir marta yaratiladi (qayta chaqirish zararsiz). Nomi joriy tilda — til almashsa keyingi chaqiruvda yangilanadi. */
    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.media_save_channel), NotificationManager.IMPORTANCE_LOW)
            .apply { description = context.getString(R.string.media_save_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "media_downloads"
    }
}
