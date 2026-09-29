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

/**
 * Tarmoq qatlami: [Json], OkHttp klientlari va Retrofit nusxalari.
 *
 * Uchta klient bor, chunki talablar har xil:
 * - [PublicClient] — tokensiz (OTP, refresh). Refresh so'rovi o'zi token so'rab qolmasligi uchun.
 * - [AuthorizedClient] — [TokenInterceptor] token qo'shadi, [TokenAuthenticator] 401 da yangilaydi.
 * - [MediaClient] — xuddi authorized, lekin logger'siz va uzunroq timeout'lar bilan (katta fayllar).
 * Nega Retrofit + kotlinx.serialization: API'lar deklarativ interfeys bo'lib qoladi, JSON esa
 * reflection'siz, kompilyatsiya vaqtida yaratilgan serializer'lar bilan parse qilinadi.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        // Server kontrakti faqat kengayadi (Changelog): noma'lum maydonlar parse'ni buzmasligi kerak.
        ignoreUnknownKeys = true
        // PATCH body'larida faqat qiymat berilgan maydonlar yuboriladi (null'lar tashlab ketiladi).
        explicitNulls = false
    }

    /** Faqat debug build'da BODY logi; `Authorization` sarlavhasi logda yashiriladi (token sizmasligi uchun). */
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

    /** Public klientdan quriladi — ikkalasi bitta connection pool va dispatcher'ni bo'lishadi (resurs tejaladi). */
    @Provides
    @Singleton
    @AuthorizedClient
    fun provideAuthorizedOkHttp(
        @PublicClient base: OkHttpClient,
        tokenInterceptor: TokenInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = base.newBuilder()
        // Logger'dan oldin qo'yiladi — shunda logdagi so'rovda (yashirilgan) token sarlavhasi ham ko'rinadi.
        .apply { interceptors().add(0, tokenInterceptor) }
        .authenticator(tokenAuthenticator)
        .build()

    /**
     * Uzoq davom etadigan oqimlar (upload/download) uchun: timeout'lar kattaroq, logger olib tashlangan.
     * Authorized klientdan quriladi, shuning uchun token va 401 da refresh mexanizmi saqlanib qoladi.
     */
    @Provides
    @Singleton
    @MediaClient
    fun provideMediaOkHttp(@AuthorizedClient authorized: OkHttpClient): OkHttpClient = authorized.newBuilder()
        .apply { interceptors().removeAll { it is HttpLoggingInterceptor } }
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @MediaClient
    fun provideMediaRetrofit(@MediaClient client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @PublicClient
    fun providePublicRetrofit(@PublicClient client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    @AuthorizedClient
    fun provideAuthorizedRetrofit(@AuthorizedClient client: OkHttpClient, json: Json): Retrofit =
        retrofit(client, json)

    /** Uchala Retrofit uchun umumiy quruvchi: bir xil base URL va JSON converter. */
    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
