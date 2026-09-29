package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.MemberRole

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

    suspend fun addMembers(chatId: String, userIds: List<String>): AppResult<Unit>

    suspend fun removeMember(chatId: String, userId: String): AppResult<Unit>

    suspend fun changeRole(chatId: String, userId: String, role: MemberRole): AppResult<Unit>

    suspend fun rename(chatId: String, title: String): AppResult<Unit>

    /** Guruhdan chiqish: muvaffaqiyatda chat lokal bazadan ham o'chiriladi. */
    suspend fun leave(chatId: String): AppResult<Unit>
}
