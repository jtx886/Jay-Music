package com.jay.music.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jay.music.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * 网络层装配：仅保留第三方音乐接口所需 OkHttp 客户端。
 * 本应用无自有后端，账号、收藏与历史均在本地持久化。
 */
class NetworkModule {

    val gson: Gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    val baseClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(loggingInterceptor)
        .build()

    /** 第三方音乐接口数据源 */
    val musicSource: MusicRemoteDataSource = MusicRemoteDataSource(gson, baseClient)
}
