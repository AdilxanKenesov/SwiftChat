package uz.relay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import uz.relay.data.outbox.OutboxScheduler
import uz.relay.data.realtime.RealtimeCoordinator
import javax.inject.Inject

/**
 * Hilt'ning ildiz komponenti shu yerda yaratiladi (SingletonComponent).
 *
 * `Configuration.Provider`: WorkManager worker'larni Hilt factory orqali yaratishi uchun (OutboxWorker
 * konstruktoriga OutboxSender inject qilinadi). Buning uchun manifest'da avtomatik initializer o'chirilgan.
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var realtimeCoordinator: RealtimeCoordinator

    @Inject
    lateinit var outboxScheduler: OutboxScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Oldingi sessiyadan yuborilmay qolgan xabarlar bo'lsa — navbatga qo'yamiz.
        outboxScheduler.schedule()
        // WebSocket: login qilingan va ilova old planda bo'lganda o'zi ulanadi, fonda uziladi.
        realtimeCoordinator.start()
    }
}
