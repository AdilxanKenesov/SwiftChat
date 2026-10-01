package uz.relay.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guruh ruxsatlari server qoidalari bilan bir xil — UI keraksiz tugmalarni ko'rsatmasligi shunga tayanadi. */
class GroupPermissionsTest {

    private fun member(role: MemberRole, isMe: Boolean = false) =
        ChatMember(userId = role.name + isMe, displayName = null, role = role, online = false, lastSeenAt = null, isMe = isMe)

    @Test
    fun `only owner and admin can manage`() {
        assertTrue(GroupPermissions.canManage(MemberRole.OWNER))
        assertTrue(GroupPermissions.canManage(MemberRole.ADMIN))
        assertFalse(GroupPermissions.canManage(MemberRole.MEMBER))
        assertFalse(GroupPermissions.canManage(MemberRole.UNKNOWN))
        assertFalse(GroupPermissions.canManage(null))
    }

    @Test
    fun `only owner changes roles and never of owner or self`() {
        assertTrue(GroupPermissions.canChangeRole(MemberRole.OWNER, member(MemberRole.MEMBER)))
        assertTrue(GroupPermissions.canChangeRole(MemberRole.OWNER, member(MemberRole.ADMIN)))
        assertFalse(GroupPermissions.canChangeRole(MemberRole.OWNER, member(MemberRole.OWNER)))
        assertFalse(GroupPermissions.canChangeRole(MemberRole.OWNER, member(MemberRole.MEMBER, isMe = true)))
        assertFalse(GroupPermissions.canChangeRole(MemberRole.ADMIN, member(MemberRole.MEMBER)))
    }

    @Test
    fun `owner removes anyone except owner while admin removes only members`() {
        assertTrue(GroupPermissions.canRemove(MemberRole.OWNER, member(MemberRole.ADMIN)))
        assertTrue(GroupPermissions.canRemove(MemberRole.OWNER, member(MemberRole.MEMBER)))
        assertTrue(GroupPermissions.canRemove(MemberRole.ADMIN, member(MemberRole.MEMBER)))
        assertFalse(GroupPermissions.canRemove(MemberRole.ADMIN, member(MemberRole.ADMIN)))
        assertFalse(GroupPermissions.canRemove(MemberRole.ADMIN, member(MemberRole.OWNER)))
        assertFalse(GroupPermissions.canRemove(MemberRole.MEMBER, member(MemberRole.MEMBER)))
    }

    @Test
    fun `nobody removes themselves through remove action`() {
        // O'zini chiqarish — "Guruhdan chiqish" orqali, "olib tashlash" emas.
        assertFalse(GroupPermissions.canRemove(MemberRole.OWNER, member(MemberRole.MEMBER, isMe = true)))
    }

    @Test
    fun `deleting others messages follows manage permission`() {
        assertTrue(GroupPermissions.canDeleteOthersMessages(MemberRole.ADMIN))
        assertFalse(GroupPermissions.canDeleteOthersMessages(MemberRole.MEMBER))
    }
}
