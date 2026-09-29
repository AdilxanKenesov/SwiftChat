package uz.relay.data.repository_impl

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import uz.relay.core.common.result.AppError
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.data.mapper.parseSystemEvent
import uz.relay.data.mapper.toDomain
import uz.relay.data.mapper.toEntity
import uz.relay.data.model.request.AddMembersRequest
import uz.relay.data.model.request.ChangeRoleRequest
import uz.relay.data.model.request.CreateGroupRequest
import uz.relay.data.model.request.UpdateChatRequest
import uz.relay.data.model.response.ChatMemberResponse
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.RelayDatabase
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.source.local.database.dao.ChatMemberDao
import uz.relay.data.source.local.database.dao.MemberCursorDao
import uz.relay.data.source.local.database.dao.MessageDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.ChatMemberEntity
import uz.relay.data.source.network.api.ChatApi
import uz.relay.data.utils.safeApiCall
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.MemberRole
import uz.relay.domain.repository.GroupRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class GroupRepositoryImpl @Inject constructor(
    private val database: RelayDatabase,
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val memberDao: ChatMemberDao,
    private val messageDao: MessageDao,
    private val memberCursorDao: MemberCursorDao,
    private val userDao: UserDao,
    private val userCache: UserCache,
    private val sessionStorage: SessionStorage,
    private val json: Json
) : GroupRepository {

    override fun observeMembers(chatId: String): Flow<List<ChatMember>> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(emptyList())
            else memberDao.observe(chatId).map { items -> items.map { it.toDomain(me) } }
        }

    override suspend fun refreshMembers(chatId: String): AppResult<Unit> {
        // Bo'sh ro'yxat bilan "qo'shish" — hech kim qo'shilmaydi, lekin to'liq ro'yxat qaytadi (faqat ADMIN/OWNER).
        return when (val result = safeApiCall { chatApi.addMembers(chatId, AddMembersRequest(emptyList())) }) {
            is AppResult.Success -> {
                saveSnapshot(chatId, result.data.members)
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                val error = result.error
                // Oddiy a'zoga server ro'yxatni bermaydi (403) — tarixdagi SYSTEM xabarlardan tiklaymiz.
                if (error is AppError.Api && error.httpStatus == 403) {
                    rebuildFromHistory(chatId)
                    AppResult.Success(Unit)
                } else {
                    result
                }
            }
        }
    }

    /** Profillar ham yuklanadi — ro'yxatda ism va "online" holati darhol ko'rinsin. */
    private suspend fun saveSnapshot(chatId: String, members: List<ChatMemberResponse>) {
        val users = userCache.fetchMissing(members.map { it.userId })
        database.withTransaction {
            userDao.upsertAll(users)
            memberDao.replace(chatId, members.map { it.toEntity(chatId) })
        }
    }

    /**
     * SYSTEM xabarlarni eskisidan yangisiga qo'llab, a'zolar ro'yxatini tiklash (oddiy a'zo uchun yagona yo'l).
     * Tartib muhim: eski "qo'shildi" yangi "chiqdi"dan keyin qo'llansa, chiqib ketgan odam ro'yxatga qaytib
     * qolardi. Jonli update'lar bilan yig'ilgan qatorlar ham saqlanadi — faqat tarixda ko'ringan o'zgarishlar
     * ustiga yoziladi. Tarix to'liq yuklanmagan bo'lsa, ro'yxat to'liq bo'lmasligi mumkin.
     */
    private suspend fun rebuildFromHistory(chatId: String) {
        val members = LinkedHashMap<String, ChatMemberEntity>()
        memberDao.getAll(chatId).forEach { members[it.userId] = it }

        messageDao.systemMessages(chatId).forEach { message ->
            val event = parseSystemEvent(message.body, json) ?: return@forEach
            fun member(userId: String, role: String) = ChatMemberEntity(chatId, userId, role, message.createdAt)
            when (event.event) {
                "group_created" -> {
                    members[event.actorId] = member(event.actorId, ROLE_OWNER)
                    event.targetUserIds.forEach { members.putIfAbsent(it, member(it, ROLE_MEMBER)) }
                }
                "members_added" -> event.targetUserIds.forEach { members.putIfAbsent(it, member(it, ROLE_MEMBER)) }
                "member_removed", "member_left" -> event.targetUserIds.forEach { members.remove(it) }
            }
        }
        // Men shu guruhdaman (ro'yxat ochilgan) — tarix qisqa bo'lsa ham o'zim ro'yxatda bo'lishim kerak.
        sessionStorage.current()?.userId?.let { me ->
            members.putIfAbsent(me, ChatMemberEntity(chatId, me, ROLE_MEMBER, System.currentTimeMillis()))
        }

        val users = userCache.fetchMissing(members.keys)
        database.withTransaction {
            userDao.upsertAll(users)
            memberDao.replace(chatId, members.values.toList())
        }
    }

    override suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String> =
        safeApiCall { chatApi.createGroup(CreateGroupRequest(title = title, memberIds = memberIds)) }
            .map { chat ->
                val me = sessionStorage.current()?.userId
                val now = System.currentTimeMillis()
                database.withTransaction {
                    // Chat va a'zolar darhol yoziladi: suhbat va guruh ma'lumoti ekranlari sync'ni kutmasin.
                    chatDao.upsert(chat.toEntity())
                    val members = memberIds.map { ChatMemberEntity(chat.id, it, ROLE_MEMBER, now) } +
                        listOfNotNull(me?.let { ChatMemberEntity(chat.id, it, ROLE_OWNER, now) })
                    memberDao.upsertAll(members)
                }
                chat.id
            }

    override suspend fun addMembers(chatId: String, userIds: List<String>): AppResult<Unit> =
        safeApiCall { chatApi.addMembers(chatId, AddMembersRequest(userIds)) }
            .map { saveSnapshot(chatId, it.members) }

    override suspend fun removeMember(chatId: String, userId: String): AppResult<Unit> =
        safeApiCall { chatApi.removeMember(chatId, userId) }
            .map { memberDao.delete(chatId, userId) }

    override suspend fun changeRole(chatId: String, userId: String, role: MemberRole): AppResult<Unit> =
        safeApiCall { chatApi.changeRole(chatId, userId, ChangeRoleRequest(role.name)) }
            .map { memberDao.upsert(it.toEntity(chatId)) }

    override suspend fun rename(chatId: String, title: String): AppResult<Unit> =
        safeApiCall { chatApi.updateChat(chatId, UpdateChatRequest(title = title)) }
            .map { chatDao.upsert(it.toEntity()) }

    /**
     * Chiqqandan keyin server bu chat haqida boshqa update yubormaydi — lokal nusxani o'zimiz tozalaymiz
     * (aks holda chat ro'yxatda "osilib" qolardi).
     */
    override suspend fun leave(chatId: String): AppResult<Unit> =
        safeApiCall { chatApi.leave(chatId) }
            .map {
                database.withTransaction {
                    chatDao.delete(chatId)
                    messageDao.deleteByChat(chatId)
                    memberCursorDao.deleteByChat(chatId)
                    memberDao.deleteByChat(chatId)
                }
            }

    private companion object {
        const val ROLE_OWNER = "OWNER"
        const val ROLE_MEMBER = "MEMBER"
    }
}
