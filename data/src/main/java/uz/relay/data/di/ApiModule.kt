package uz.relay.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import uz.relay.data.source.network.api.AuthApi
import uz.relay.data.source.network.api.ChatApi
import uz.relay.data.source.network.api.MessageApi
import uz.relay.data.source.network.api.SessionApi
import uz.relay.data.source.network.api.SyncApi
import uz.relay.data.source.network.api.UserApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(@PublicClient retrofit: Retrofit): AuthApi = retrofit.create()

    @Provides
    @Singleton
    fun provideUserApi(@AuthorizedClient retrofit: Retrofit): UserApi = retrofit.create()

    @Provides
    @Singleton
    fun provideChatApi(@AuthorizedClient retrofit: Retrofit): ChatApi = retrofit.create()

    @Provides
    @Singleton
    fun provideMessageApi(@AuthorizedClient retrofit: Retrofit): MessageApi = retrofit.create()

    @Provides
    @Singleton
    fun provideSyncApi(@AuthorizedClient retrofit: Retrofit): SyncApi = retrofit.create()

    @Provides
    @Singleton
    fun provideSessionApi(@AuthorizedClient retrofit: Retrofit): SessionApi = retrofit.create()
}
