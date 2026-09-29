package uz.relay.feature.conversation.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.conversation.chat.ChatContract
import uz.relay.feature.conversation.chat.ChatDirectionsImpl
import uz.relay.feature.conversation.search.ChatSearchContract
import uz.relay.feature.conversation.search.ChatSearchDirectionsImpl
import uz.relay.feature.conversation.viewer.MediaViewerContract
import uz.relay.feature.conversation.viewer.MediaViewerDirectionsImpl

/** Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun ViewModel doirasida. */
@Module
@InstallIn(ViewModelComponent::class)
internal interface ConversationDirectionsModule {

    @Binds
    fun bindChatDirections(impl: ChatDirectionsImpl): ChatContract.Directions

    @Binds
    fun bindChatSearchDirections(impl: ChatSearchDirectionsImpl): ChatSearchContract.Directions

    @Binds
    fun bindMediaViewerDirections(impl: MediaViewerDirectionsImpl): MediaViewerContract.Directions
}
