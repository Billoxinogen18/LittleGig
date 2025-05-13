package com.partympakache.littlegig.domain.usecase

import android.net.Uri
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.repository.RecapRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecapsForEventUseCase  @Inject constructor(private val recapRepository: RecapRepository) {
    suspend operator fun invoke(eventId: String): Flow<List<Recap>> {
        return recapRepository.getRecapsForEvent(eventId)
    }
}