package uz.relay.data.di

import javax.inject.Qualifier

/*
 * Hilt qualifier'lari: bir xil turdagi (OkHttpClient/Retrofit) bir nechta nusxani farqlash uchun.
 * Har biri o'z maqsadiga ega — pastdagi KDoc'larga qarang.
 */

/** Tokensiz Retrofit/OkHttp (OTP, refresh). Refresh so'rovi token kutib aylanib qolmasligi uchun alohida. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

/** Token qo'shadigan va 401 kelganda uni yangilaydigan Retrofit/OkHttp — oddiy API chaqiruvlari uchun. */
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
