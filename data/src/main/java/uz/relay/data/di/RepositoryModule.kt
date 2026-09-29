package uz.relay.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.relay.data.repository_impl.AuthRepositoryImpl
import uz.relay.data.repository_impl.UserRepositoryImpl
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.UserRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {

    @Binds
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
