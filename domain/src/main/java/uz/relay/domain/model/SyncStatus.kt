package uz.relay.domain.model

/**
 * Server bilan sinxronlash holati — chatlar ekrani sarlavhasi va skeleton/bo'sh holat uchun.
 *
 * @param isSyncing hozir bootstrap yoki catch-up ketyapti ("Yangilanmoqda…").
 * @param isBootstrapped lokal baza kamida bir marta to'liq to'ldirilgan. `false` bo'lsa va ro'yxat bo'sh
 * bo'lsa, bu "chatlar yo'q" degani emas — hali yuklanmagan (skeleton ko'rsatiladi).
 */
data class SyncStatus(
    val isSyncing: Boolean,
    val isBootstrapped: Boolean
)
