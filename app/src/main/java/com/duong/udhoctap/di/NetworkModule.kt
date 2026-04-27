package com.duong.udhoctap.di

import com.duong.udhoctap.core.data.repository.KnowledgeBaseRepository
import com.duong.udhoctap.core.network.BackendApiService
import com.duong.udhoctap.core.network.DocumentApi
import com.duong.udhoctap.core.network.WebSocketManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "http://10.0.2.2:8001/"

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideDocumentApi(retrofit: Retrofit): DocumentApi =
        retrofit.create(DocumentApi::class.java)

    @Provides
    @Singleton
    fun provideBackendApiService(retrofit: Retrofit): BackendApiService =
        retrofit.create(BackendApiService::class.java)

    @Provides
    @Singleton
    fun provideWebSocketManager(okHttpClient: OkHttpClient): WebSocketManager =
        WebSocketManager(okHttpClient)

    // ── Repositories ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideKnowledgeBaseRepository(
        api: BackendApiService,
        okHttpClient: OkHttpClient
    ): KnowledgeBaseRepository = KnowledgeBaseRepository(api, okHttpClient)
}
