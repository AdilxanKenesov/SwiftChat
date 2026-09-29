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
