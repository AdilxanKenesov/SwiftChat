package uz.relay.feature.conversation.util

import android.content.res.Resources
import uz.relay.domain.model.SystemEvent
import uz.relay.feature.conversation.R

/**
 * SYSTEM xabar gapi (spec 3.8): "Ali guruhni yaratdi", "Ali Valini qo'shdi", "Vali guruhdan chiqdi"...
 * Server tayyor matn bermaydi — ismlar keshdan olinadi, yo'q bo'lsa "Kimdir". O'zim bo'lsam — "Siz".
 */
fun systemText(event: SystemEvent, myUserId: String?, names: Map<String, String>, resources: Resources): String {
    fun nameOf(userId: String) = when (userId) {
        myUserId -> resources.getString(R.string.sys_you)
        else -> names[userId] ?: resources.getString(R.string.sys_someone)
    }
    val actor = nameOf(event.actorId)
    val targets = event.targetUserIds.joinToString(", ") { nameOf(it) }

    return when (event.event) {
        "group_created" -> resources.getString(R.string.sys_group_created, actor)
        "members_added" ->
            // O'zi qo'shilgan bo'lsa (actor == target) — "Ali guruhga qo'shildi".
            if (event.targetUserIds == listOf(event.actorId)) resources.getString(R.string.sys_member_joined, actor)
            else resources.getString(R.string.sys_members_added, actor, targets)
        "member_removed" -> resources.getString(R.string.sys_member_removed, actor, targets)
        "member_left" -> resources.getString(R.string.sys_member_left, actor)
        // Kelajakdagi yangi hodisa turi — bo'sh qoldiramiz, ilova yiqilmaydi.
        else -> ""
    }
}
