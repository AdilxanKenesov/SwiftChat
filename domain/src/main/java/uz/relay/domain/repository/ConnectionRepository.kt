package uz.relay.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.relay.domain.model.ConnectionStatus

interface ConnectionRepository {
    /** Internet + WebSocket + sync holatidan yig'ilgan yagona holat. */
    val status: Flow<ConnectionStatus>
}
