package uz.relay.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import uz.relay.data.call.StreamVideoConnector
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
    /** Stream Video (qo'ng'iroqlar): login'da ulanadi, logout'da uziladi. */
    @Inject
    lateinit var streamVideoConnector: StreamVideoConnector

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
        enableStrictModeInDebug()
        // Oldingi sessiyadan yuborilmay qolgan xabarlar bo'lsa — navbatga qo'yamiz.
        outboxScheduler.schedule()
        // WebSocket: login qilingan va ilova old planda bo'lganda o'zi ulanadi, fonda uziladi.
        realtimeCoordinator.start()
        // Qo'ng'iroqlar: Stream client Relay sessiyasiga bog'lanadi (API key bo'lmasa hech narsa qilmaydi).
        streamVideoConnector.start()
    }

    /**
     * Faqat debug build'da: main thread'dagi disk/tarmoq ishlari va yopilmagan resurslar logcat'ga yoziladi
     * (`StrictMode` tegi bilan). "Ilova qotib ishlayapti" kabi muammolarning manbasini topish uchun —
     * ilovani to'xtatmaydi, faqat ogohlantiradi.
     */
    private fun enableStrictModeInDebug() {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }
}
