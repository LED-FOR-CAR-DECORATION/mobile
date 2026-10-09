package com.carled.app.presentation.screens.store

import com.carled.app.domain.model.Product

data class StoreUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null
)
