package uz.relay.domain.model

/** Ekranda ko'rsatiladigan umumiy ulanish holati (spec, 4-bo'lim). Ustuvorlik tartibida. */
enum class ConnectionStatus {
    /** Qurilmada internet yo'q — "Internet aloqasi yoʻq" banneri. */
    OFFLINE,

    /** Bootstrap yoki `GET /v1/updates` catch-up ketmoqda — "Yangilanmoqda…". */
    UPDATING,

    /** WebSocket ulanmoqda yoki uzilgandan keyin qayta ulanmoqda — "Ulanmoqda…". */
    CONNECTING,

    /** Hammasi joyida — oddiy sarlavha (logo + nom). */
    CONNECTED
}
