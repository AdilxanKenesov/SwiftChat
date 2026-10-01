package uz.relay.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Qo'ng'iroq yozuvi chatda oddiy matn bo'lib saqlanadi — format o'zgarsa eski xabarlar "oddiy matn" bo'lib
 * qolib ketadi. Shuning uchun yozish va o'qish bir-biriga mosligi (round-trip) va eski formatlar tekshiriladi.
 */
class CallLogFormatTest {

    @Test
    fun `audio call with duration is formatted in readable english`() {
        val text = CallLogFormat.format(CallLog(video = false, outcome = CallOutcome.ANSWERED, durationSeconds = 151))
        assertEquals("📞 Call · audio · 2:31", text)
    }

    @Test
    fun `every outcome survives format and parse round trip`() {
        val logs = listOf(
            CallLog(video = true, outcome = CallOutcome.ANSWERED, durationSeconds = 3723),
            CallLog(video = false, outcome = CallOutcome.MISSED, durationSeconds = 0),
            CallLog(video = true, outcome = CallOutcome.DECLINED, durationSeconds = 0),
            CallLog(video = false, outcome = CallOutcome.CANCELED, durationSeconds = 0),
            CallLog(video = true, outcome = CallOutcome.STARTED, durationSeconds = 0, group = true),
            CallLog(video = true, outcome = CallOutcome.ANSWERED, durationSeconds = 754, group = true)
        )
        logs.forEach { log -> assertEquals(log, CallLogFormat.parse(CallLogFormat.format(log))) }
    }

    @Test
    fun `group call log uses its own prefix`() {
        val text = CallLogFormat.format(CallLog(video = true, outcome = CallOutcome.STARTED, durationSeconds = 0, group = true))
        assertEquals("📞 Group call · video · started", text)
    }

    @Test
    fun `hours are parsed from h mm ss`() {
        assertEquals(3723L, CallLogFormat.parse("📞 Call · video · 1:02:03")?.durationSeconds)
    }

    @Test
    fun `started is only valid for group calls`() {
        // 1:1 qo'ng'iroqda "boshlandi" holati yo'q — bunday matn oddiy xabar sifatida ko'rsatiladi.
        assertNull(CallLogFormat.parse("📞 Call · video · started"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertEquals(CallOutcome.MISSED, CallLogFormat.parse("  📞 Call · audio · missed \n")?.outcome)
    }

    @Test
    fun `ordinary text and null are not call logs`() {
        assertNull(CallLogFormat.parse(null))
        assertNull(CallLogFormat.parse("Salom"))
        assertNull(CallLogFormat.parse("📞 Call · audio · 2:31 qo'ng'iroq qildim"))
        assertNull(CallLogFormat.parse("📞 Call · fax · missed"))
    }

    @Test
    fun `duration pads seconds and minutes`() {
        assertEquals("0:00", CallLogFormat.duration(0))
        assertEquals("0:07", CallLogFormat.duration(7))
        assertEquals("10:00", CallLogFormat.duration(600))
        assertEquals("1:00:05", CallLogFormat.duration(3605))
    }

    @Test
    fun `negative duration is shown as zero`() {
        assertEquals("0:00", CallLogFormat.duration(-5))
    }
}
