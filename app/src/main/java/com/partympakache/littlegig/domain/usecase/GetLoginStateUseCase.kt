package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.local.DataStoreManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLoginStateUseCase @Inject constructor(private val dataStoreManager: DataStoreManager) {
    operator fun invoke(): Flow<Boolean> {
        return dataStoreManager.getLoginStatus
    }
}