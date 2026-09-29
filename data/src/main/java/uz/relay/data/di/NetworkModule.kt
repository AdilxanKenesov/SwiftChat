package uz.relay.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import uz.relay.data.BuildConfig
import uz.relay.data.source.network.interceptor.TokenAuthenticator
import uz.relay.data.source.network.interceptor.TokenInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // The contract only grows (Changelog): unknown fields must not break parsing.
        ignoreUnknownKeys = true
        // PATCH bodies send only the fields that are set.
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideLogging(): HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    @Provides
    @Singleton
    @PublicClient
    fun providePublicOkHttp(logging: HttpLoggingInterceptor): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    /** Built from the public client, so both share one connection pool and dispatcher. */
    @Provides
    @Singleton
    @AuthorizedClient
    fun provideAuthorizedOkHttp(
        @PublicClient base: OkHttpClient,
        tokenInterceptor: TokenInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = base.newBuilder()
        // Before logging, so the logged request carries the (redacted) header.
        .apply { interceptors().add(0, tokenInterceptor) }
        .authenticator(tokenAuthenticator)
        .build()

    @Provides
    @Singleton
    @PublicClient
    fun providePublicRetrofit(@PublicClient client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @AuthorizedClient
    fun provideAuthorizedRetrofit(@AuthorizedClient client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json)

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
