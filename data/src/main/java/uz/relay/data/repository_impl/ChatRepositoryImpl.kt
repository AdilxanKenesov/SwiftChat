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
import uz.relay.data.mapper.toDomain
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.sync.SyncEngine
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.SyncStatus
import uz.relay.domain.repository.ChatRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
internal class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val sessionStorage: SessionStorage,
    private val syncEngine: SyncEngine,
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

    override fun observeSyncStatus(): Flow<SyncStatus> = combine(
        syncEngine.isSyncing,
        syncEngine.observeCursor()
    ) { isSyncing, cursor ->
        SyncStatus(isSyncing = isSyncing, isBootstrapped = cursor != null)
    }.distinctUntilChanged()

    override suspend fun refresh(): AppResult<Unit> = syncEngine.catchUp()
}
