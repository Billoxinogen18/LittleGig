package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventsByKeywordUseCase @Inject constructor(private val eventRepository: EventRepository) {
    suspend operator fun invoke(keyword: String): Flow<List<Event>> {
        return eventRepository.getEventsByKeyword(keyword)
    }
}