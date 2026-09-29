package uz.relay.data.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Outbox'ni fonda yuboradigan WorkManager ishi: [OutboxSender.flush]ni chaqiradi va natijani
 * WorkManager tiliga (success/retry) o'giradi. [OutboxScheduler] navbatga qo'yadi; @HiltWorker —
 * Hilt bog'liqliklarni (OutboxSender) worker'ga inject qilishi uchun.
 *
 * Nega WorkManager: foydalanuvchi internet yo'qligida xabar yozib, ilovani yopib qo'yishi mumkin. WorkManager
 * ishni process o'lganidan keyin ham eslab qoladi va internet paydo bo'lganda (NetworkType.CONNECTED sharti)
 * o'zi ishga tushiradi — xabar yo'qolmaydi.
 */
@HiltWorker
class OutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val outboxSender: OutboxSender
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (outboxSender.flush()) {
        FlushResult.DONE -> Result.success()
        // retry → WorkManager eksponensial backoff bilan qayta ishga tushiradi.
        FlushResult.RETRY_LATER -> Result.retry()
    }
}
