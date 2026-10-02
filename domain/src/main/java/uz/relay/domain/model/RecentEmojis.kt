package uz.relay.domain.model

/**
 * Emoji panelidagi "So'nggi" bo'limi qoidasi (Telegram'dagidek): oxirgi tanlangan emoji birinchi, takrorlanmaydi,
 * eng ko'pi [MAX] ta. Sof funksiya — saqlash joyidan qat'i nazar bir xil ishlaydi va oson test qilinadi.
 */
object RecentEmojis {
    const val MAX = 24

    fun push(current: List<String>, emoji: String): List<String> =
        (listOf(emoji) + current.filterNot { it == emoji }).take(MAX)
}
