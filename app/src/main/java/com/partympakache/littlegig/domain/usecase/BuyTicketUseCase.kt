package com.partympakache.littlegig.domain.usecase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.data.repository.EventRepository
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class BuyTicketUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val firestore: FirebaseFirestore // Inject Firestore directly
) {
    suspend operator fun invoke(eventId: String) {
        val userId = FirebaseHelper.getCurrentUserId() ?: throw Exception("User not logged in")

        // Use .first() to get the first (and in this case, only) value from the Flow.
        val event = eventRepository.getEvent(eventId).first() ?: throw Exception("Event not found")

        if (event.availableTickets <= 0) {
            throw Exception("No tickets available")
        }

        // Use Firestore's transaction API for atomicity.
        firestore.runTransaction { transaction ->
            // 1. Get the event document *inside* the transaction.  This is crucial for consistency.
            val eventRef = firestore.collection("events").document(eventId)
            val snapshot = transaction.get(eventRef)
            val currentEvent = snapshot.toObject<Event>() ?: throw Exception("Event not found") // Use toObject

            // 2. Check availability *again* inside the transaction.
            if (currentEvent.availableTickets <= 0) {
                throw Exception("No tickets available") // This will roll back the transaction
            }

            // 3. Decrement tickets.
            transaction.update(eventRef, "availableTickets", currentEvent.availableTickets - 1)

            // 4. Create the ticket.
            val ticketId = UUID.randomUUID().toString()
            val ticket = Ticket(
                ticketId = ticketId,
                eventId = eventId,
                userId = userId,
                purchaseDate = com.google.firebase.Timestamp.now().toDate().toString(),
                qrCodeData = generateQrCodeData(ticketId)
            )
            val ticketRef = firestore.collection("tickets").document(ticketId)
            transaction.set(ticketRef, ticket)

            null // Transactions must return something.  Null is fine.

        }.await() // Use .await() to properly handle the Task<Void> returned by runTransaction
    }

    private fun generateQrCodeData(ticketId: String): String {
        return "littlegig-ticket:$ticketId"
    }
}