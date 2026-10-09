package com.carled.app.data.repository

import com.carled.app.core.common.Resource
import com.carled.app.data.remote.ApiService
import com.carled.app.domain.model.Product
import com.carled.app.domain.repository.ProductRepository

class ProductRepositoryImpl(
    private val apiService: ApiService
) : ProductRepository {

    override suspend fun getProducts(): Resource<List<Product>> {
        return try {
            val dtoList = apiService.getProducts()
            val domainList = dtoList.map { dto ->
                Product(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    price = dto.price,
                    imageUrl = dto.imageUrl,
                    stockQuantity = dto.stockQuantity,
                    category = dto.category
                )
            }
            Resource.Success(domainList)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không thể tải danh sách sản phẩm", e)
        }
    }

    override suspend fun getProductById(id: String): Resource<Product> {
        return try {
            val dto = apiService.getProductById(id)
            Resource.Success(
                Product(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    price = dto.price,
                    imageUrl = dto.imageUrl,
                    stockQuantity = dto.stockQuantity,
                    category = dto.category
                )
            )
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không tìm thấy sản phẩm", e)
        }
    }
}
