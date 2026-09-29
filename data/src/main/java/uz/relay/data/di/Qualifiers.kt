package uz.relay.data.di

import javax.inject.Qualifier

/** Retrofit/OkHttp without a token (OTP, refresh). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

/** Retrofit/OkHttp that adds the token and refreshes it on 401. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthorizedClient

/**
 * Media uchun (yuklash/yuklab olish, Coil, ExoPlayer): token va refresh xuddi [AuthorizedClient] dagidek,
 * lekin logging'siz — BODY darajadagi logger 100 MB faylni xotiraga to'liq o'qib olardi.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MediaClient
