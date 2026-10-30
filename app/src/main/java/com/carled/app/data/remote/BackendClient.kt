package com.carled.app.data.remote

import com.carled.app.BuildConfig
import com.carled.app.data.local.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class BackendClient(private val preferences: UserPreferences) {
    private val lock = Mutex()
    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null, authorized: Boolean = true): JSONObject = withContext(Dispatchers.IO) {
        lock.withLock {
            val first = exchange(path,method,body,if (authorized) preferences.getAuthToken() else null)
            if (first.first == 401 && authorized) {
                val refresh = preferences.getRefreshToken() ?: error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
                val renewed = exchange("/api/auth/refresh","POST",JSONObject().put("refreshToken",refresh),null)
                val data = checked(renewed)
                preferences.saveAuthToken(data.getString("accessToken")); preferences.saveRefreshToken(data.getString("refreshToken"))
                checked(exchange(path,method,body,preferences.getAuthToken()))
            } else checked(first)
        }
    }
    private fun checked(response: Pair<Int,JSONObject>): JSONObject {
        check(response.first in 200..299) {
            val message = response.second.optString("message","Yêu cầu không thành công")
            val trace = response.second.optString("traceId")
            "$message${if (trace.isBlank()) "" else " (Mã hỗ trợ: $trace)"}"
        }
        return response.second.optJSONObject("data") ?: JSONObject()
    }
    private fun exchange(path: String, method: String, body: JSONObject?, token: String?): Pair<Int,JSONObject> {
        check(BuildConfig.BACKEND_URL.startsWith("https://")) { "Ứng dụng chưa được cấu hình máy chủ HTTPS. Cần bản cài kết nối backend." }
        val connection = URL(BuildConfig.BACKEND_URL + path).openConnection() as HttpsURLConnection
        try {
            connection.requestMethod=method; connection.connectTimeout=15000; connection.readTimeout=20000
            connection.instanceFollowRedirects=false; connection.setRequestProperty("Accept","application/json")
            token?.let { connection.setRequestProperty("Authorization","Bearer $it") }
            body?.let {
                connection.doOutput=true; connection.setRequestProperty("Content-Type","application/json")
                connection.outputStream.use { output -> output.write(it.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status=connection.responseCode
            val stream=if(status in 200..299) connection.inputStream else connection.errorStream
            val text=stream?.use {
                val output=java.io.ByteArrayOutputStream()
                val buffer=ByteArray(4096)
                while(true) {
                    val count=it.read(buffer)
                    if(count<0) break
                    check(output.size()+count<=65536) { "Phản hồi máy chủ quá lớn" }
                    output.write(buffer,0,count)
                }
                String(output.toByteArray(),Charsets.UTF_8)
            } ?: "{}"
            val json=try { JSONObject(text) } catch (_: org.json.JSONException) { error("Máy chủ trả về dữ liệu không hợp lệ ($status)") }
            return status to json
        } finally { connection.disconnect() }
    }
}
