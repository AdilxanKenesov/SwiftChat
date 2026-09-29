package uz.relay.core.common.result

/**
 * Repository natijasi: muvaffaqiyat ([Success]) yoki xato ([Error]).
 *
 * Nega exception emas: repository'lar xatoni otmaydi, balki qiymat sifatida qaytaradi. Shunda chaqiruvchi
 * ikkala holatni `when` bilan majburan ko'rib chiqadi, xato "unutilib" ilovani yiqitmaydi va coroutine
 * bekor qilinishi (CancellationException) oddiy xatolar bilan aralashmaydi.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()
}

/** Muvaffaqiyatli qiymatni o'zgartiradi; xato o'zgarishsiz o'tadi. */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}

/** Muvaffaqiyat bo'lsa [action] bajariladi; natijaning o'zi qaytadi (zanjir qilish uchun). */
inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

/** Xato bo'lsa [action] bajariladi; natijaning o'zi qaytadi (zanjir qilish uchun). */
inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}
