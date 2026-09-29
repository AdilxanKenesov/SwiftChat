package uz.relay.data.sync

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import uz.relay.core.common.result.AppResult
import uz.relay.data.mapper.systemEventUserIds
import uz.relay.data.mapper.toEntity
import uz.relay.data.model.response.ChatResponse
import uz.relay.data.model.response.UpdateResponse
import uz.relay.data.source.local.cache.UserCache
import uz.relay.data.source.local.database.RelayDatabase
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.source.local.database.dao.MemberCursorDao
import uz.relay.data.source.local.database.dao.SyncStateDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.network.api.ChatApi
import uz.relay.data.source.network.api.SyncApi
import uz.relay.data.utils.safeApiCall
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Jonli frame bilan nima qilish kerak (Guide, 4-bo'lim "Gap detection"). */
enum class LiveUpdateAction { IGNORE, APPLY, CATCH_UP }

/**
 * - `seq <= cursor` → allaqachon qo'llangan (dublikat yoki kechikkan frame) — e'tiborsiz qoldiramiz.
 * - `seq == cursor + 1` → navbatdagisi — qo'llaymiz.
 * - `seq > cursor + 1` → orada nimadir tushib qolgan (yo'qolgan yoki tartibsiz frame): bu frame'ni
 *   ko'r-ko'rona qo'llamaymiz, teshikni REST'dan to'ldiramiz (bu frame ham o'sha javob ichida keladi).
 */
fun classifyLiveUpdate(cursor: Long, updateSeq: Long): LiveUpdateAction = when {
    updateSeq <= cursor -> LiveUpdateAction.IGNORE
    updateSeq == cursor + 1 -> LiveUpdateAction.APPLY
    else -> LiveUpdateAction.CATCH_UP
}

/**
 * Server bilan sinxronlash markazi: bootstrap, catch-up va WebSocket'ning jonli update'lari.
 *
 * Hammasi bitta Mutex ostida: kursor bilan ishlaydigan ikki jarayon hech qachon parallel ketmaydi.
 * Aks holda ikkalasi bir xil kursordan boshlab, bir-birining natijasini buzishi mumkin edi.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val database: RelayDatabase,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val memberCursorDao: MemberCursorDao,
    private val syncStateDao: SyncStateDao,
    private val syncApi: SyncApi,
    private val chatApi: ChatApi,
    private val userCache: UserCache,
    private val updateApplier: UpdateApplier,
    private val json: Json
) {
    private val mutex = Mutex()

    private val _isSyncing = MutableStateFlow(false)

    /** UI'dagi "Yangilanmoqda…" uchun. */
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** `null` — baza hali hech qachon to'ldirilmagan (bootstrap bo'lmagan). */
    fun observeCursor(): Flow<Long?> = syncStateDao.observeCursor()

    /** `null` — hali bootstrap qilinmagan. */
    suspend fun cursor(): Long? = syncStateDao.getCursor()

    /** Bootstrap bo'lmagan bo'lsa — bootstrap, aks holda kursordan keyingi hamma hodisalarni olish. */
    suspend fun catchUp(): AppResult<Unit> = mutex.withLock { syncing { catchUpLocked() } }

    /** WebSocket'dan kelgan bitta update (tartib va teshik tekshiruvi bilan). */
    suspend fun onLiveUpdate(update: UpdateResponse) {
        mutex.withLock {
            // Bootstrap hali bo'lmagan: bu hodisa baribir bootstrap snapshot'iga yoki keyingi catch-up'ga kiradi.
            val cursor = syncStateDao.getCursor() ?: return@withLock
            when (classifyLiveUpdate(cursor, update.updateSeq)) {
                LiveUpdateAction.IGNORE -> Unit
                LiveUpdateAction.APPLY -> try {
                    updateApplier.apply(listOf(update))
                } catch (e: IOException) {
                    // Kerakli chat qatori yuklanmadi — kursor surilmadi; keyingi frame teshikni ko'rib catch-up qiladi.
                    Log.w(TAG, "Jonli update qo'llanmadi", e)
                }
                LiveUpdateAction.CATCH_UP -> syncing { catchUpLocked() }
            }
        }
    }

    private suspend fun <T> syncing(block: suspend () -> T): T {
        _isSyncing.value = true
        try {
            return block()
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun catchUpLocked(): AppResult<Unit> {
        if (syncStateDao.getCursor() == null) return fullResyncLocked()

        val result = safeApiCall {
            var needsResync = false
            while (true) {
                val cursor = syncStateDao.getCursor() ?: 0
                val page = syncApi.getUpdates(since = cursor)
                if (page.tooLong) {
                    needsResync = true
                    break
                }
                if (page.updates.isEmpty()) break
                // Sahifa kursordan teshiksiz davom etadi (server kafolati) — butunicha qo'llaymiz.
                updateApplier.apply(page.updates)
                // Serverning joriy kursoriga yetdik — boshqa sahifa yo'q.
                if (page.updates.last().updateSeq >= page.state.updateSeq) break
            }
            needsResync
        }
        return when (result) {
            is AppResult.Error -> result
            // Kursor 7 kunlik saqlash muddatidan eski yoki 10 000 hodisadan ortiq orqada: sahifalab
            // yetib bo'lmaydi, snapshot'dan qayta boshlaymiz.
            is AppResult.Success -> if (result.data) fullResyncLocked() else AppResult.Success(Unit)
        }
    }

    /**
     * Bootstrap — Guide'ning 4-bo'limidagi tartibda, AYNAN shu tartibda:
     *  1. `GET /v1/updates/state` → kursor.
     *  2. `GET /v1/chats` (hamma sahifalar) → snapshot.
     *  3. Snapshot va kursor bitta tranzaksiyada saqlanadi.
     *
     * Nega kursor snapshot'dan OLDIN o'qiladi: 1- va 2-qadam orasida sodir bo'lgan hodisa kursordan keyin
     * turadi, demak keyingi catch-up uni yana beradi (qayta qo'llash zararsiz — idempotent). Teskari
     * tartibda o'sha oraliqdagi hodisa snapshot'da ham, catch-up'da ham bo'lmay, jimgina yo'qolardi.
     */
    private suspend fun fullResyncLocked(): AppResult<Unit> = safeApiCall {
        val cursor = syncApi.getState().updateSeq
        val chats = fetchAllChats()
        // DIRECT chatda server `title` bermaydi — suhbatdosh ismi profildan olinadi. Guruhdagi oxirgi xabar
        // egasi ("Malika: ...") va SYSTEM xabar ishtirokchilari ham kerak.
        val users = userCache.fetchMissing(
            chats.flatMap { chat ->
                listOfNotNull(chat.peerUserId, chat.lastMessage?.senderId) +
                    systemEventUserIds(chat.lastMessage?.body.takeIf { chat.lastMessage?.type == "SYSTEM" }, json)
            }
        )

        database.withTransaction {
            userDao.upsertAll(users)
            chatDao.replaceAll(chats.map { it.toEntity() })
            // Kursorlar eski snapshot'ga tegishli — yangi update'lar ularni qayta to'ldiradi.
            memberCursorDao.deleteAll()
            syncStateDao.setCursor(cursor)
        }
    }

    private suspend fun fetchAllChats(): List<ChatResponse> {
        val result = mutableListOf<ChatResponse>()
        var cursor: String? = null
        do {
            val page = chatApi.getChats(cursor = cursor)
            result += page.chats
            cursor = page.nextCursor
        } while (cursor != null)
        return result
    }

    private companion object {
        const val TAG = "SyncEngine"
    }
}
