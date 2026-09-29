package uz.relay.data.source.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import uz.relay.data.source.local.database.dao.ChatDao
import uz.relay.data.source.local.database.dao.MemberCursorDao
import uz.relay.data.source.local.database.dao.MessageDao
import uz.relay.data.source.local.database.dao.SyncStateDao
import uz.relay.data.source.local.database.dao.UserDao
import uz.relay.data.source.local.database.entity.ChatEntity
import uz.relay.data.source.local.database.entity.MemberCursorEntity
import uz.relay.data.source.local.database.entity.MessageEntity
import uz.relay.data.source.local.database.entity.SyncStateEntity
import uz.relay.data.source.local.database.entity.UserEntity

/**
 * Ilovaning lokal bazasi — offline-first'ning markazi: UI faqat shu yerdan o'qiydi.
 *
 * `exportSchema = false`: ishlab chiqish bosqichida sxema tez-tez o'zgaradi va migratsiya yozilmaydi
 * (DatabaseModule'dagi destructive migration'ga qarang). Reliz oldidan sxema eksport qilinib,
 * haqiqiy migratsiyalar yoziladi.
 */
@Database(
    entities = [
        ChatEntity::class,
        UserEntity::class,
        MemberCursorEntity::class,
        SyncStateEntity::class,
        MessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RelayDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao
    abstract fun memberCursorDao(): MemberCursorDao
    abstract fun syncStateDao(): SyncStateDao
    abstract fun messageDao(): MessageDao
}
