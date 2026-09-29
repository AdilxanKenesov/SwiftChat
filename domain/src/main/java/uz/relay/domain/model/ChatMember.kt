package uz.relay.domain.model

/** Guruhdagi rol. Serverdan String bo'lib keladi — noma'lum (kelajakdagi) qiymat [UNKNOWN]. */
enum class MemberRole { OWNER, ADMIN, MEMBER, UNKNOWN }

/** Guruhning bitta a'zosi — a'zolar ro'yxati va ruxsatlarni hisoblash uchun. */
data class ChatMember(
    val userId: String,
    /** Profil hali keshda bo'lmasa `null`. */
    val displayName: String?,
    val role: MemberRole,
    val online: Boolean,
    val lastSeenAt: Long?,
    val isMe: Boolean
)

/** Rolga bog'liq ruxsatlar — server qoidalari bilan bir xil (UI keraksiz tugmalarni ko'rsatmasligi uchun). */
object GroupPermissions {

    /** Nom o'zgartirish va a'zo qo'shish: ADMIN yoki OWNER. */
    fun canManage(myRole: MemberRole?): Boolean = myRole == MemberRole.OWNER || myRole == MemberRole.ADMIN

    /** Rolni o'zgartirish faqat OWNER'ga; egasining o'z roli esa o'zgarmaydi. */
    fun canChangeRole(myRole: MemberRole?, target: ChatMember): Boolean =
        myRole == MemberRole.OWNER && !target.isMe && target.role != MemberRole.OWNER

    /** OWNER egasidan boshqa har kimni, ADMIN esa faqat oddiy a'zolarni chiqara oladi. */
    fun canRemove(myRole: MemberRole?, target: ChatMember): Boolean = when {
        target.isMe || target.role == MemberRole.OWNER -> false
        myRole == MemberRole.OWNER -> true
        myRole == MemberRole.ADMIN -> target.role == MemberRole.MEMBER
        else -> false
    }

    /** Boshqaning xabarini o'chirish: guruhda OWNER/ADMIN (server qoidasi). */
    fun canDeleteOthersMessages(myRole: MemberRole?): Boolean = canManage(myRole)
}
