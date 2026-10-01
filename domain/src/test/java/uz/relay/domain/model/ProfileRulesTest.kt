package uz.relay.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Klientdagi tekshiruv server qoidasi bilan bir xil bo'lishi kerak — aks holda server 400 qaytaradi. */
class ProfileRulesTest {

    @Test
    fun `username accepts latin letters digits and underscore within 3 to 32`() {
        assertTrue(ProfileRules.isUsernameValid("ali"))
        assertTrue(ProfileRules.isUsernameValid("Ali_Valiyev_2024"))
        assertTrue(ProfileRules.isUsernameValid("a".repeat(32)))
    }

    @Test
    fun `username rejects too short too long and other characters`() {
        assertFalse(ProfileRules.isUsernameValid("al"))
        assertFalse(ProfileRules.isUsernameValid("a".repeat(33)))
        assertFalse(ProfileRules.isUsernameValid("ali valiyev"))
        assertFalse(ProfileRules.isUsernameValid("ali-valiyev"))
        assertFalse(ProfileRules.isUsernameValid("алишер"))
        assertFalse(ProfileRules.isUsernameValid("ali@"))
    }

    @Test
    fun `name is trimmed and must be 1 to 128 characters`() {
        assertTrue(ProfileRules.isNameValid("A"))
        assertTrue(ProfileRules.isNameValid("  Ali  "))
        assertTrue(ProfileRules.isNameValid("x".repeat(128)))
        assertFalse(ProfileRules.isNameValid("   "))
        assertFalse(ProfileRules.isNameValid(""))
        assertFalse(ProfileRules.isNameValid("x".repeat(129)))
    }
}
