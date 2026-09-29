package uz.relay.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import uz.relay.data.source.network.api.AuthApi
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
}
