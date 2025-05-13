package com.partympakache.littlegig.data.repository

import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.Ticket
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    suspend fun getEvents(): Flow<List<Event>>
    suspend fun getEvent(eventId: String): Flow<Event?> // Changed to Flow
    suspend fun createEvent(event: Event): String
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(eventId: String)
    suspend fun getEventsByKeyword(keyword: String): Flow<List<Event>>
    suspend fun createTicket(ticket: Ticket) // Add this
}