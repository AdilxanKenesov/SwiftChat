package uz.relay.data.repository_impl

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import uz.relay.core.common.result.AppResult
import uz.relay.core.common.result.map
import uz.relay.data.mapper.toEntity
import uz.relay.data.model.request.ChatSettingsRequest
import uz.relay.data.model.request.CreateDirectRequest
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.network.api.ChatApi
import uz.relay.data.utils.safeApiCall
import uz.relay.data.mapper.toDomain
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.sync.SyncEngine
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.SyncStatus
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * [ChatRepository] implementatsiyasi: chat ro'yxati, bitta chat va sync holatini kuzatish.
 *
 * Offline-first: UI faqat Room'dan o'qiydi, server ma'lumoti esa [SyncEngine] (bootstrap/catch-up) va
 * realtime update'lar orqali bazaga tushadi — shuning uchun internet yo'q paytda ham ro'yxat ko'rinadi.
 * To'g'ridan-to'g'ri so'rovlar (direct chat ochish, mute) natijasi ham darhol bazaga yoziladi.
 * Chat ro'yxati, suhbat va chat ma'lumoti ekranlari ishlatadi.
 */
@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val sessionStorage: SessionStorage,
    private val syncEngine: SyncEngine,
    private val chatApi: ChatApi,
    private val userCache: UserCache,
    private val userDao: UserDao,
    private val json: Json
) : ChatRepository {

    /**
     * Mening userId'm so'rov parametri sifatida kerak ("meniki"mi va boshqa a'zolarning kursorlari).
     * Sessiya o'zgarsa (boshqa hisobga kirilsa), flatMapLatest eski so'rovni to'xtatib, yangisini boshlaydi.
     */
    override fun observeChats(): Flow<List<ChatSummary>> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(emptyList())
            else chatDao.observeChatList(me).map { items -> items.map { it.toDomain(me, json) } }
        }

    /** Bitta chatni kuzatadi (suhbat ekrani sarlavhasi, mute holati); chat o'chsa `null`. */
    override fun observeChat(chatId: String): Flow<ChatSummary?> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(null)
            else chatDao.observeChat(chatId, me).map { it?.toDomain(me, json) }
        }

    /** Foydalanuvchi bilan shaxsiy chat bormi — profil ekrani "Xabar yozish" uchun; hali yaratilmagan bo'lsa `null`. */
    override fun observeDirectChat(peerUserId: String): Flow<ChatSummary?> = sessionStorage.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { me ->
            if (me == null) flowOf(null)
            else chatDao.observeDirectChat(peerUserId, me).map { it?.toDomain(me, json) }
        }

    /** Kursor hali yo'q — bootstrap bo'lmagan (birinchi kirish): UI bo'sh ro'yxat o'rniga yuklanish ko'rsatadi. */
    override fun observeSyncStatus(): Flow<SyncStatus> = combine(
        syncEngine.isSyncing,
        syncEngine.observeCursor()
    ) { isSyncing, cursor ->
        SyncStatus(isSyncing = isSyncing, isBootstrapped = cursor != null)
    }.distinctUntilChanged()

    /** Pull-to-refresh: kursordan keyingi update'larni serverdan tortib oladi. */
    override suspend fun refresh(): AppResult<Unit> = syncEngine.catchUp()

    /**
     * Chat qatori darhol bazaga yoziladi — suhbat ekrani (u bazadan o'qiydi) sync'ni kutmasdan ochilsin.
     * Yangi chat bo'lsa server `chat` update'ini ham yuboradi; qayta yozish zararsiz.
     */
    override suspend fun openDirect(peerUserId: String): AppResult<String> =
        safeApiCall { chatApi.createDirect(CreateDirectRequest(peerUserId)) }
            .map { chat ->
                userDao.upsertAll(userCache.fetchMissing(listOf(peerUserId)))
                chatDao.upsert(chat.toEntity())
                chat.id
            }

    /** Server yangilangan chat qatorini qaytaradi — uni darhol yozamiz, ro'yxatdagi belgi shu zahoti o'zgarsin. */
    override suspend fun setMuted(chatId: String, muted: Boolean, mutedUntil: Long?): AppResult<Unit> =
        safeApiCall { chatApi.updateSettings(chatId, ChatSettingsRequest(muted = muted, mutedUntil = mutedUntil.takeIf { muted })) }
            .map { chatDao.upsert(it.toEntity()) }
}
