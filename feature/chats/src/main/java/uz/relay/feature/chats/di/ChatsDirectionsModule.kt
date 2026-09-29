package uz.relay.feature.chats.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import uz.relay.feature.chats.list.ChatsContract
import uz.relay.feature.chats.list.ChatsDirectionsImpl
import uz.relay.feature.chats.search.SearchContract
import uz.relay.feature.chats.search.SearchDirectionsImpl

/**
 * Directions interfeyslarini ularning Impl'lariga bog'laydi (@Binds — qo'shimcha kod generatsiyasiz eng arzon usul).
 * Directions faqat ViewModel'larga kerak va holatsiz — shuning uchun ViewModelComponent doirasida.
 */
@Module
@InstallIn(ViewModelComponent::class)
internal interface ChatsDirectionsModule {

    @Binds
    fun bindChatsDirections(impl: ChatsDirectionsImpl): ChatsContract.Directions

    @Binds
    fun bindSearchDirections(impl: SearchDirectionsImpl): SearchContract.Directions
}
