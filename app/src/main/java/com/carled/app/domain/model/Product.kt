package com.carled.app.domain.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val stockQuantity: Int,
    val category: String = "LED Accessory"
)
