package uz.relay.data.source.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sessiya ombori: access/refresh token, userId va deviceId ni shifrlangan holda saqlaydi.
 *
 * Nega DataStore + Tink: token — hisobga to'liq kirish kaliti, uni ochiq matnda diskda saqlash
 * xavfli (root'langan qurilma, backup). Tink AEAD (AES-256-GCM) kaliti Android Keystore'dagi
 * master key bilan o'ralgan — kalit qurilmadan tashqariga chiqmaydi. EncryptedSharedPreferences
 * deprecated bo'lgani uchun DataStore + Tink tanlangan.
 *
 * [session] va [profileSetupPending] — Flow: auth holati (sessiya tugaganda login ekraniga qaytish ham)
 * omborga o'zi ergashadi, qo'lda signal yuborish shart emas. Deshifrlangan sessiya xotirada ham
 * keshlanadi — OkHttp oqimlari ([TokenInterceptor], [TokenRefresher]) har so'rovda diskka
 * tegmasdan o'qiydi.
 *
 * Kim ishlatadi: AuthRepositoryImpl (login/logout), network interceptor'lar, RealtimeClient (WS token).
 */
@Singleton
class SessionStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) {

    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(DATASTORE_NAME)
    }

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    val session: Flow<Session?> = dataStore.data
        .map { it.decryptSession() }
        .distinctUntilChanged()

    /**
     * Yangi foydalanuvchi hali profilini to'ldirmagan. Diskda saqlanadi — ilova profil ekranida
     * yopilib qolsa, qayta ochilganda yana o'sha ekranga qaytadi.
     */
    val profileSetupPending: Flow<Boolean> = dataStore.data
        .map { it[KEY_PROFILE_SETUP_PENDING] ?: false }
        .distinctUntilChanged()

    // Birinchi yuklash va kesh yangilanishi uchun umumiy qulf (bir nechta OkHttp oqimi bir vaqtda chaqiradi).
    private val lock = Any()

    @Volatile
    private var loaded = false

    @Volatile
    private var cached: Session? = null

    /**
     * OkHttp oqimlari uchun bloklovchi o'qish; diskdan faqat bir marta yuklaydi (double-checked locking).
     * `runBlocking` bu yerda maqbul: interceptor'lar baribir fon oqimida va sinxron ishlaydi.
     */
    fun current(): Session? {
        if (!loaded) synchronized(lock) {
            if (!loaded) {
                cached = runBlocking { dataStore.data.first().decryptSession() }
                loaded = true
            }
        }
        return cached
    }

    /** Sessiya va profil bayrog'i BITTA edit'da yoziladi — UI hech qachon yarim holatni ko'rmaydi. */
    suspend fun save(session: Session, profileSetupPending: Boolean) {
        val encrypted = encrypt(session)
        dataStore.edit {
            it[KEY_SESSION] = encrypted
            it[KEY_PROFILE_SETUP_PENDING] = profileSetupPending
        }
        cache(session)
    }

    /** Refresh rotatsiyasidan keyin: userId/deviceId o'zgarmaydi, faqat token juftligi yangilanadi. */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        val session = current()?.copy(accessToken = accessToken, refreshToken = refreshToken) ?: return
        val encrypted = encrypt(session)
        dataStore.edit { it[KEY_SESSION] = encrypted }
        cache(session)
    }

    /** Profil to'ldirilgach `false` qilinadi — navigatsiya asosiy ekranga o'tadi. */
    suspend fun setProfileSetupPending(pending: Boolean) {
        dataStore.edit { it[KEY_PROFILE_SETUP_PENDING] = pending }
    }

    /** Logout: hamma narsa o'chiriladi, [session] `null` chiqaradi va UI login'ga qaytadi. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
        cache(null)
    }

    private fun cache(session: Session?) = synchronized(lock) {
        cached = session
        loaded = true
    }

    /** JSON → AEAD shifrlash → Base64 (Preferences faqat String saqlaydi). */
    private fun encrypt(session: Session): String {
        val plain = json.encodeToString(Session.serializer(), session).toByteArray()
        return Base64.encodeToString(aead.encrypt(plain, ASSOCIATED_DATA), Base64.NO_WRAP)
    }

    /** Endi deshifrlanmaydigan qiymat (masalan, Keystore kaliti o'chib ketgan) — tizimdan chiqilgan deb o'qiladi. */
    private fun Preferences.decryptSession(): Session? {
        val encrypted = this[KEY_SESSION] ?: return null
        return runCatching {
            val plain = aead.decrypt(Base64.decode(encrypted, Base64.NO_WRAP), ASSOCIATED_DATA)
            json.decodeFromString(Session.serializer(), String(plain))
        }.getOrNull()
    }

    private companion object {
        const val DATASTORE_NAME = "session"
        const val KEYSET_NAME = "relay_session_keyset"
        const val KEYSET_PREFS = "relay_session_keyset_prefs"
        // Keystore'dagi master key — Tink keyset'ini shifrlaydi.
        const val MASTER_KEY_URI = "android-keystore://relay_session_master_key"
        // AEAD associated data: shifrlangan matn boshqa kontekstga ko'chirilsa deshifrlanmaydi.
        val ASSOCIATED_DATA = "relay_session".toByteArray()
        val KEY_SESSION = stringPreferencesKey("session")
        val KEY_PROFILE_SETUP_PENDING = booleanPreferencesKey("profile_setup_pending")
    }
}
