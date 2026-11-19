package com.carled.app.data.repository

import android.content.Context
import android.net.Uri
import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.domain.model.MediaUploadUrlRequest
import com.carled.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024L // AC02 — 10MB, matches x-goog-content-length-range: 0,10485760
private val ALLOWED_CONTENT_TYPES = setOf(
    "image/jpeg", "image/png", "image/webp", "image/gif", "image/svg+xml"
)

class MediaRepositoryImpl(
    private val context: Context,
    private val api: ApiService,
    /** No-authenticator client — the signed-URL PUT must not carry our Bearer
     *  token or risk the 401-retry authenticator touching this request;
     *  GCS V4 signatures are tied to the exact request they were signed for. */
    private val uploadHttpClient: OkHttpClient,
    private val authHeaderProvider: suspend () -> String?
) : MediaRepository {

    override suspend fun uploadImage(uri: Uri, purpose: String, targetId: Long?): Resource<String> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi tải ảnh lên.")

        var tempFile: File? = null
        return try {
            val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            if (contentType !in ALLOWED_CONTENT_TYPES) {
                return Resource.Error("Định dạng ảnh không được hỗ trợ ($contentType). Chỉ chấp nhận JPEG, PNG, WEBP, GIF hoặc SVG.")
            }

            tempFile = withContext(Dispatchers.IO) { copyToCache(uri) }
                ?: return Resource.Error("Không đọc được tệp ảnh đã chọn.")

            val fileSize = tempFile.length()
            if (fileSize <= 0 || fileSize > MAX_UPLOAD_BYTES) {
                return Resource.Error("Kích thước ảnh phải từ 1 byte đến 10MB.")
            }

            val fileName = queryDisplayName(uri) ?: tempFile.name

            val urlRequest = MediaUploadUrlRequest(
                fileName = fileName,
                contentType = contentType,
                fileSize = fileSize,
                purpose = purpose,
                targetId = targetId
            )

            val urlResponse = api.getMediaUploadUrl(authHeader, urlRequest)
            if (!urlResponse.isSuccessful) {
                return Resource.Error("Không lấy được đường dẫn tải ảnh lên (lỗi ${urlResponse.code()}).")
            }
            val upload = urlResponse.body()?.data
                ?: return Resource.Error("Phản hồi từ máy chủ trống")

            // These three headers are part of what the V4 signature itself covers
            // (AC04) — the actual PUT must echo them exactly, or GCS rejects the
            // request as a signature mismatch. Not yet verified against a live
            // deployment — this endpoint isn't deployed as of this writing.
            val putRequest = Request.Builder()
                .url(upload.signedUploadUrl)
                .put(tempFile.asRequestBody(contentType.toMediaTypeOrNull()))
                .header("Content-Type", contentType)
                .header("x-goog-content-length-range", "0,10485760")
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .build()

            val putResult = withContext(Dispatchers.IO) { uploadHttpClient.newCall(putRequest).execute() }
            putResult.use {
                if (!it.isSuccessful) {
                    return Resource.Error("Tải ảnh lên bộ nhớ thất bại (lỗi ${it.code}).")
                }
            }

            Resource.Success(upload.publicUrl)
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể tải ảnh lên.")
        } finally {
            tempFile?.delete()
        }
    }

    private fun copyToCache(uri: Uri): File? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        input.use { source -> tempFile.outputStream().use { target -> source.copyTo(target) } }
        return tempFile
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (e: Exception) { null }
}