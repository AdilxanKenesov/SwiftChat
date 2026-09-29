package uz.relay.domain.model

/**
 * Chat turi. Serverdan String bo'lib keladi; kontrakt faqat qo'shimcha o'zgaradi, ya'ni kelajakda
 * yangi tur qo'shilishi mumkin. Shunda ilova yiqilmasligi uchun noma'lum qiymat [UNKNOWN] bo'ladi.
 */
enum class ChatType { DIRECT, GROUP, UNKNOWN }

/** Xabar turi. Noma'lum (kelajakdagi) tur ham [UNKNOWN] bo'ladi — sababi [ChatType] dagidek. */
enum class MessageType { TEXT, IMAGE, VIDEO, FILE, SYSTEM, UNKNOWN }

/**
 * O'zim yuborgan xabarning holati (✓ belgilari uchun), spec 3.8:
 * - [SENDING] — outbox'da, server hali qabul qilmagan (soat belgisi);
 * - [SENT] — server qabul qildi, ack keldi (✓);
 * - [DELIVERED] — suhbatdoshning qurilmasiga yetib bordi (✓✓ kulrang);
 * - [READ] — suhbatdosh o'qidi (✓✓ rangli);
 * - [FAILED] — server qayta urinib bo'lmaydigan xato bilan rad etdi (qizil belgi + "qayta yuborish").
 */
enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

/** Chatlar ro'yxatidagi bitta qator uchun kerak bo'ladigan hamma narsa. */
data class ChatSummary(
    val id: String,
    val type: ChatType,
    /**
     * GROUP: guruh nomi. DIRECT: suhbatdoshning ismi — server DIRECT chat uchun `title` bermaydi,
     * ism profillar keshidan olinadi. Profil hali yuklanmagan bo'lsa `null`.
     */
    val title: String?,
    /** Faqat DIRECT chat uchun: suhbatdoshning id'si (avatar rangi va "online" belgisi uchun). */
    val peerUserId: String?,
    val peerOnline: Boolean,
    /** Suhbatdosh oxirgi marta qachon online bo'lgan (online paytda yoki noma'lum bo'lsa `null`). */
    val peerLastSeenAt: Long?,
    val lastMessage: LastMessage?,
    /** Ro'yxat shu vaqt bo'yicha tartiblanadi (eng yangisi tepada). */
    val lastActivityAt: Long,
    val unreadCount: Int,
    /**
     * Faqat MENING sozlamam: chat HOZIR ovozsizmi. Muddati o'tgan mute bu yerda allaqachon `false` —
     * server `muted = true` ni muddat tugagandan keyin ham qaytarishi mumkin, UI esa buni bilishi shart emas.
     */
    val muted: Boolean,
    /** Ovozsiz qachongacha (epoch ms). `null` — muddatsiz yoki ovozsiz emas. */
    val mutedUntil: Long? = null
)

/** Chatlar ro'yxatidagi qatorda ko'rinadigan oxirgi xabarning qisqa ko'rinishi. */
data class LastMessage(
    val serverId: Long,
    val senderId: String,
    /** Oxirgi xabarni o'zim yuborganmanmi — "Siz: ..." prefiksi va ✓ belgilari shunga bog'liq. */
    val isMine: Boolean,
    val type: MessageType,
    /** TEXT uchun matn, media uchun izoh (caption). SYSTEM uchun `null` — [systemEvent] ga qarang. */
    val text: String?,
    /** Faqat SYSTEM xabar uchun: nima bo'lgani ("guruh yaratildi", "a'zo qo'shildi"...). */
    val systemEvent: SystemEvent?,
    /** O'chirilgan xabar ro'yxatda "Xabar oʻchirildi" bo'lib ko'rinadi (tombstone). */
    val isDeleted: Boolean,
    val createdAt: Long,
    /** Faqat o'zimning xabarim uchun ma'noli. */
    val status: MessageStatus
)

/**
 * SYSTEM xabarning `body`si matn emas, JSON (API'dagi SystemMessageBody). Server tayyor gap bermaydi:
 * "Ali guruhni yaratdi" kabi matnni klient o'zi ismlar va o'z tilida tuzadi.
 */
data class SystemEvent(
    /** group_created | members_added | member_removed | member_left (kelajakda yangilari ham bo'lishi mumkin) */
    val event: String,
    val actorId: String,
    val targetUserIds: List<String>,
    /** Faqat group_created uchun. */
    val title: String?
)
