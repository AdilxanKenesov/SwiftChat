package uz.relay.data.source.network.realtime

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import uz.relay.data.model.response.UpdateResponse

/**
 * Klient → server frame'lari (AsyncAPI: /docs/asyncapi.yaml).
 *
 * Nega sealed + @SerialName: kotlinx.serialization sealed ierarxiyani yozganda avtomatik
 * `"type": "<SerialName>"` maydonini qo'shadi — protokoldagi frame turi aynan shu maydon. Shuning uchun
 * har bir frame'ga `type` ni qo'lda yozish shart emas (va xato yozib qo'yish imkoni ham yo'q).
 */
@Serializable
sealed interface ClientFrame {

    /** Har bir socket'ning BIRINCHI frame'i, ochilgandan keyin 10 soniya ichida. */
    @Serializable
    @SerialName("auth")
    data class Auth(
        val token: String,
        val deviceId: String,
        /** Oxirgi qo'llangan updateSeq. Server faqat ma'lumot sifatida oladi — undan replay qilmaydi. */
        val cursor: Long
    ) : ClientFrame

    /**
     * Xabar yuborish — REST `POST /v1/chats/{id}/messages` bilan bir xil servis. Javob: `ack` yoki `nack`.
     * `messageType` deb nomlangan, chunki `type` frame turi uchun band.
     */
    @Serializable
    @SerialName("send")
    data class Send(
        val clientMessageId: String,
        val chatId: String,
        val messageType: String,
        val body: String? = null,
        val mediaIds: List<String>? = null,
        val replyTo: String? = null
    ) : ClientFrame

    /** "Men shu serverSeq gacha o'qidim" (max-wins). */
    @Serializable
    @SerialName("read")
    data class Read(val chatId: String, val upToSeq: Long) : ClientFrame

    /** "Qurilmam shu serverSeq gacha xabarlarni oldi" — yuboruvchi ✓✓ (yetkazildi) ko'radi. */
    @Serializable
    @SerialName("received")
    data class Received(val chatId: String, val upToSeq: Long) : ClientFrame

    /** "Men yozyapman" — MessageRepositoryImpl yuboradi; faqat socket orqali (REST muqobili yo'q). */
    @Serializable
    @SerialName("typing")
    data class Typing(val chatId: String) : ClientFrame
}

/** Server → klient frame'lari. */
@Serializable
sealed interface ServerFrame {

    /**
     * Auth qabul qilindi. `updateSeq` socket jonli yetkazish uchun ro'yxatga olingandan KEYIN o'qiladi:
     * undan kattalari socket orqali keladi, unga qadar bo'lganlari esa REST'da bor.
     */
    @Serializable
    @SerialName("auth_ok")
    data class AuthOk(val userId: String, val updateSeq: Long) : ServerFrame

    /** `send` qabul qilindi: server id/seq/vaqt — outbox xabarni SENT deb belgilaydi. */
    @Serializable
    @SerialName("ack")
    data class Ack(
        val clientMessageId: String,
        val serverId: Long,
        val serverSeq: Long,
        val serverCreatedAt: Long
    ) : ServerFrame

    /** `send` rad etildi. `retryable = true` bo'lsa keyinroq qayta urinish mumkin, aks holda xabar FAILED. */
    @Serializable
    @SerialName("nack")
    data class Nack(
        val clientMessageId: String,
        val code: String,
        val message: String = "",
        val retryable: Boolean = false
    ) : ServerFrame

    /** `GET /v1/updates` dagi bitta qator bilan aynan bir xil — ikkalasini bitta kod qo'llaydi. */
    @Serializable
    @SerialName("update")
    data class Update(val updateSeq: Long, val kind: String, val payload: JsonObject) : ServerFrame {
        fun toUpdateResponse() = UpdateResponse(updateSeq = updateSeq, kind = kind, payload = payload)
    }

    /** Boshqa a'zo shu chatda yozyapti. */
    @Serializable
    @SerialName("typing")
    data class Typing(val chatId: String, val userId: String) : ServerFrame

    /** Umumiy chatdagi odamning online holati o'zgardi (server 5 s debounce qiladi). */
    @Serializable
    @SerialName("presence")
    data class Presence(val userId: String, val online: Boolean, val lastSeenAt: Long? = null) : ServerFrame

    /** Frame qayta ishlanmadi, lekin ulanish ochiq qoladi. */
    @Serializable
    @SerialName("error")
    data class Error(val code: String, val message: String = "") : ServerFrame
}

/**
 * Serverdan kelgan matnni frame'ga aylantiradi. Noma'lum `type` yoki buzuq JSON → `null`:
 * protokolga yangi frame turlari qo'shilishi mumkin (Changelog) — ilova ularni jimgina o'tkazib yuboradi.
 */
internal fun Json.decodeServerFrame(text: String): ServerFrame? = try {
    decodeFromString(ServerFrame.serializer(), text)
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}

/** Klient frame'ini JSON matnga aylantiradi (`type` maydoni avtomatik qo'shiladi). */
internal fun Json.encodeClientFrame(frame: ClientFrame): String = encodeToString(ClientFrame.serializer(), frame)
