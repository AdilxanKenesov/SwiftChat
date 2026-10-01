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

/**
 * Room bazasi ([RelayDatabase]) va uning DAO'larini Hilt'ga beradi.
 *
 * Nega Room: ilova offline-first — UI faqat lokal bazadan o'qiydi (Flow orqali), tarmoq/sync esa bazaga
 * yozadi. Shunda internet bo'lmasa ham chatlar ko'rinadi va yangilanishlar avtomatik UI'ga yetib boradi.
 * Baza @Singleton (bitta ulanish havzasi); DAO'lar arzon getter bo'lgani uchun scope'siz beriladi.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RelayDatabase =
        Room.databaseBuilder(context, RelayDatabase::class.java, "relay.db")
            // Ishlab chiqish bosqichida: sxema o'zgarsa baza tozalanadi. Bu xavfsiz, chunki hamma
            // ma'lumot serverda bor — keyingi sync uni qayta yuklaydi.
            // Downgrade (eskiroq build yangi baza ustiga) ham shu bilan qamrab olinadi. DIQQAT: alohida
            // fallbackToDestructiveMigrationOnDowngrade() QO'SHILMASIN — u requireMigration = true qilib, upgrade'da
            // destructive migration'ni o'chirib qo'yadi ("A migration from 5 to 6 was required" crash'i shundan bo'lgan).
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

    @Provides
    fun provideChatMemberDao(database: RelayDatabase) = database.chatMemberDao()

    @Provides
    fun provideUploadDao(database: RelayDatabase) = database.uploadDao()

    @Provides
    fun provideContactDao(database: RelayDatabase) = database.contactDao()
}
