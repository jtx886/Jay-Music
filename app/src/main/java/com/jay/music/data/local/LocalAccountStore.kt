package com.jay.music.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 本机账号存储（无后端方案）：
 * - 用户名与密码哈希写入 EncryptedSharedPreferences（AES256-GCM）
 * - 密码使用 PBKDF2 + 随机盐做哈希，绝不明文落盘
 * - 设备不支持加密存储时退化为进程内内存存储（不写磁盘）
 */
class LocalAccountStore(context: Context) {

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

    // 加密存储不可用时的内存兜底
    private var memoryUsername: String? = null
    private var memoryHash: String? = null
    private var memorySalt: String? = null
    private var memoryIterations: Int = 0

    fun hasAccount(): Boolean = !username().isNullOrBlank()

    fun username(): String? =
        prefs?.getString(KEY_USERNAME, null) ?: memoryUsername

    /**
     * 保存本机账号。调用前需确认当前没有账号。
     */
    fun register(username: String, password: String): Boolean {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hash(password, salt, ITERATIONS)
        val saltText = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashText = Base64.encodeToString(hash, Base64.NO_WRAP)

        memoryUsername = username
        memorySalt = saltText
        memoryHash = hashText
        memoryIterations = ITERATIONS

        val editor = prefs?.edit() ?: return true
        editor.putString(KEY_USERNAME, username)
            .putString(KEY_SALT, saltText)
            .putString(KEY_HASH, hashText)
            .putInt(KEY_ITERATIONS, ITERATIONS)
            .apply()
        return true
    }

    /**
     * 校验用户名与密码是否与本机账号一致。
     */
    fun verify(username: String, password: String): Boolean {
        val storedUsername = username() ?: return false
        if (!storedUsername.equals(username.trim(), ignoreCase = true)) return false

        val saltText = prefs?.getString(KEY_SALT, null) ?: memorySalt ?: return false
        val hashText = prefs?.getString(KEY_HASH, null) ?: memoryHash ?: return false
        val iterations = prefs?.getInt(KEY_ITERATIONS, ITERATIONS)
            ?.takeIf { it > 0 } ?: memoryIterations

        return runCatching {
            val salt = Base64.decode(saltText, Base64.NO_WRAP)
            val expected = Base64.decode(hashText, Base64.NO_WRAP)
            val actual = hash(password, salt, iterations)
            MessageDigest.isEqual(actual, expected)
        }.getOrDefault(false)
    }

    private fun hash(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec)
            .encoded
    }

    companion object {
        private const val FILE_NAME = "jay_music_local_account"
        private const val KEY_USERNAME = "account_username"
        private const val KEY_SALT = "account_salt"
        private const val KEY_HASH = "account_hash"
        private const val KEY_ITERATIONS = "account_iterations"

        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
        private const val ITERATIONS = 120_000
    }
}
