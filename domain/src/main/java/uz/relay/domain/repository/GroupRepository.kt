package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.MemberRole

/**
 * Guruh amallari: yaratish, a'zolar, rollar, nom va chiqish.
 *
 * Nega interface: domain toza Kotlin moduli (Android'ga bog'liq emas) va faqat shartnomani belgilaydi,
 * amalga oshirish esa `data` modulida (Retrofit + Room). Shunda feature modullar data'ni bilmaydi,
 * use case'larni fake repository bilan oson test qilish mumkin (clean architecture, dependency inversion).
 */
interface GroupRepository {

    /** Guruh a'zolari lokal bazadan (egasi, adminlar, keyin a'zolar). */
    fun observeMembers(chatId: String): Flow<List<ChatMember>>

    /**
     * A'zolar ro'yxatini yangilash. ADMIN/OWNER uchun serverdan to'liq ro'yxat olinadi; oddiy a'zoga server
     * ruxsat bermaydi — unda ro'yxat yuklangan tarixdagi SYSTEM xabarlardan tiklanadi.
     */
    suspend fun refreshMembers(chatId: String): AppResult<Unit>

    /** Yangi guruh; men OWNER bo'laman. Qiymat — yangi chat id'si. */
    suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String>

    /** A'zo qo'shish (ADMIN/OWNER). */
    suspend fun addMembers(chatId: String, userIds: List<String>): AppResult<Unit>

    /** A'zoni chiqarish — ruxsat [uz.relay.domain.model.GroupPermissions.canRemove] bilan bir xil. */
    suspend fun removeMember(chatId: String, userId: String): AppResult<Unit>

    /** Rolni o'zgartirish (faqat OWNER). */
    suspend fun changeRole(chatId: String, userId: String, role: MemberRole): AppResult<Unit>

    /** Guruh nomini o'zgartirish (ADMIN/OWNER). */
    suspend fun rename(chatId: String, title: String): AppResult<Unit>

    /** Guruhdan chiqish: muvaffaqiyatda chat lokal bazadan ham o'chiriladi. */
    suspend fun leave(chatId: String): AppResult<Unit>
}
