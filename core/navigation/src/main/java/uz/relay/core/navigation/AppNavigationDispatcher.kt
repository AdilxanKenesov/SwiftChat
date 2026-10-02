package uz.relay.core.navigation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigatsiya "event bus"i: ViewModel'lar buyruqni shu yerga yuboradi, app modulidagi AppNavHost esa
 * uni o'qib back stack'ga qo'llaydi. [AppNavigator] (yuboruvchi) va [AppNavigationHandler] (qabul qiluvchi)
 * ikkalasi ham shu bitta @Singleton obyekt.
 *
 * Nega event bus: ViewModel NavController/back stack'ni ushlamaydi (xotira sizishi va Activity umriga
 * bog'lanish yo'q), har ekran faqat o'z Directions interfeysi orqali "qayerga" deydi (Uzum_Bank_Cloning
 * uslubi), va ViewModel'ni test'da fake navigator bilan tekshirish oson.
 *
 * Navigatsiya holat emas, hodisa: Channel har bir buyruqni aniq bir marta yetkazadi, shuning uchun ekran
 * burilganda (rotation) qayta bajarilmaydi. Bufer UI yig'ishni boshlashidan oldin yuborilgan buyruqlarni saqlaydi.
 */
@Singleton
class AppNavigationDispatcher @Inject constructor() : AppNavigator, AppNavigationHandler {

    // 16 ta buyruqdan oshsa eng eskisi tashlanadi — send() hech qachon osilib qolmaydi.
    private val channel = Channel<AppNavigationParam>(capacity = 16, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val params: Flow<AppNavigationParam> = channel.receiveAsFlow()

    override suspend fun navigate(param: AppNavigationParam) {
        channel.send(param)
    }
}
