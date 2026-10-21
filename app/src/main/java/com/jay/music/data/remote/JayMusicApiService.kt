package com.jay.music.data.remote

import com.jay.music.data.remote.dto.ApiResponse
import com.jay.music.data.remote.dto.AuthDataDto
import com.jay.music.data.remote.dto.FavoriteActionRequest
import com.jay.music.data.remote.dto.HistoryActionRequest
import com.jay.music.data.remote.dto.HistoryListDataDto
import com.jay.music.data.remote.dto.LoginRequest
import com.jay.music.data.remote.dto.RegisterRequest
import com.jay.music.data.remote.dto.ResetPasswordRequest
import com.jay.music.data.remote.dto.SendCodeDataDto
import com.jay.music.data.remote.dto.SendCodeRequest
import com.jay.music.data.remote.dto.SongListDataDto
import com.jay.music.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Jay Music 自有后端接口。
 * 除验证码/注册/登录/重置密码外，均需 Authorization: Bearer {token}
 */
interface JayMusicApiService {

    @POST("api/send_code.php")
    suspend fun sendCode(@Body body: SendCodeRequest): ApiResponse<SendCodeDataDto>

    @POST("api/register.php")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthDataDto>

    @POST("api/login.php")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthDataDto>

    @POST("api/reset_password.php")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ApiResponse<Any?>

    @GET("api/user.php")
    suspend fun getUser(): ApiResponse<UserDto>

    @GET("api/favorites.php")
    suspend fun getFavorites(): ApiResponse<SongListDataDto>

    @POST("api/favorites.php")
    suspend fun updateFavorite(@Body body: FavoriteActionRequest): ApiResponse<Any?>

    @GET("api/history.php")
    suspend fun getHistory(): ApiResponse<HistoryListDataDto>

    @POST("api/history.php")
    suspend fun updateHistory(@Body body: HistoryActionRequest): ApiResponse<Any?>
}
