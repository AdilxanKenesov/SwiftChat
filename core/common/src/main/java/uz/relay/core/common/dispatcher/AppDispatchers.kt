package uz.relay.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Qualifier

/**
 * Coroutine dispatcher'lar to'plami (IO, Default, Main).
 *
 * Nega inject qilinadi: kod `Dispatchers.*` ni to'g'ridan-to'g'ri chaqirsa, test'da uni almashtirib bo'lmaydi.
 * Hilt orqali berilgani uchun test'da hammasi o'rniga bitta TestDispatcher qo'yiladi va vaqt boshqariladi
 * (deterministik test). Qiymatlari data modulidagi DI modulida beriladi.
 */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher
)

/**
 * Ilova umri davomida yashaydigan CoroutineScope uchun Hilt qualifier'i. Ekran yopilganda ham to'xtamasligi
 * kerak bo'lgan ishlar (sinxronlash, WebSocket, outbox) shu scope'da ishlaydi — viewModelScope'da emas.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
