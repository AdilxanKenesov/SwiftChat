package uz.relay.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import uz.relay.core.common.dispatcher.AppDispatchers
import uz.relay.core.common.dispatcher.ApplicationScope
import javax.inject.Singleton

/**
 * Coroutine dispatcher'lari va ilova darajasidagi [CoroutineScope] ni beradi.
 *
 * Nega: dispatcher'larni to'g'ridan-to'g'ri `Dispatchers.IO` deb yozish o'rniga [AppDispatchers] inject
 * qilinadi — testlarda ularni test dispatcher bilan almashtirish oson bo'ladi. [ApplicationScope] esa
 * ekran yopilganda ham davom etishi kerak bo'lgan ishlar (WebSocket, sync, receipt yuborish) uchun.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = AppDispatchers(
        io = Dispatchers.IO,
        default = Dispatchers.Default,
        main = Dispatchers.Main
    )

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(dispatchers: AppDispatchers): CoroutineScope =
        // SupervisorJob: bitta job xato bilan tugasa, qolganlari (masalan, WebSocket) bekor bo'lib ketmaydi.
        CoroutineScope(SupervisorJob() + dispatchers.default)
}
