package uz.relay.data.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.relay.domain.model.MessageStatus

/**
 * ✓/✓✓ holati bazada saqlanmaydi, suhbatdoshlar kursoridan hisoblanadi — kursor bir marta ko'tarilsa, undan oldingi
 * barcha xabarlar birdaniga yangilanadi. Shu hisob to'g'riligi tekshiriladi.
 */
class OutgoingStatusTest {

    @Test
    fun `sending and failed do not depend on cursors`() {
        val peers = PeerCursors(readUpToSeq = 100, deliveredUpToSeq = 100)
        assertEquals(MessageStatus.SENDING, outgoingStatus(MessageStatus.SENDING, serverSeq = null, peers = peers))
        assertEquals(MessageStatus.FAILED, outgoingStatus(MessageStatus.FAILED, serverSeq = 5, peers = peers))
    }

    @Test
    fun `sent without server seq stays sent`() {
        assertEquals(MessageStatus.SENT, outgoingStatus(MessageStatus.SENT, serverSeq = null, peers = PeerCursors(10, 10)))
    }

    @Test
    fun `message above both cursors is sent`() {
        assertEquals(MessageStatus.SENT, outgoingStatus(MessageStatus.SENT, serverSeq = 11, peers = PeerCursors(5, 10)))
    }

    @Test
    fun `message at or below delivered cursor is delivered`() {
        assertEquals(MessageStatus.DELIVERED, outgoingStatus(MessageStatus.SENT, serverSeq = 10, peers = PeerCursors(5, 10)))
        assertEquals(MessageStatus.DELIVERED, outgoingStatus(MessageStatus.SENT, serverSeq = 6, peers = PeerCursors(5, 10)))
    }

    @Test
    fun `read wins over delivered`() {
        assertEquals(MessageStatus.READ, outgoingStatus(MessageStatus.SENT, serverSeq = 5, peers = PeerCursors(5, 10)))
        // O'qish kursori yetkazilishdan katta bo'lsa ham (eventlar tartibsiz kelsa) — o'qilgan.
        assertEquals(MessageStatus.READ, outgoingStatus(MessageStatus.SENT, serverSeq = 7, peers = PeerCursors(8, 3)))
    }
}
