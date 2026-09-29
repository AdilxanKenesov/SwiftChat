package uz.relay.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.relay.data.repository_impl.AuthRepositoryImpl
import uz.relay.data.repository_impl.ChatRepositoryImpl
import uz.relay.data.repository_impl.ConnectionRepositoryImpl
import uz.relay.data.repository_impl.GroupRepositoryImpl
import uz.relay.data.repository_impl.MessageRepositoryImpl
import uz.relay.data.realtime.TypingTracker
import uz.relay.data.repository_impl.SettingsRepositoryImpl
import uz.relay.data.repository_impl.UserRepositoryImpl
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.ChatRepository
import uz.relay.domain.repository.ConnectionRepository
import uz.relay.domain.repository.GroupRepository
import uz.relay.domain.repository.MessageRepository
import uz.relay.domain.repository.SettingsRepository
import uz.relay.domain.repository.TypingRepository
import uz.relay.domain.repository.UserRepository

@Module
@InstallIn(SingletonComponent::class)
internal interface RepositoryModule {

    @Binds
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    fun bindConnectionRepository(impl: ConnectionRepositoryImpl): ConnectionRepository

    /** TypingTracker @Singleton: frame'larni yozadigan va UI o'qiydigan nusxa bitta bo'lishi shart. */
    @Binds
    fun bindTypingRepository(impl: TypingTracker): TypingRepository

    @Binds
    fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository

    @Binds
    fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
