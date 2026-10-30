package com.carled.app.data.remote

import android.util.Base64
import com.carled.app.data.local.UserPreferences
import com.carled.app.data.remote.dto.*
import org.json.JSONObject

// Store endpoints retain their existing prototype adapter; authentication is real.
class BackendApiService(private val client: BackendClient, private val preferences: UserPreferences) : ApiService by MockApiService() {
    override suspend fun login(request: LoginRequest): LoginResponse {
        val data=client.request("/api/auth/login","POST",JSONObject().put("email",request.email).put("password",request.password),false)
        val token=data.getString("accessToken")
        val claims=JSONObject(String(Base64.decode(token.split('.')[1],Base64.URL_SAFE or Base64.NO_WRAP),Charsets.UTF_8))
        preferences.saveRefreshToken(data.getString("refreshToken"))
        return LoginResponse(token,UserDto(claims.getString("sub"),request.email,request.email.substringBefore('@'),"Customer"))
    }
    override suspend fun register(request: LoginRequest): LoginResponse {
        client.request("/api/auth/register","POST",JSONObject().put("email",request.email).put("password",request.password).put("confirmPassword",request.password),false)
        error("Đã tạo tài khoản. Hãy xác thực email trên website, sau đó đăng nhập vào ứng dụng.")
    }
}
