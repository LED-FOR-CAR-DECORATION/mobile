package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.data.remote.dto.ApiErrorResponse
import com.carled.app.data.remote.dto.ProductDto
import com.carled.app.domain.model.Product
import com.carled.app.domain.repository.ProductRepository
import com.google.gson.Gson
import retrofit2.Response

class ProductRepositoryImpl(
    private val apiService: ApiService,
    private val gson: Gson = Gson()
) : ProductRepository {

    override suspend fun getProducts(): Resource<List<Product>> {
        return try {
            val response = apiService.getProducts()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Resource.Error("Phản hồi từ máy chủ trống")
                Resource.Success(body.map { it.toDomain() })
            } else {
                Resource.Error(parseErrorBody(response, "Không thể tải danh sách sản phẩm"))
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
        }
    }

    override suspend fun getProductById(id: String): Resource<Product> {
        return try {
            val response = apiService.getProductById(id)
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Resource.Error("Phản hồi từ máy chủ trống")
                Resource.Success(body.toDomain())
            } else {
                Resource.Error(parseErrorBody(response, "Không tìm thấy sản phẩm"))
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi kết nối mạng", e)
        }
    }

    private fun ProductDto.toDomain() = Product(
        id            = id,
        name          = name,
        description   = description,
        price         = price,
        imageUrl      = imageUrl,
        stockQuantity = stockQuantity,
        category      = category
    )

    private fun parseErrorBody(response: Response<*>, defaultMessage: String): String {
        return try {
            response.errorBody()
                ?.string()
                ?.takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, ApiErrorResponse::class.java).resolvedMessage() }
                ?: defaultMessage
        } catch (e: Exception) {
            defaultMessage
        }
    }
}