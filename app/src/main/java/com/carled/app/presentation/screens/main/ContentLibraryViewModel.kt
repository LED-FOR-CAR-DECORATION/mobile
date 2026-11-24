package com.carled.app.presentation.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.carled.app.core.common.Resource
import com.carled.app.core.di.ServiceLocator
import com.carled.app.domain.model.CustomerOwnedCharacterResponse
import com.carled.app.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ContentLibraryViewModel(
    private val repository: CharacterRepository = ServiceLocator.characterRepository
) : ViewModel() {

    private val _ownedCharacters = MutableStateFlow<Resource<List<CustomerOwnedCharacterResponse>>>(Resource.Loading)
    val ownedCharacters: StateFlow<Resource<List<CustomerOwnedCharacterResponse>>> = _ownedCharacters.asStateFlow()

    init { loadOwnedCharacters() }

    fun loadOwnedCharacters() {
        viewModelScope.launch {
            _ownedCharacters.value = Resource.Loading
            _ownedCharacters.value = repository.getOwnedCharacters()
        }
    }
}