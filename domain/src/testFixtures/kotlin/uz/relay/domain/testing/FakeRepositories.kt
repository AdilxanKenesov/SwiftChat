package uz.relay.domain.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.AppLanguage
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.AuthState
import uz.relay.domain.model.ChatMember
import uz.relay.domain.model.ChatSummary
import uz.relay.domain.model.ConnectionStatus
import uz.relay.domain.model.DownloadState
import uz.relay.domain.model.MemberRole
import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageMedia
import uz.relay.domain.model.SyncStatus
import uz.relay.domain.model.ThemeMode
import uz.relay.domain.model.User
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.CallRepository
import uz.relay.domain.repository.ChatRepository
import uz.relay.domain.repository.ConnectionRepository
import uz.relay.domain.repository.ContactRepository
import uz.relay.domain.repository.GroupRepository
import uz.relay.domain.repository.MediaRepository
import uz.relay.domain.repository.MessageRepository
import uz.relay.domain.repository.SettingsRepository
import uz.relay.domain.repository.TypingRepository
import uz.relay.domain.repository.UserRepository

/*
 * Repository fake'lari — mock kutubxonasiz testlar uchun. Qoida bir xil:
 *  - holat `MutableStateFlow`da (testdan o'zgartiriladi, ViewModel kuzatadi);
 *  - natijalar `var ...Result` orqali sozlanadi (standart — muvaffaqiyat);
 *  - chaqiruvlar ro'yxatlarga yoziladi (test "nima yuborildi"ni tekshiradi).
 */

class FakeAuthRepository : AuthRepository {
    val state = MutableStateFlow(AuthState.LOGGED_OUT)
    override val authState: Flow<AuthState> = state

    var requestOtpResult: AppResult<Unit> = AppResult.Success(Unit)
    /** `true` — profil hali to'ldirilmagan (yangi foydalanuvchi). */
    var verifyOtpResult: AppResult<Boolean> = AppResult.Success(false)
    val requestedPhones = mutableListOf<String>()
    val verifiedCodes = mutableListOf<Pair<String, String>>()
    var profileSetupCompleted = false
    var loggedOut = false

    override suspend fun requestOtp(phone: String): AppResult<Unit> {
        requestedPhones += phone
        return requestOtpResult
    }

    override suspend fun verifyOtp(phone: String, code: String): AppResult<Boolean> {
        verifiedCodes += phone to code
        return verifyOtpResult
    }

    override suspend fun completeProfileSetup() {
        profileSetupCompleted = true
    }

    override suspend fun logout() {
        loggedOut = true
        state.value = AuthState.LOGGED_OUT
    }
}

class FakeUserRepository : UserRepository {
    val me = MutableStateFlow<User?>(TestData.user())
    val users = MutableStateFlow<Map<String, User>>(emptyMap())
    var updateProfileResult: AppResult<User>? = null
    var searchResult: AppResult<List<User>> = AppResult.Success(emptyList())
    val updates = mutableListOf<Pair<String, String>>()
    val searches = mutableListOf<String>()

    override suspend fun updateProfile(displayName: String, username: String): AppResult<User> {
        updates += displayName to username
        val result = updateProfileResult ?: AppResult.Success(me.value!!.copy(displayName = displayName, username = username))
        if (result is AppResult.Success) me.value = result.data
        return result
    }

    override fun observeMe(): Flow<User?> = me
    override suspend fun refreshMe(): AppResult<User> = me.value?.let { AppResult.Success(it) } ?: AppResult.Error(TestData.apiError())
    override fun observeUserNames(): Flow<Map<String, String>> = users.map { all -> all.mapValues { it.value.displayName } }

    override suspend fun search(query: String): AppResult<List<User>> {
        searches += query
        return searchResult
    }

    override fun observeUser(userId: String): Flow<User?> = users.map { it[userId] }
    override suspend fun refreshUser(userId: String): AppResult<User> =
        users.value[userId]?.let { AppResult.Success(it) } ?: AppResult.Error(TestData.apiError("USER_NOT_FOUND", 404))

    override fun observeKnownUsers(): Flow<List<User>> = users.map { it.values.toList() }
}

class FakeSettingsRepository : SettingsRepository {
    val theme = MutableStateFlow(ThemeMode.LIGHT)
    val notifications = MutableStateFlow(true)
    val lang = MutableStateFlow(AppLanguage.UZ)
    override val themeMode: Flow<ThemeMode> = theme
    override suspend fun setThemeMode(mode: ThemeMode) { theme.value = mode }
    override val notificationsEnabled: Flow<Boolean> = notifications
    override suspend fun setNotificationsEnabled(enabled: Boolean) { notifications.value = enabled }
    override val language: Flow<AppLanguage> = lang
    override suspend fun setLanguage(language: AppLanguage) { lang.value = language }
}

class FakeConnectionRepository : ConnectionRepository {
    val current = MutableStateFlow(ConnectionStatus.CONNECTED)
    override val status: Flow<ConnectionStatus> = current
}

class FakeTypingRepository : TypingRepository {
    val current = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    override val typing: Flow<Map<String, Set<String>>> = current
}

class FakeChatRepository : ChatRepository {
    val chats = MutableStateFlow<List<ChatSummary>>(emptyList())
    val sync = MutableStateFlow(SyncStatus(isSyncing = false, isBootstrapped = true))
    var openDirectResult: AppResult<String>? = null
    var refreshResult: AppResult<Unit> = AppResult.Success(Unit)
    val mutes = mutableListOf<Triple<String, Boolean, Long?>>()
    val openedDirects = mutableListOf<String>()

    override fun observeChats(): Flow<List<ChatSummary>> = chats
    override fun observeChat(chatId: String): Flow<ChatSummary?> = chats.map { list -> list.firstOrNull { it.id == chatId } }
    override fun observeDirectChat(peerUserId: String): Flow<ChatSummary?> =
        chats.map { list -> list.firstOrNull { it.peerUserId == peerUserId } }
    override fun observeSyncStatus(): Flow<SyncStatus> = sync
    override suspend fun refresh(): AppResult<Unit> = refreshResult

    override suspend fun openDirect(peerUserId: String): AppResult<String> {
        openedDirects += peerUserId
        return openDirectResult ?: AppResult.Success("direct-$peerUserId")
    }

    override suspend fun setMuted(chatId: String, muted: Boolean, mutedUntil: Long?): AppResult<Unit> {
        mutes += Triple(chatId, muted, mutedUntil)
        chats.value = chats.value.map { if (it.id == chatId) it.copy(muted = muted, mutedUntil = mutedUntil) else it }
        return AppResult.Success(Unit)
    }
}

class FakeContactRepository : ContactRepository {
    val contacts = MutableStateFlow<List<User>>(emptyList())
    var addResult: AppResult<Unit>? = null
    /** `add` chaqirilganda kontaktga aylanadigan foydalanuvchilar (haqiqiy repo profilni serverdan oladi). */
    val knownUsers = mutableMapOf<String, User>()

    override fun observeContacts(): Flow<List<User>> = contacts
    override fun observeContactIds(): Flow<Set<String>> = contacts.map { list -> list.map { it.id }.toSet() }

    override suspend fun add(userId: String): AppResult<Unit> {
        val result = addResult ?: AppResult.Success(Unit)
        if (result is AppResult.Success) {
            val user = knownUsers[userId] ?: TestData.user(id = userId, name = userId)
            contacts.value = contacts.value.filterNot { it.id == userId } + user
        }
        return result
    }

    override suspend fun remove(userId: String) {
        contacts.value = contacts.value.filterNot { it.id == userId }
    }
}

class FakeGroupRepository : GroupRepository {
    val members = MutableStateFlow<Map<String, List<ChatMember>>>(emptyMap())
    var createResult: AppResult<String> = AppResult.Success("group-1")
    var actionResult: AppResult<Unit> = AppResult.Success(Unit)
    val created = mutableListOf<Pair<String, List<String>>>()
    val renamed = mutableListOf<Pair<String, String>>()
    val left = mutableListOf<String>()
    val removed = mutableListOf<Pair<String, String>>()
    val roleChanges = mutableListOf<Triple<String, String, MemberRole>>()
    val added = mutableListOf<Pair<String, List<String>>>()

    override fun observeMembers(chatId: String): Flow<List<ChatMember>> = members.map { it[chatId].orEmpty() }
    override suspend fun refreshMembers(chatId: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun createGroup(title: String, memberIds: List<String>): AppResult<String> {
        created += title to memberIds
        return createResult
    }

    override suspend fun addMembers(chatId: String, userIds: List<String>): AppResult<Unit> {
        added += chatId to userIds
        return actionResult
    }

    override suspend fun removeMember(chatId: String, userId: String): AppResult<Unit> {
        removed += chatId to userId
        return actionResult
    }

    override suspend fun changeRole(chatId: String, userId: String, role: MemberRole): AppResult<Unit> {
        roleChanges += Triple(chatId, userId, role)
        return actionResult
    }

    override suspend fun rename(chatId: String, title: String): AppResult<Unit> {
        renamed += chatId to title
        return actionResult
    }

    override suspend fun leave(chatId: String): AppResult<Unit> {
        left += chatId
        return actionResult
    }
}

class FakeMessageRepository : MessageRepository {
    val messages = MutableStateFlow<Map<String, List<Message>>>(emptyMap())
    var loadLatestResult: AppResult<Boolean> = AppResult.Success(false)
    var loadOlderResult: AppResult<Boolean> = AppResult.Success(false)
    var editResult: AppResult<Unit> = AppResult.Success(Unit)
    var deleteResult: AppResult<Unit> = AppResult.Success(Unit)
    var sendMediaResult: AppResult<Unit> = AppResult.Success(Unit)
    var searchResult: List<Message> = emptyList()

    /** (chatId, matn, javob berilgan xabar) */
    val sentTexts = mutableListOf<Triple<String, String, String?>>()
    val sentMedia = mutableListOf<Pair<Attachment, String?>>()
    val edits = mutableListOf<Pair<Long, String>>()
    val deletes = mutableListOf<Long>()
    val retries = mutableListOf<String>()
    val canceledUploads = mutableListOf<String>()
    val markedRead = mutableListOf<String>()
    var loadOlderCalls = 0
    var typingSignals = 0

    override fun observeMessages(chatId: String): Flow<List<Message>> = messages.map { it[chatId].orEmpty() }
    override suspend fun loadLatest(chatId: String): AppResult<Boolean> = loadLatestResult

    override suspend fun loadOlder(chatId: String): AppResult<Boolean> {
        loadOlderCalls++
        return loadOlderResult
    }

    override suspend fun sendText(chatId: String, text: String, replyToClientMessageId: String?) {
        sentTexts += Triple(chatId, text, replyToClientMessageId)
    }

    override suspend fun sendMedia(chatId: String, attachment: Attachment, caption: String?, replyToClientMessageId: String?): AppResult<Unit> {
        sentMedia += attachment to caption
        return sendMediaResult
    }

    override suspend fun cancelUpload(clientMessageId: String) { canceledUploads += clientMessageId }
    override suspend fun retry(clientMessageId: String) { retries += clientMessageId }

    override suspend fun edit(serverId: Long, text: String): AppResult<Unit> {
        edits += serverId to text
        return editResult
    }

    override suspend fun delete(serverId: Long): AppResult<Unit> {
        deletes += serverId
        return deleteResult
    }

    override fun sendTyping(chatId: String) { typingSignals++ }

    override suspend fun markRead(chatId: String): AppResult<Unit> {
        markedRead += chatId
        return AppResult.Success(Unit)
    }

    override suspend fun search(chatId: String, query: String): List<Message> = searchResult
}

class FakeMediaRepository : MediaRepository {
    var downloadStates: List<DownloadState> = listOf(DownloadState.Done("/cache/file"))
    var saveResult: AppResult<Unit> = AppResult.Success(Unit)
    val saved = mutableListOf<String>()

    override fun download(media: MessageMedia, fileName: String): Flow<DownloadState> = flowOf(*downloadStates.toTypedArray())

    override suspend fun saveToGallery(media: MessageMedia, fileName: String): AppResult<Unit> {
        saved += fileName
        return saveResult
    }
}

class FakeCallRepository : CallRepository {
    var startCallResult: AppResult<String> = AppResult.Success("call-1")
    var prepareGroupCallResult: AppResult<String> = AppResult.Success("group_chat")
    val groupCallCount = MutableStateFlow(0)
    val started = mutableListOf<Pair<String, Boolean>>()

    override suspend fun startCall(peerUserId: String, video: Boolean): AppResult<String> {
        started += peerUserId to video
        return startCallResult
    }

    override fun observeIncomingCalls(): Flow<String> = emptyFlow()
    override suspend fun prepareGroupCall(chatId: String, memberIds: List<String>): AppResult<String> = prepareGroupCallResult
    override fun observeGroupCall(chatId: String): Flow<Int> = groupCallCount
}
