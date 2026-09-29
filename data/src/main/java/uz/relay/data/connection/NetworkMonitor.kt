package uz.relay.data.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import uz.relay.core.common.dispatcher.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Qurilmada internet bormi (ConnectivityManager callback'i orqali).
 *
 * Nega kerak: (1) internet yo'qligida "Ulanmoqda…" o'rniga aniqroq "Internet aloqasi yoʻq" banneri;
 * (2) internet qaytishi bilan WebSocket'ni backoff tugashini kutmasdan darhol qayta ulash.
 *
 * NET_CAPABILITY_INTERNET (VALIDATED emas): VALIDATED Google serveriga tekshiruv so'roviga bog'liq —
 * u bloklangan tarmoqlarda internet bo'lsa ham ilova doim "offline" ko'rinib qolardi.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(currentlyOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, currentlyOnline())

    private fun currentlyOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
