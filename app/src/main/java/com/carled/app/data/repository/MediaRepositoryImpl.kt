package com.carled.app.data.repository

import android.content.Context
import android.net.Uri
import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.domain.model.CreateSignedUploadUrlRequest
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

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024L
// Matches the server's deployed regex exactly (^(image/jpeg|image/png|image/webp|image/gif)$) —
// note this excludes image/svg+xml despite an earlier AC02 text description
// listing SVG as allowed; the actual enforced validation pattern does not.
private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/gif")

/** Purposes whose objectKey (not publicUrl) must be sent to downstream endpoints — see SignedUploadUrlResponse.publicUrl's doc. */
private val PRIVATE_PURPOSES = setOf("COMMISSION_REFERENCE", "COMMISSION_CHAT", "COMMISSION_DRAFT", "WARRANTY_EVIDENCE")

class MediaRepositoryImpl(
    private val context: Context,
    private val api: ApiService,
    private val uploadHttpClient: OkHttpClient,
    private val authHeaderProvider: suspend () -> String?
) : MediaRepository {

    override suspend fun uploadImage(uri: Uri, purpose: String, targetId: Long?): Resource<String> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi tải ảnh lên.")

        var tempFile: File? = null
        return try {
            val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            if (contentType !in ALLOWED_CONTENT_TYPES) {
                return Resource.Error("Định dạng ảnh không được hỗ trợ ($contentType). Chỉ chấp nhận JPEG, PNG, WEBP hoặc GIF.")
            }

            tempFile = withContext(Dispatchers.IO) { copyToCache(uri) }
                ?: return Resource.Error("Không đọc được tệp ảnh đã chọn.")

            val fileSize = tempFile.length()
            if (fileSize <= 0 || fileSize > MAX_UPLOAD_BYTES) {
                return Resource.Error("Kích thước ảnh phải từ 1 byte đến 10MB.")
            }

            val fileName = queryDisplayName(uri) ?: tempFile.name

            val urlRequest = CreateSignedUploadUrlRequest(
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

            val putRequestBuilder = Request.Builder()
                .url(upload.signedUploadUrl)
                .put(tempFile.asRequestBody(contentType.toMediaTypeOrNull()))
            // Echo back exactly what the server specified — these headers are
            // part of what the V4 signature itself covers.
            upload.uploadHeaders.forEach { (key, value) -> putRequestBuilder.header(key, value) }

            val putResult = withContext(Dispatchers.IO) { uploadHttpClient.newCall(putRequestBuilder.build()).execute() }
            putResult.use {
                if (!it.isSuccessful) {
                    return Resource.Error("Tải ảnh lên bộ nhớ thất bại (lỗi ${it.code}).")
                }
            }

            // Private purposes (COMMISSION_REFERENCE, COMMISSION_CHAT, ...) have
            // publicUrl = null by design — downstream endpoints want objectKey
            // instead, per CommissionAttachmentItemRequest's own field doc.
            val result = if (purpose in PRIVATE_PURPOSES) upload.objectKey else (upload.publicUrl ?: upload.objectKey)
            Resource.Success(result)
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