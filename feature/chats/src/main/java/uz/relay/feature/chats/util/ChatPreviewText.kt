package uz.relay.feature.chats.util

import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import uz.relay.domain.model.ChatType
import uz.relay.domain.model.LastMessage
import uz.relay.domain.model.MessageType
import uz.relay.domain.model.SystemEvent
import uz.relay.feature.chats.R

/** Preview matnidagi ranglar (tema'dan olinadi, bu yerda Compose'ga bog'lanmaslik uchun parametr). */
data class PreviewColors(
    /** Asosiy matn: "Siz: " / "Malika: " prefiksi. */
    val prefix: Color,
    /** Media yorlig'i ("Rasm", "Video", "Fayl"). */
    val highlight: Color
)

/**
 * Chatlar ro'yxatidagi ikkinchi qator (oxirgi xabar), spec 3.5 bo'yicha:
 *  - o'chirilgan xabar → kursiv "Xabar oʻchirildi" (prefikssiz);
 *  - SYSTEM → ismlar bilan tuzilgan gap ("Ali guruhni yaratdi");
 *  - o'zimniki → "Siz: " prefiksi; guruhda boshqaniki → "Malika: " prefiksi;
 *  - media → rangli "Rasm" / "Video" / "Fayl" (izohi bo'lsa — izohning o'zi).
 *
 * Qolgan qism rangini chaqiruvchi Text beradi (text2), bu yerda faqat alohida bo'laklar bo'yaladi.
 */
fun buildChatPreview(
    message: LastMessage,
    chatType: ChatType,
    names: Map<String, String>,
    resources: Resources,
    colors: PreviewColors
): AnnotatedString = buildAnnotatedString {
    if (message.isDeleted) {
        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(resources.getString(R.string.deleted)) }
        return@buildAnnotatedString
    }
    if (message.type == MessageType.SYSTEM) {
        message.systemEvent?.let { append(systemText(it, message.senderId, names, resources, message.isMine)) }
        return@buildAnnotatedString
    }

    val prefix = when {
        message.isMine -> resources.getString(R.string.you_prefix)
        chatType == ChatType.GROUP -> names[message.senderId]?.let { "$it: " }
        else -> null
    }
    prefix?.let { withStyle(SpanStyle(color = colors.prefix)) { append(it) } }

    val caption = message.text?.takeIf { it.isNotBlank() }
    val mediaLabel = when (message.type) {
        MessageType.IMAGE -> R.string.photo
        MessageType.VIDEO -> R.string.video
        MessageType.FILE -> R.string.file
        else -> null
    }
    when {
        caption != null -> append(caption.replace('\n', ' '))
        mediaLabel != null -> withStyle(SpanStyle(color = colors.highlight)) { append(resources.getString(mediaLabel)) }
    }
}

/**
 * SYSTEM xabar gapi. Ism keshda bo'lmasa — "Kimdir". O'zim bo'lsam — "Siz".
 * `members_added` da qo'shilgan odam o'zi bo'lsa (o'zi kirgan) — "Ali guruhga qoʻshildi".
 */
private fun systemText(
    event: SystemEvent,
    senderId: String,
    names: Map<String, String>,
    resources: Resources,
    actorIsMe: Boolean
): String {
    val someone = resources.getString(R.string.sys_someone)
    fun nameOf(userId: String) = names[userId] ?: someone
    val actor = if (actorIsMe && event.actorId == senderId) resources.getString(R.string.sys_you) else nameOf(event.actorId)
    val targets = event.targetUserIds.joinToString(", ") { nameOf(it) }

    return when (event.event) {
        "group_created" -> resources.getString(R.string.sys_group_created, actor)
        "members_added" ->
            if (event.targetUserIds == listOf(event.actorId)) resources.getString(R.string.sys_member_joined, actor)
            else resources.getString(R.string.sys_members_added, actor, targets)
        "member_removed" -> resources.getString(R.string.sys_member_removed, actor, targets)
        "member_left" -> resources.getString(R.string.sys_member_left, actor)
        // Kelajakdagi yangi hodisa turi — bo'sh qoldiramiz, ilova yiqilmaydi.
        else -> ""
    }
}
