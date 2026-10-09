package com.carled.app.data.remote

import com.carled.app.data.remote.dto.LoginRequest
import com.carled.app.data.remote.dto.LoginResponse
import com.carled.app.data.remote.dto.ProductDto
import com.carled.app.data.remote.dto.UserDto
import kotlinx.coroutines.delay

interface ApiService {
    suspend fun login(request: LoginRequest): LoginResponse
    suspend fun getProducts(): List<ProductDto>
    suspend fun getProductById(id: String): ProductDto
}

/**
 * Mock implementation sẵn sàng chạy thử và dễ dàng thay bằng Retrofit/Ktor khi backend online.
 */
class MockApiService : ApiService {
    override suspend fun login(request: LoginRequest): LoginResponse {
        delay(600) // Giả lập độ trễ mạng
        return LoginResponse(
            token = "jwt-mock-token-sample-sep-carled",
            user = UserDto(
                id = "usr_001",
                email = request.email,
                fullName = "Car LED Driver",
                role = "Customer"
            )
        )
    }

    override suspend fun getProducts(): List<ProductDto> {
        delay(400)
        return listOf(
            ProductDto(
                id = "prod_01",
                name = "ESP32 Matrix HUD LED Bar 64x16",
                description = "Thanh LED kính lái độ phân giải cao hỗ trợ hiệu ứng nhân vật Character và âm thanh",
                price = 850000.0,
                imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f",
                stockQuantity = 25,
                category = "LED Hardware"
            ),
            ProductDto(
                id = "prod_02",
                name = "Gói hoạt ảnh: Cyberpunk Neon Character Pack",
                description = "Bộ sưu tập 12 animation nhân vật phong cách tương lai chiếu kính lái xe",
                price = 150000.0,
                imageUrl = "https://images.unsplash.com/photo-1563089145-599997674d42",
                stockQuantity = 999,
                category = "Digital Content"
            ),
            ProductDto(
                id = "prod_03",
                name = "Bộ cấp nguồn chuyển đổi 12V sang 5V 5A siêu êm",
                description = "Chuyên dụng cho lắp đặt ô tô chống sụt áp và lọc nhiễu hệ thống điện xe",
                price = 120000.0,
                imageUrl = "https://images.unsplash.com/photo-1581092160607-ee22621dd758",
                stockQuantity = 40,
                category = "Accessories"
            )
        )
    }

    override suspend fun getProductById(id: String): ProductDto {
        return getProducts().first { it.id == id }
    }
}
