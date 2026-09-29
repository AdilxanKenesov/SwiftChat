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
 * Ilovaning Application klassi — hamma narsa shu yerdan boshlanadi.
 *
 * `@HiltAndroidApp`: Hilt'ning ildiz komponenti (SingletonComponent) shu yerda yaratiladi; ilovadagi
 * barcha @Singleton obyektlar (repository'lar, Retrofit, Room, navigator) shu komponentda yashaydi.
 *
 * `Configuration.Provider`: WorkManager worker'larni Hilt factory orqali yaratishi uchun (OutboxWorker
 * konstruktoriga OutboxSender inject qilinadi). Buning uchun manifest'da avtomatik initializer o'chirilgan.
 *
 * `SingletonImageLoader.Factory`: Coil'ga bitta umumiy ImageLoader beriladi (pastda [newImageLoader]).
 */
@HiltAndroidApp
class App : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    /** Worker'larga Hilt orqali bog'liqliklarni beradigan factory. */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** WebSocket ulanishini ilova holatiga (login, old/orqa plan) qarab boshqaradi. */
    @Inject
    lateinit var realtimeCoordinator: RealtimeCoordinator

    /** Yuborilmagan xabarlar navbatini (outbox) WorkManager'ga qo'yadi. */
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

    // WorkManager birinchi ishlatilganda shu konfiguratsiyani oladi (on-demand initialization).
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
