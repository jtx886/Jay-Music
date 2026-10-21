package com.jay.music.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.jay.music.data.model.User

/**
 * 敏感信息存储：
 * - token、用户信息全部写入 EncryptedSharedPreferences（AES256-GCM）
 * - 绝不使用明文 SharedPreferences 保存账号密码或 token
 * - 若设备不支持加密存储，则退化为进程内内存存储（不落盘），保证不泄露敏感数据
 */
class SecureStorage(context: Context) {

    private val gson = Gson()
    private val appContext = context.applicationContext

    private var prefs: SharedPreferences? = runCatching {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()

    // 加密存储不可用时的内存兜底（绝不写入磁盘）
    private var memoryToken: String? = null
    private var memoryUser: String? = null

    var token: String?
        get() = prefs?.getString(KEY_TOKEN, null) ?: memoryToken
        set(value) {
            memoryToken = value
            prefs?.edit()?.apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }?.apply()
        }

    var user: User?
        get() {
            val json = prefs?.getString(KEY_USER, null) ?: memoryUser ?: return null
            return runCatching { gson.fromJson(json, User::class.java) }.getOrNull()
        }
        set(value) {
            val json = value?.let { gson.toJson(it) }
            memoryUser = json
            prefs?.edit()?.apply {
                if (json == null) remove(KEY_USER) else putString(KEY_USER, json)
            }?.apply()
        }

    fun saveSession(token: String, user: User) {
        this.token = token
        this.user = user
    }

    /** 退出登录，只清敏感数据 */
    fun clear() {
        token = null
        user = null
    }

    companion object {
        private const val FILE_NAME = "jay_music_secure_store"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "auth_user"
    }
}
