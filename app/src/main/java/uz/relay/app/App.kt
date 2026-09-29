package uz.relay.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
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
class App : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var realtimeCoordinator: RealtimeCoordinator

    @Inject
    lateinit var outboxScheduler: OutboxScheduler

    /** Lazy: Coil birinchi rasmni chizgandagina yaratiladi (ilova ishga tushishini sekinlashtirmaydi). */
    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    /**
     * Hamma `AsyncImage`lar shu ImageLoader'dan foydalanadi: u media OkHttp klienti ustida, ya'ni
     * `GET /v1/media/{id}` so'roviga token qo'shiladi va eskirgan token o'zi yangilanadi.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()

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
