package uz.relay.feature.conversation.chat

import uz.relay.domain.model.Message
import uz.relay.domain.model.MessageType
import uz.relay.feature.conversation.util.dayStartMillis

/**
 * Suhbat ro'yxatidagi bitta element. [key] — LazyColumn uchun barqaror kalit (animatsiya va scroll to'g'ri ishlashi uchun).
 *
 * Nega xabarlar to'g'ridan-to'g'ri emas, ChatItem sifatida beriladi: sana ajratgichlari va "ketma-ketlik"
 * (ism/avatar qachon ko'rinishi) ViewModel'da bir marta hisoblanadi. Shunda Composable'lar faqat tayyor
 * ma'lumotni chizadi, har recomposition'da qo'shni xabarlarni solishtirmaydi va mantiq alohida test qilinadi.
 */
sealed interface ChatItem {
    val key: String

    /** "Bugun" / "Kecha" / "24-sentabr" chip'i — shu kunning birinchi xabari ustida. */
    data class DateSeparator(val dayStart: Long) : ChatItem {
        override val key: String get() = "date-$dayStart"
    }

    /** Markazdagi SYSTEM chip'i ("Ali guruhni yaratdi"). */
    data class System(val message: Message) : ChatItem {
        override val key: String get() = message.clientMessageId
    }

    /** Oddiy xabar bubble'i va uni chizish uchun oldindan hisoblangan bayroqlar. */
    data class Bubble(
        val message: Message,
        /** Javob berilgan xabar (bazada bo'lsa) — bubble ichidagi iqtibos uchun. */
        val replied: Message?,
        /** Guruhda: bir yuboruvchining ketma-ket xabarlaridan BIRINCHIsida ism ko'rsatiladi. */
        val showSenderName: Boolean,
        /** Guruhda: ketma-ketlikning OXIRGIsida avatar, qolganlarida o'rniga bo'sh joy. */
        val showAvatar: Boolean
    ) : ChatItem {
        override val key: String get() = message.clientMessageId
    }
}

/**
 * Xabarlardan ro'yxat elementlarini tuzadi (sof funksiya — test qilish oson).
 *
 * @param messages eng yangisi birinchi (LazyColumn `reverseLayout = true` bilan pastdan chizadi).
 * @return ham eng yangisi birinchi. Sana ajratgichi har kunning ENG ESKI xabaridan keyin qo'yiladi —
 * teskari ro'yxatda u o'sha kun xabarlarining ustida ko'rinadi.
 */
fun buildChatItems(messages: List<Message>, isGroup: Boolean): List<ChatItem> {
    val byId = messages.associateBy { it.clientMessageId }
    val items = ArrayList<ChatItem>(messages.size + 8)

    messages.forEachIndexed { index, message ->
        val newer = messages.getOrNull(index - 1)
        val older = messages.getOrNull(index + 1)
        val day = dayStartMillis(message.createdAt)

        if (message.type == MessageType.SYSTEM) {
            items += ChatItem.System(message)
        } else {
            items += ChatItem.Bubble(
                message = message,
                replied = message.replyToClientMessageId?.let(byId::get),
                // Ekranda yuqoridagi (eskiroq) xabar boshqa odamniki yoki boshqa kun — ketma-ketlik shu yerdan boshlanadi.
                showSenderName = isGroup && !message.isMine && !sameRun(message, older),
                // Ekranda pastdagi (yangiroq) xabar boshqa odamniki yoki boshqa kun — ketma-ketlik shu yerda tugaydi.
                showAvatar = isGroup && !message.isMine && !sameRun(message, newer)
            )
        }

        if (older == null || dayStartMillis(older.createdAt) != day) {
            items += ChatItem.DateSeparator(day)
        }
    }
    return items
}

/** Ikki xabar bitta "ketma-ketlik"danmi: bir yuboruvchi, bir kun va ikkalasi ham oddiy xabar. */
private fun sameRun(message: Message, other: Message?): Boolean =
    other != null &&
        other.type != MessageType.SYSTEM &&
        other.senderId == message.senderId &&
        dayStartMillis(other.createdAt) == dayStartMillis(message.createdAt)
