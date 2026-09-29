package uz.relay.feature.conversation.viewer

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.viewmodel.orbitContainer
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.MediaKind
import uz.relay.domain.model.MessageType
import uz.relay.domain.usecase.media.SaveMediaToGalleryUseCase
import uz.relay.domain.usecase.message.ObserveMessagesUseCase
import uz.relay.domain.usecase.user.ObserveMeUseCase
import uz.relay.domain.usecase.user.ObserveUserNamesUseCase
import java.io.File

/**
 * Media ko'ruvchi ViewModel'i: chatdagi rasm/videolarni kuzatadi, video pleyerni boshqaradi va galereyaga saqlaydi.
 *
 * Pleyer ViewModel'da yashaydi: ekran burilganda video to'xtab, boshidan boshlanmaydi. Bitta pleyer
 * hamma sahifalar uchun — faqat ko'rinib turgan video yuklanadi (xotira va trafik tejaladi).
 * Pleyer `onCleared`da bo'shatiladi — ekran back stack'dan chiqqanda dekoder va tarmoq resurslari qaytariladi.
 *
 * Ikki runtime argument (`chatId`, `clientMessageId`) AssistedInject orqali Nav3 kalitidan keladi; bir xil
 * tipdagi ikki `@Assisted` parametrni Dagger ajrata olishi uchun ularga nom beriladi.
 */
@OptIn(UnstableApi::class)
@HiltViewModel(assistedFactory = MediaViewerViewModel.Factory::class)
class MediaViewerViewModel @AssistedInject constructor(
    @Assisted("chatId") private val chatId: String,
    @Assisted("clientMessageId") private val openedClientMessageId: String,
    @ApplicationContext context: Context,
    /** Token bilan ishlaydigan tarmoq manbai (data modulida, media OkHttp klienti ustida). */
    mediaDataSourceFactory: DataSource.Factory,
    private val observeMessages: ObserveMessagesUseCase,
    private val observeUserNames: ObserveUserNamesUseCase,
    private val observeMe: ObserveMeUseCase,
    private val saveMediaToGallery: SaveMediaToGalleryUseCase,
    private val directions: MediaViewerContract.Directions
) : ViewModel(), MediaViewerContract.ViewModel {

    /** MediaViewerScreen shu factory orqali ViewModel'ni kalitdagi qiymatlar bilan yaratadi. */
    @AssistedFactory
    interface Factory {
        fun create(@Assisted("chatId") chatId: String, @Assisted("clientMessageId") clientMessageId: String): MediaViewerViewModel
    }

    /**
     * DefaultDataSource: `file://` (o'zim yuborgan video) — to'g'ridan-to'g'ri, `https://` — token qo'shadigan
     * media manbai orqali. Server `Range`ni qo'llaydi, shuning uchun video oxirigacha yuklanmasdan ijro etiladi.
     */
    val player: Player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(context, mediaDataSourceFactory)))
        .build()

    /** Pleyerga hozir yuklangan xabar — bir xil videoni qayta yuklamaslik uchun. */
    private var loadedKey: String? = null

    override val container =
        orbitContainer<MediaViewerContract.UiState, MediaViewerContract.SideEffect>(MediaViewerContract.UiState()) {
            observeData()
        }

    override fun onEventDispatcher(intent: MediaViewerContract.Intent) {
        when (intent) {
            MediaViewerContract.Intent.OnBack -> intent { directions.back() }
            is MediaViewerContract.Intent.OnPageChange -> intent { preparePage(state.items.getOrNull(intent.index)) }
            is MediaViewerContract.Intent.OnSave -> save(intent.item)
        }
    }

    /** Chat xabarlaridan faqat o'chirilmagan rasm/videolarni ajratib, ko'ruvchi sahifalariga aylantiradi. */
    private fun observeData() = intent {
        repeatOnSubscription {
            combine(observeMessages(chatId), observeUserNames(), observeMe()) { messages, names, me ->
                Triple(messages, names, me?.id)
            }.collect { (messages, names, myUserId) ->
                // Bazada eng yangisi birinchi — ko'ruvchida vaqt bo'yicha (chapda eskisi) bo'lishi kerak.
                val items = messages.asReversed().mapNotNull { message ->
                    val media = message.media.firstOrNull()
                    val visual = message.type == MessageType.IMAGE || message.type == MessageType.VIDEO
                    if (visual && !message.isDeleted && media != null) ViewerItem(message, media) else null
                }
                reduce {
                    state.copy(
                        items = items,
                        initialIndex = state.initialIndex ?: items.indexOfFirst { it.key == openedClientMessageId }.takeIf { it >= 0 },
                        userNames = names,
                        myUserId = myUserId
                    )
                }
            }
        }
    }

    /** Video sahifasi — pleyerga yuklanadi (avtomatik boshlanmaydi: spec'da markazda "play" tugmasi). */
    private fun preparePage(item: ViewerItem?) {
        if (item == null || item.media.kind != MediaKind.VIDEO) {
            player.pause()
            return
        }
        if (loadedKey == item.key) return
        val uri = item.media.localPath?.let(::File)?.takeIf { it.exists() }?.let(Uri::fromFile)
            ?: item.media.url?.let(Uri::parse)
            ?: return
        loadedKey = item.key
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = false
    }

    /** Media'ni galereyaga (MediaStore) saqlaydi; fayl nomi yaratilgan vaqt va MIME kengaytmasidan tuziladi. */
    private fun save(item: ViewerItem) = intent {
        if (state.isSaving) return@intent
        reduce { state.copy(isSaving = true) }
        val extension = item.media.mimeType.substringAfter('/', "").substringBefore(';').ifEmpty { "bin" }
        val result = saveMediaToGallery(item.media, "SwiftChat_${item.message.createdAt}.$extension")
        reduce { state.copy(isSaving = false) }
        when (result) {
            is AppResult.Success -> postSideEffect(MediaViewerContract.SideEffect.Saved)
            is AppResult.Error -> postSideEffect(MediaViewerContract.SideEffect.ShowError(result.error))
        }
    }

    override fun onCleared() {
        player.release()
    }
}
