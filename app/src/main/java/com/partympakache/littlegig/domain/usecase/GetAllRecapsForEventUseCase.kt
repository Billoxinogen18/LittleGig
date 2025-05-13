package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.repository.RecapRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllRecapsForEventUseCase @Inject constructor(private val recapRepository: RecapRepository) {
    suspend operator fun invoke(eventId: String): Flow<List<Recap>> {
        return recapRepository.getRecapsForEvent(eventId)
    }
}