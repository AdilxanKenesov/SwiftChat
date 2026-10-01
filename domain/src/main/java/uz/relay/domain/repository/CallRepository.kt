package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult

/**
 * Audio/video qo'ng'iroqlar. Relay'da qo'ng'iroq, signaling yoki qo'ng'iroq push'i yo'q (API o'zgarmaydi), shuning
 * uchun media va jiringlash tashqi xizmat — Stream Video orqali. Interfeys domain'da: Stream tiplari feature/domain'ga
 * chiqmaydi, provayder almashsa faqat data qatlami o'zgaradi.
 *
 * Identifikator: Stream'dagi foydalanuvchi id'si = Relay `userId` — alohida ro'yxatdan o'tish yo'q.
 */
interface CallRepository {

    /**
     * Shu odamga qo'ng'iroq qiladi (jiringlatadi). Qiymat — qo'ng'iroq id'si: qo'ng'iroq ekrani shu id bilan ochiladi.
     * Suhbatdosh qabul qilsa, SDK qo'ng'iroq qiluvchini o'zi ulaydi.
     */
    suspend fun startCall(peerUserId: String, video: Boolean): AppResult<String>

    /**
     * Kiruvchi (hali qabul qilinmagan) qo'ng'iroqlar id'lari. Hozircha faqat ilova ochiq paytda keladi (Stream WebSocket);
     * FCM ulangach yopiq ilovada ham push orqali keladi.
     */
    fun observeIncomingCalls(): Flow<String>
}
