package uz.relay.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import uz.relay.data.realtime.RealtimeCoordinator
import javax.inject.Inject

/** Hilt'ning ildiz komponenti shu yerda yaratiladi (SingletonComponent). */
@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var realtimeCoordinator: RealtimeCoordinator

    override fun onCreate() {
        super.onCreate()
        // WebSocket: login qilingan va ilova old planda bo'lganda o'zi ulanadi, fonda uziladi.
        realtimeCoordinator.start()
    }
}
