package com.carled.app.presentation.screens.store

import com.carled.app.domain.model.Product

data class StoreUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val pixelItems: List<PixelArtStoreItem> = PixelArtStoreData.items,
    val selectedTopTab: Int = 0, // 0: "Bộ sưu tập", 1: "Chuyên gia", 2: "Tập ảnh"
    val selectedCategory: String = "MỚI",
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val showBanner: Boolean = true,
    val selectedItemDetail: PixelArtStoreItem? = null,
    val isApplying: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)
