package uz.relay.data.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Media'ni KO'RSATISH uchun vositalar (Coil rasm yuklovchi va Media3/ExoPlayer tarmoq manbai).
 *
 * Ikkalasi ham [MediaClient] ustida: `GET /v1/media/{id}` token talab qiladi, token eskirsa authenticator
 * uni yangilaydi — feature modullar bu haqda hech narsa bilmaydi. Logging'siz klient ishlatiladi, chunki
 * BODY logger katta faylni xotiraga to'liq o'qib olardi.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    /** Ilova bo'yicha yagona Coil ImageLoader (App uni SingletonImageLoader sifatida o'rnatadi). */
    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context, @MediaClient client: OkHttpClient): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()

    /**
     * ExoPlayer uchun tarmoq manbai: `Range` so'rovlari bilan videoni oxirigacha yuklamasdan ijro etadi
     * va istalgan joyga o'tkazadi (server 206 qo'llaydi).
     */
    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideMediaDataSourceFactory(@MediaClient client: OkHttpClient): DataSource.Factory = OkHttpDataSource.Factory(client)
}
