package com.carled.app.presentation.screens.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.repository.ProductRepository
import com.carled.app.domain.usecase.ble.BleUseCases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StoreViewModel(
    private val productRepository: ProductRepository,
    private val bleUseCases: BleUseCases = ServiceLocator.bleUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun selectTopTab(index: Int) {
        _uiState.update { it.copy(selectedTopTab = index) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(open: Boolean? = null) {
        _uiState.update { it.copy(isSearchOpen = open ?: !it.isSearchOpen) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(showBanner = false) }
    }

    fun selectItemDetail(item: PixelArtStoreItem?) {
        _uiState.update { it.copy(selectedItemDetail = item) }
    }

    fun toggleLike(itemId: String) {
        _uiState.update { state ->
            val updated = state.pixelItems.map { item ->
                if (item.id == itemId) {
                    val newLiked = !item.isLiked
                    item.copy(
                        isLiked = newLiked,
                        likes = if (newLiked) item.likes + 1 else (item.likes - 1).coerceAtLeast(0)
                    )
                } else item
            }
            state.copy(
                pixelItems = updated,
                selectedItemDetail = if (state.selectedItemDetail?.id == itemId) {
                    val curr = state.selectedItemDetail
                    val newLiked = !curr.isLiked
                    curr.copy(
                        isLiked = newLiked,
                        likes = if (newLiked) curr.likes + 1 else (curr.likes - 1).coerceAtLeast(0)
                    )
                } else state.selectedItemDetail
            )
        }
    }

    /**
     * Chiếu tác phẩm Pixel Art trực tiếp lên bảng LED ma trận của xe.
     */
    fun applyPixelArtToCar(item: PixelArtStoreItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, feedbackMessage = "Đang gửi hình \"${item.title}\" lên xe...") }
            runCatching {
                val rgb565 = item.toRgb565Data()
                bleUseCases.uploadProjection(rgb565)
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        feedbackMessage = "✅ Đã chiếu \"${item.title}\" lên màn hình LED xe!"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        feedbackMessage = "❌ Không thể chiếu ảnh: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Ghi tác phẩm Pixel Art vĩnh viễn vào bộ nhớ Flash SPIFFS của xe (0x10 -> 0x11 -> 0x12).
     */
    fun burnPixelArtToFlash(item: PixelArtStoreItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isApplying = true, feedbackMessage = "Đang lưu \"${item.title}\" vào bộ nhớ xe (SPIFFS Flash)...") }
            runCatching {
                val rgb565 = item.toRgb565Data()
                bleUseCases.uploadProjection(rgb565)
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        feedbackMessage = "✅ Đã lưu \"${item.title}\" vĩnh viễn vào bộ nhớ xe thành công!"
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isApplying = false,
                        feedbackMessage = "❌ Không thể lưu vào Flash: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = productRepository.getProducts()) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, products = result.data)
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
