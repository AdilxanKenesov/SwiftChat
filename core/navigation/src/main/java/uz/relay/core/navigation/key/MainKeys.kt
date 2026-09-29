package uz.relay.core.navigation.key

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Ilovaning asosiy qismi ekranlari kalitlari (login'dan keyin).

/** Chatlar ro'yxati — asosiy ekran. */
@Serializable
data object ChatsKey : NavKey

/**
 * Suhbat ekrani. Hamma ma'lumot bazadan `chatId` bo'yicha o'qiladi — kalitda faqat id.
 * [focusMessageId] — chat ichidagi qidiruvdan kelinganda shu xabarga scroll qilinadi.
 */
@Serializable
data class ChatKey(val chatId: String, val focusMessageId: String? = null) : NavKey

/** Username bo'yicha odam qidirish (yangi chat / yangi guruh). */
@Serializable
data object SearchKey : NavKey

/** [addToChatId] `null` — yangi guruh (2 qadam); aks holda mavjud guruhga a'zo qo'shish (1 qadam). */
@Serializable
data class GroupCreateKey(val addToChatId: String? = null) : NavKey

/** Guruh ma'lumoti: a'zolar, nom, ovozsiz qilish, chiqish. */
@Serializable
data class GroupInfoKey(val chatId: String) : NavKey

/** Chat ichida (qurilmadagi xabarlar bo'yicha) qidiruv. */
@Serializable
data class ChatSearchKey(val chatId: String) : NavKey

/** Mening profilim va sozlamalar (tema, bildirishnomalar, chiqish). */
@Serializable
data object MyProfileKey : NavKey

/** Ism va username'ni tahrirlash. */
@Serializable
data object EditProfileKey : NavKey

/** Boshqa foydalanuvchining profili. */
@Serializable
data class UserProfileKey(val userId: String) : NavKey

/** Rasm/video ko'ruvchi: shu chatdagi hamma rasm va videolar orasida suriladi, [clientMessageId] dan boshlanadi. */
@Serializable
data class MediaViewerKey(val chatId: String, val clientMessageId: String) : NavKey
