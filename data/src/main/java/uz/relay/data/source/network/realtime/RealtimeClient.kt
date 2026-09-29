package uz.relay.data.source.network.realtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import uz.relay.core.common.dispatcher.ApplicationScope
import uz.relay.data.BuildConfig
import uz.relay.data.di.PublicClient
import uz.relay.data.source.local.Session
import uz.relay.data.source.local.SessionStorage
import uz.relay.data.source.network.interceptor.RefreshOutcome
import uz.relay.data.source.network.interceptor.TokenRefresher
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** WebSocket ulanish holati — UI'dagi "Ulanmoqda..." indikatori va RealtimeCoordinator shunga qaraydi. */
enum class RealtimeState {
    /** Ishga tushirilmagan: ilova fonda yoki login qilinmagan. Indikator kerak emas. */
    IDLE,

    /** Ulanmoqda yoki uzilgandan keyin qayta ulanishni kutmoqda. */
    CONNECTING,

    /** `auth_ok` olindi — frame yuborish va qabul qilish mumkin. */
    CONNECTED
}

/**
 * Bitta WebSocket ulanishini boshqaradi: auth, qayta ulanish (backoff) va close kodlari.
 *
 * Socket faqat "optimallashtirish": hujjatga ko'ra jonli frame'lar yo'qolishi, takrorlanishi yoki tartibsiz
 * kelishi mumkin, haqiqat manbai esa `GET /v1/updates`. Shuning uchun bu klass hech narsani bazaga
 * qo'llamaydi — faqat frame'larni [frames] oqimiga uzatadi. Qo'llash va teshiklarni yamash SyncEngine'da.
 *
 * Nega OkHttp WebSocket: REST bilan bir xil klient (DNS, TLS, proxy sozlamalari umumiy), ping/pong'ni
 * o'zi boshqaradi va qo'shimcha kutubxona talab qilmaydi. `@PublicClient` olinadi, chunki token HTTP
 * sarlavhada emas, birinchi `auth` frame'ida yuboriladi — TokenInterceptor/Authenticator bu yerda keraksiz.
 *
 * Kim ishlatadi: RealtimeCoordinator (start/stop, frame'larni SyncEngine'ga uzatish), OutboxSender,
 * ReceiptSender va MessageRepositoryImpl ([send] orqali; ulanish bo'lmasa REST'ga o'tiladi).
 */
@Singleton
class RealtimeClient @Inject constructor(
    @PublicClient baseClient: OkHttpClient,
    private val json: Json,
    private val sessionStorage: SessionStorage,
    private val tokenRefresher: TokenRefresher,
    @ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Server har 25 s da ping yuboradi va 60 s jim turgan socket'ni uzadi. Bizning ping ham yoqilgan:
     * pong kelmasa OkHttp ulanishni o'zi "o'lik" deb yopadi — tarmoq jimgina uzilganini tezroq sezamiz.
     * readTimeout = 0: socket uzoq vaqt jim turishi normal holat, uni timeout deb uzmaslik kerak.
     */
    private val client = baseClient.newBuilder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _state = MutableStateFlow(RealtimeState.IDLE)
    val state: StateFlow<RealtimeState> = _state.asStateFlow()

    /**
     * Kelgan frame'lar, kelish tartibida (protokol: bitta socket'ning frame'lari qat'iy tartibda keladi).
     * Bufer bor: kolektor sekin bo'lsa (masalan, catch-up paytida) o'quvchi kutadi, frame tashlanmaydi.
     */
    private val _frames = MutableSharedFlow<ServerFrame>(extraBufferCapacity = 64)
    val frames: SharedFlow<ServerFrame> = _frames.asSharedFlow()

    /** Faqat `auth_ok` dan keyin o'rnatiladi — autentifikatsiyadan oldin hech narsa yuborilmasin. */
    @Volatile
    private var socket: WebSocket? = null

    private var loopJob: Job? = null

    // CONFLATED: bir nechta "uyg'on" signali bittaga birlashadi — backoff kutishini uzish uchun bittasi yetarli.
    private val wakeUp = Channel<Unit>(Channel.CONFLATED)

    /**
     * Ulanish siklini ishga tushiradi (allaqachon ishlayotgan bo'lsa hech narsa qilmaydi).
     * @param cursorProvider `auth` frame'idagi kursor (server uchun ma'lumot); har qayta ulanishda qayta o'qiladi.
     */
    @Synchronized
    fun start(cursorProvider: suspend () -> Long) {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { runLoop(cursorProvider) }
    }

    /** Siklni to'xtatadi va socket'ni yopadi (ilova fonga o'tganda yoki logout'da). */
    @Synchronized
    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    /** Backoff kutishini to'xtatib, darhol qayta ulanish (masalan, internet qaytganda). */
    fun reconnectNow() {
        wakeUp.trySend(Unit)
    }

    /** @return `false` — ulanish yo'q; chaqiruvchi REST'ga o'tadi. */
    fun send(frame: ClientFrame): Boolean = socket?.send(json.encodeClientFrame(frame)) ?: false

    private suspend fun runLoop(cursorProvider: suspend () -> Long) {
        var failedAttempts = 0
        var unauthorizedInARow = 0
        try {
            while (currentCoroutineContext().isActive) {
                // Sessiya yo'q (logout) — ulanishga urinish ma'nosiz, sikl tugaydi.
                val session = sessionStorage.current() ?: break
                _state.value = RealtimeState.CONNECTING

                val outcome = connectOnce(session, cursorProvider())
                // Muvaffaqiyatli auth bo'lgan bo'lsa, backoff va 4003 hisoblagichlari noldan boshlanadi.
                if (outcome.wasAuthenticated) {
                    failedAttempts = 0
                    unauthorizedInARow = 0
                }

                when (outcome.closeCode) {
                    // Access token muddati tugadi: yangilab, darhol qayta ulanamiz — qayta login shart emas.
                    CLOSE_TOKEN_EXPIRED -> {
                        if (tokenRefresher.refresh(session.accessToken) == RefreshOutcome.SessionEnded) break
                        continue
                    }
                    // Token/deviceId yaroqsiz yoki qurilma logout qilingan. Bir marta refresh bilan urinamiz:
                    // sessiya haqiqatan bekor qilingan bo'lsa, refresh TOKEN_REUSED oladi va sessiya tozalanadi
                    // (UI login'ga qaytadi). Ketma-ket ikkinchi 4003 — ko'r-ko'rona urinishni to'xtatamiz.
                    CLOSE_UNAUTHORIZED -> {
                        unauthorizedInARow++
                        if (unauthorizedInARow >= 2) {
                            sessionStorage.clear()
                            break
                        }
                        if (tokenRefresher.refresh(session.accessToken) == RefreshOutcome.SessionEnded) break
                        continue
                    }
                    // Shu qurilma uchun yangiroq socket ochildi. Qayta ulansak, ikkalamiz bir-birimizni navbat
                    // bilan uzib, cheksiz aylanib qolamiz — shuning uchun to'xtaymiz.
                    CLOSE_SESSION_REPLACED -> break
                    // `auth` 10 s ichida yetib bormadi — darhol qayta urinamiz.
                    CLOSE_AUTH_TIMEOUT -> continue
                }

                // Qolgan hammasi (1001 server restart, 1013 sekin o'qish, 1006 tarmoq uzilishi...) — backoff bilan.
                failedAttempts++
                waitBeforeReconnect(failedAttempts)
            }
        } finally {
            socket = null
            _state.value = RealtimeState.IDLE
        }
    }

    /** Bitta ulanish natijasi: yopilish kodi (`null` — tarmoq xatosi) va auth'gacha yetib borilganmi. */
    private class Outcome(val closeCode: Int?, val wasAuthenticated: Boolean)

    /** Bitta ulanish: ochiladi, `auth` yuboriladi, yopilguncha frame'lar o'qiladi. */
    private suspend fun connectOnce(session: Session, cursor: Long): Outcome = coroutineScope {
        // UNLIMITED: OkHttp listener'i bloklanmasligi kerak, trySend hech qachon rad etilmaydi.
        val incoming = Channel<String>(Channel.UNLIMITED)
        val closed = CompletableDeferred<Int?>()
        val authenticated = AtomicBoolean(false)

        val webSocket = client.newWebSocket(
            Request.Builder().url(BuildConfig.WS_URL).build(),
            object : WebSocketListener() {
                // Protokol: ochilgandan keyin birinchi frame albatta `auth` bo'lishi kerak.
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(json.encodeClientFrame(ClientFrame.Auth(session.accessToken, session.deviceId, cursor)))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    incoming.trySend(text)
                }

                // Server yopishni boshladi — javoban biz ham yopamiz va kodni natija sifatida olamiz.
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, null)
                    closed.complete(code)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    closed.complete(code)
                }

                // Tarmoq xatosi: yopilish kodi yo'q — oddiy backoff bilan qayta ulanamiz.
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    closed.complete(null)
                }
            }
        )

        // Listener OkHttp thread'ida ishlaydi. Frame'larni bitta coroutine'da ketma-ket qayta ishlaymiz:
        // tartib saqlanadi, `emit` esa kolektor sekin bo'lsa o'quvchini tabiiy ravishda kutdiradi.
        val reader = launch {
            for (text in incoming) {
                val frame = json.decodeServerFrame(text) ?: continue
                if (frame is ServerFrame.AuthOk) {
                    authenticated.set(true)
                    socket = webSocket
                    _state.value = RealtimeState.CONNECTED
                }
                _frames.emit(frame)
            }
        }

        try {
            Outcome(closeCode = closed.await(), wasAuthenticated = authenticated.get())
        } finally {
            socket = null
            reader.cancel()
            // stop() (coroutine bekor qilinishi) holatida ham socket ochiq qolib ketmasin.
            webSocket.close(CLOSE_NORMAL, null)
        }
    }

    /**
     * Eksponensial backoff (1, 2, 4 … 30 s) + tasodifiy "jitter": server qayta ishga tushganda butun sinf
     * bir soniyada bir vaqtda urilib, uni yana yiqitmasin. Internet qaytsa [reconnectNow] kutishni to'xtatadi.
     */
    private suspend fun waitBeforeReconnect(attempt: Int) {
        val base = (1_000L shl (attempt - 1).coerceAtMost(5)).coerceAtMost(MAX_BACKOFF_MS)
        val delayMs = base + Random.nextLong(0, 500)
        // Timeout — oddiy kutish tugadi; wakeUp kelsa — darhol qayta ulanamiz.
        withTimeoutOrNull(delayMs) { wakeUp.receive() }
    }

    // WebSocket yopilish kodlari (protokol hujjatidan).
    private companion object {
        const val CLOSE_NORMAL = 1000
        const val CLOSE_TOKEN_EXPIRED = 4001
        const val CLOSE_UNAUTHORIZED = 4003
        const val CLOSE_AUTH_TIMEOUT = 4008
        const val CLOSE_SESSION_REPLACED = 4009
        const val MAX_BACKOFF_MS = 30_000L
    }
}
