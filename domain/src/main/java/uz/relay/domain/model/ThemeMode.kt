package uz.relay.domain.model

/**
 * Ilova temasi: telefon sozlamasiga ergashish ([SYSTEM]), doim kunduzgi yoki doim tungi.
 * Tanlanmagan bo'lsa — [LIGHT] (oldingi versiyalardagi standart; mavjud foydalanuvchilarda tema o'zgarib qolmasin).
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }
