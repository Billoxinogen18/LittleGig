package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.repository.EventRepository
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(event: Event): String {
        return eventRepository.createEvent(event)
    }
}