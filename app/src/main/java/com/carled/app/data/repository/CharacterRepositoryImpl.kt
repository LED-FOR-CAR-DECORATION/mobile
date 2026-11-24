package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.repository.CharacterRepository
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class CharacterRepositoryImpl(
    private val api: ApiService,
    private val authHeaderProvider: suspend () -> String?
) : CharacterRepository {

    override suspend fun getOwnedCharacters(): Resource<List<CustomerOwnedCharacterResponse>> {
        val authHeader = authHeaderProvider() ?: return Resource.Error("Bạn cần đăng nhập trước khi xem Inventory.")
        return try {
            val response = api.getOwnedCharacters(authHeader)
            if (response.isSuccessful) {
                Resource.Success(response.body()?.data.orEmpty())
            } else {
                Resource.Error("Không tải được danh sách character (lỗi ${response.code()}).")
            }
        } catch (e: IOException) {
            Resource.Error("Không kết nối được tới máy chủ. Kiểm tra mạng và thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không tải được danh sách character.")
        }
    }
}