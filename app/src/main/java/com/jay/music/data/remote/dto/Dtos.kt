package com.jay.music.data.remote.dto

import com.google.gson.annotations.SerializedName

/** 后端统一响应 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int = -1,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 0
}

data class UserDto(
    @SerializedName("id") val id: Long = 0,
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("avatar") val avatar: String? = null
)

data class AuthDataDto(
    @SerializedName("token") val token: String = "",
    @SerializedName("user") val user: UserDto? = null
)

data class SendCodeDataDto(
    @SerializedName("cooldown") val cooldown: Int = 60,
    @SerializedName("expiresIn") val expiresIn: Int = 300
)

data class SongDto(
    @SerializedName("songId") val songId: String? = null,
    @SerializedName("id") val id: String? = null,
    @SerializedName("songName") val songName: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("duration") val duration: Long = 0L,
    @SerializedName("createdAt") val createdAt: Long = 0L,
    @SerializedName("playedAt") val playedAt: Long = 0L
) {
    val realId: String get() = (songId ?: id ?: "").toString()
    val realName: String get() = songName ?: name ?: ""
}

data class SongListDataDto(
    @SerializedName("songs") val songs: List<SongDto>? = null
)

data class HistoryListDataDto(
    @SerializedName("records") val records: List<SongDto>? = null,
    @SerializedName("songs") val songs: List<SongDto>? = null
)

// ------------------------- 请求体 -------------------------

data class SendCodeRequest(
    @SerializedName("email") val email: String,
    @SerializedName("scene") val scene: String // register / reset
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("code") val code: String
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("code") val code: String,
    @SerializedName("newPassword") val newPassword: String
)

data class FavoriteActionRequest(
    @SerializedName("action") val action: String, // add / remove
    @SerializedName("songId") val songId: String,
    @SerializedName("songName") val songName: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("duration") val duration: Long = 0L
)

data class HistoryActionRequest(
    @SerializedName("action") val action: String, // add / clear
    @SerializedName("songId") val songId: String? = null,
    @SerializedName("songName") val songName: String? = null,
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("duration") val duration: Long = 0L
)
