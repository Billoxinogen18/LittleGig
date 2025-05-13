package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.local.DataStoreManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SaveLoginStateUseCase @Inject constructor(private val dataStoreManager: DataStoreManager) {
    suspend operator fun invoke(isLoggedIn: Boolean) {
        dataStoreManager.saveLoginState(isLoggedIn)
    }
}