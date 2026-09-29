package uz.relay.data.outbox

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutboxScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Unique work + APPEND_OR_REPLACE: flush'lar hech qachon parallel ketmaydi, lekin ish ketayotgan paytda
     * qo'shilgan xabar ham e'tibordan chetda qolmaydi — navbatdagi ish uni oladi. (KEEP bo'lsa, flush
     * "bo'sh" deb tugayotgan lahzada qo'shilgan xabar keyingi safargacha osilib qolardi.)
     */
    fun schedule() {
        val request = OneTimeWorkRequestBuilder<OutboxWorker>()
            .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private companion object {
        const val UNIQUE_NAME = "outbox"
    }
}
