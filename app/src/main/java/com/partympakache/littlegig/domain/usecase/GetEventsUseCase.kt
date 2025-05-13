package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): Flow<List<Event>> {
        return eventRepository.getEvents()
    }
}