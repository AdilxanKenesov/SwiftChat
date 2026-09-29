package uz.relay.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import uz.relay.data.source.local.database.RelayDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RelayDatabase =
        Room.databaseBuilder(context, RelayDatabase::class.java, "relay.db")
            // Ishlab chiqish bosqichida: sxema o'zgarsa baza tozalanadi. Bu xavfsiz, chunki hamma
            // ma'lumot serverda bor — keyingi sync uni qayta yuklaydi.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideChatDao(database: RelayDatabase) = database.chatDao()

    @Provides
    fun provideUserDao(database: RelayDatabase) = database.userDao()

    @Provides
    fun provideMemberCursorDao(database: RelayDatabase) = database.memberCursorDao()

    @Provides
    fun provideSyncStateDao(database: RelayDatabase) = database.syncStateDao()

    @Provides
    fun provideMessageDao(database: RelayDatabase) = database.messageDao()
}
