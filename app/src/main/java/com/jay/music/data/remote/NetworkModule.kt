package com.jay.music.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jay.music.BuildConfig
import com.jay.music.core.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层装配：后端 Retrofit + 第三方音乐 OkHttp。
 * 所有后端请求都会自动带上 Authorization: Bearer {token}
 */
class NetworkModule(
    private val sessionManager: SessionManager,
    baseUrl: String
) {
    val gson: Gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    /** 通用 OkHttp 客户端（第三方接口使用） */
    val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .build()

    /** 后端专用客户端：注入 Bearer Token */
    private val backendClient: OkHttpClient = baseClient.newBuilder()
        .addInterceptor { chain ->
            val token = sessionManager.token.value
            val request = if (token.isNullOrBlank()) {
                chain.request()
            } else {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            }
            chain.proceed(request)
        }
        .build()

    val api: JayMusicApiService = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(backendClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(JayMusicApiService::class.java)

    /** 第三方音乐接口数据源 */
    val musicSource: MusicRemoteDataSource = MusicRemoteDataSource(gson, baseClient)
}
