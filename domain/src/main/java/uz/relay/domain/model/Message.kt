package uz.relay.domain.model

/** Suhbat ekranidagi bitta xabar. */
data class Message(
    /** Qurilmada yaratilgan UUID — xabarning doimiy identifikatori (ro'yxat kaliti ham shu). */
    val clientMessageId: String,
    /** Server qabul qilgandan keyin paydo bo'ladi; tahrir/o'chirish so'rovlari shu bilan yuboriladi. */
    val serverId: Long?,
    val chatId: String,
    val senderId: String,
    val isMine: Boolean,
    /** Chat ichidagi tartib raqami; hali yuborilmagan xabarda `null`. */
    val serverSeq: Long?,
    val type: MessageType,
    /** TEXT uchun matn, media uchun izoh. SYSTEM uchun `null` — [systemEvent] ga qarang. */
    val text: String?,
    val systemEvent: SystemEvent?,
    /** Javob berilgan xabarning clientMessageId'si. */
    val replyToClientMessageId: String?,
    /** Server vaqti; yuborilmagan xabar uchun — lokal yaratilgan vaqt. */
    val createdAt: Long,
    val isEdited: Boolean,
    /** O'chirilgan xabar tombstone bo'lib qoladi: "Xabar oʻchirildi", joyi saqlanadi. */
    val isDeleted: Boolean,
    /** Faqat o'zimning xabarim uchun ma'noli. */
    val status: MessageStatus
)
