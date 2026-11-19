package com.carled.app.domain.repository

import android.net.Uri
import com.carled.app.core.common.Resource

interface MediaRepository {
    suspend fun uploadImage(uri: Uri, purpose: String, targetId: Long? = null): Resource<String>
}