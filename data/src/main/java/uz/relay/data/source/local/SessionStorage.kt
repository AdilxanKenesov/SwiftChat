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
 * Access/refresh tokens and deviceId, encrypted with a Tink AEAD key kept in the Android Keystore.
 *
 * [session] and [profileSetupPending] are Flows, so the auth state (and the jump back to login when
 * the session ends) follows the storage by itself. The decrypted session is also cached in memory
 * so OkHttp threads read it without touching disk on every request.
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
     * A new user has not filled in the profile yet. Kept on disk so the app returns to the
     * profile screen if it is closed there.
     */
    val profileSetupPending: Flow<Boolean> = dataStore.data
        .map { it[KEY_PROFILE_SETUP_PENDING] ?: false }
        .distinctUntilChanged()

    private val lock = Any()

    @Volatile
    private var loaded = false

    @Volatile
    private var cached: Session? = null

    /** Blocking read for OkHttp threads; loads from disk once. */
    fun current(): Session? {
        if (!loaded) synchronized(lock) {
            if (!loaded) {
                cached = runBlocking { dataStore.data.first().decryptSession() }
                loaded = true
            }
        }
        return cached
    }

    /** Session and the profile flag are written in ONE edit, so the UI never sees a half state. */
    suspend fun save(session: Session, profileSetupPending: Boolean) {
        val encrypted = encrypt(session)
        dataStore.edit {
            it[KEY_SESSION] = encrypted
            it[KEY_PROFILE_SETUP_PENDING] = profileSetupPending
        }
        cache(session)
    }

    /** After a refresh rotation: userId/deviceId stay, only the token pair changes. */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        val session = current()?.copy(accessToken = accessToken, refreshToken = refreshToken) ?: return
        val encrypted = encrypt(session)
        dataStore.edit { it[KEY_SESSION] = encrypted }
        cache(session)
    }

    suspend fun setProfileSetupPending(pending: Boolean) {
        dataStore.edit { it[KEY_PROFILE_SETUP_PENDING] = pending }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
        cache(null)
    }

    private fun cache(session: Session?) = synchronized(lock) {
        cached = session
        loaded = true
    }

    private fun encrypt(session: Session): String {
        val plain = json.encodeToString(Session.serializer(), session).toByteArray()
        return Base64.encodeToString(aead.encrypt(plain, ASSOCIATED_DATA), Base64.NO_WRAP)
    }

    /** A value that no longer decrypts (e.g. the Keystore key was wiped) reads as logged out. */
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
        const val MASTER_KEY_URI = "android-keystore://relay_session_master_key"
        val ASSOCIATED_DATA = "relay_session".toByteArray()
        val KEY_SESSION = stringPreferencesKey("session")
        val KEY_PROFILE_SETUP_PENDING = booleanPreferencesKey("profile_setup_pending")
    }
}
