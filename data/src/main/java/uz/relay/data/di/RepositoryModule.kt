package uz.relay.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import uz.relay.data.repository_impl.AuthRepositoryImpl
import uz.relay.data.repository_impl.ChatRepositoryImpl
import uz.relay.data.repository_impl.ConnectionRepositoryImpl
import uz.relay.data.repository_impl.ContactRepositoryImpl
import uz.relay.data.repository_impl.GroupRepositoryImpl
import uz.relay.data.repository_impl.MediaRepositoryImpl
import uz.relay.data.repository_impl.MessageRepositoryImpl
import uz.relay.data.realtime.TypingTracker
import uz.relay.data.repository_impl.SettingsRepositoryImpl
import uz.relay.data.repository_impl.UserRepositoryImpl
import uz.relay.domain.repository.AuthRepository
import uz.relay.domain.repository.ChatRepository
import uz.relay.domain.repository.ConnectionRepository
import uz.relay.domain.repository.ContactRepository
import uz.relay.domain.repository.GroupRepository
import uz.relay.domain.repository.MediaRepository
import uz.relay.domain.repository.MessageRepository
import uz.relay.domain.repository.SettingsRepository
import uz.relay.domain.repository.TypingRepository
import uz.relay.domain.repository.UserRepository

/**
 * Domain qatlamidagi repository interfeyslarini data qatlamidagi implementatsiyalarga bog'laydi.
 *
 * Nega @Binds: implementatsiyalar @Inject konstruktorli, shuning uchun qo'shimcha kod generatsiyasiz
 * faqat "interfeys -> klass" bog'lanishi kerak. Feature modullar faqat domain interfeyslarini ko'radi,
 * data qatlami esa `internal` bo'lib yashirin qoladi (Clean Architecture chegarasi).
 */
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

    @Binds
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
}
