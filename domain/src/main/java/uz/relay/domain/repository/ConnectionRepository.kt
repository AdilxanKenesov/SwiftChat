package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ConnectionStatus

/**
 * Ulanish holati manbai (internet, WebSocket, sinxronlash birlashtirilgan).
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface ConnectionRepository {
    /** Internet + WebSocket + sync holatidan yig'ilgan yagona holat. */
    val status: Flow<ConnectionStatus>
}
