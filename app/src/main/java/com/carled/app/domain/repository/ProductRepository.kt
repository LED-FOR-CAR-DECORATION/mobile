package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(): Resource<List<Product>>
    suspend fun getProductById(id: String): Resource<Product>
}
