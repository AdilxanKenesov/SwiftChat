package uz.relay.data.repository_impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import uz.relay.data.connection.NetworkMonitor
import uz.relay.data.source.network.realtime.RealtimeClient
import uz.relay.data.source.network.realtime.RealtimeState
import uz.relay.data.sync.SyncEngine
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.repository.ConnectionRepository
import javax.inject.Inject

internal class ConnectionRepositoryImpl @Inject constructor(
    networkMonitor: NetworkMonitor,
    realtimeClient: RealtimeClient,
    syncEngine: SyncEngine
) : ConnectionRepository {

    /** Ustuvorlik: internet yo'q > yangilanmoqda > ulanmoqda > ulangan. */
    override val status: Flow<ConnectionStatus> = combine(
        networkMonitor.isOnline,
        realtimeClient.state,
        syncEngine.isSyncing
    ) { online, socket, syncing ->
        when {
            !online -> ConnectionStatus.OFFLINE
            syncing -> ConnectionStatus.UPDATING
            socket == RealtimeState.CONNECTING -> ConnectionStatus.CONNECTING
            // IDLE — socket ataylab yopiq (fon, login yo'q): indikator kerak emas.
            else -> ConnectionStatus.CONNECTED
        }
    }.distinctUntilChanged()
}
