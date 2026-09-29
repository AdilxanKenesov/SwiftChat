package uz.relay.data.mapper

import uz.relay.data.model.response.TokenPairResponse
import uz.relay.data.source.local.Session

/**
 * Auth javobidagi token juftligini lokal [Session]ga aylantiradi.
 *
 * Nega alohida model: network DTO serverning JSON shakliga bog'langan, [Session] esa shifrlangan
 * storage'da saqlanadigan ichki model — server maydon qo'shsa ham saqlash formati o'zgarmaydi.
 * AuthRepositoryImpl (OTP tasdiqlash) va token yangilash oqimi chaqiradi.
 */
fun TokenPairResponse.toSession() = Session(
    accessToken = accessToken,
    refreshToken = refreshToken,
    userId = userId,
    deviceId = deviceId
)
