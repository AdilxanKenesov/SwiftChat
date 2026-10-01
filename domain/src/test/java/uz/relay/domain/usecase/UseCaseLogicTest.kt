package uz.relay.domain.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.relay.core.common.result.AppResult
import uz.relay.domain.model.Attachment
import uz.relay.domain.model.MuteDuration
import uz.relay.domain.testing.FakeChatRepository
import uz.relay.domain.testing.FakeMessageRepository
import uz.relay.domain.testing.FakeUserRepository
import uz.relay.domain.usecase.chat.SetChatMutedUseCase
import uz.relay.domain.usecase.media.SendMediaMessageUseCase
import uz.relay.domain.usecase.message.SearchMessagesUseCase
import uz.relay.domain.usecase.user.SearchUsersUseCase

/** Delegatsiyadan tashqari mantig'i bor use case'lar (qolganlari repository'ga to'g'ridan-to'g'ri uzatadi). */
class UseCaseLogicTest {

    @Test
    fun `user search strips at sign and skips blank queries`() = runTest {
        val users = FakeUserRepository()
        val search = SearchUsersUseCase(users)
        assertEquals(AppResult.Success(emptyList<Any>()), search("  @ "))
        search(" @vali ")
        assertEquals(listOf("vali"), users.searches)
    }

    @Test
    fun `message search trims and skips blank queries`() = runTest {
        val messages = FakeMessageRepository()
        assertTrue(SearchMessagesUseCase(messages)("chat", "   ").isEmpty())
    }

    @Test
    fun `mute duration becomes absolute end time and forever has none`() = runTest {
        val chats = FakeChatRepository()
        val mute = SetChatMutedUseCase(chats)
        val before = System.currentTimeMillis()
        mute("c1", MuteDuration.EIGHT_HOURS)
        mute("c1", MuteDuration.FOREVER)
        val after = System.currentTimeMillis()
        val until = chats.mutes[0].third!!
        assertTrue(until in (before + MuteDuration.EIGHT_HOURS.millis!!)..(after + MuteDuration.EIGHT_HOURS.millis!!))
        assertEquals(Triple("c1", true, null as Long?), chats.mutes[1])
    }

    @Test
    fun `media caption is trimmed and blank caption dropped`() = runTest {
        val messages = FakeMessageRepository()
        val send = SendMediaMessageUseCase(messages)
        val photo = uz.relay.domain.model.Attachment("content://p", asFile = false)
        send("c1", photo, "  ", null)
        send("c1", photo, " Mana ", null)
        assertEquals(listOf(photo to null, photo to "Mana"), messages.sentMedia)
    }
}
