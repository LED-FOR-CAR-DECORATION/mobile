package com.carled.app.domain.repository

import com.carled.app.core.common.Resource
import com.carled.app.domain.model.CustomerOwnedCharacterResponse

interface CharacterRepository {
    suspend fun getOwnedCharacters(): Resource<List<CustomerOwnedCharacterResponse>>
}