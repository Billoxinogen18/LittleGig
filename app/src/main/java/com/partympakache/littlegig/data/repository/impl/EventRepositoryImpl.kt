package com.partympakache.littlegig.data.repository.impl

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.data.repository.EventRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : EventRepository {

    override suspend fun getEvents(): Flow<List<Event>> = callbackFlow {
        val listenerRegistration = firestore.collection("events")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Close the flow with the error
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(events) // Use trySend to avoid blocking
                }
            }
        awaitClose { listenerRegistration.remove() } // Clean up the listener
    }


    override suspend fun getEvent(eventId: String): Flow<Event?> = callbackFlow { //Change this
        val docRef = firestore.collection("events").document(eventId)
        val listenerRegistration = docRef.addSnapshotListener{ snapshot, error ->
            if(error != null){
                close(error)
                return@addSnapshotListener
            }
            if(snapshot != null && snapshot.exists()){
                trySend(snapshot.toObject(Event::class.java))
            } else {
                trySend(null) //Send null if event doesn't exist/deleted
            }
        }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createEvent(event: Event): String {
        return try {
            val docRef = firestore.collection("events").add(event).await()
            // Update the event with the generated ID.
            firestore.collection("events").document(docRef.id).update("eventId", docRef.id).await()
            docRef.id // Return the ID

        }catch (e: Exception){
            Log.e("EventRepo", "Failed to create", e)
            ""
        }
    }


    override suspend fun updateEvent(event: Event) {
        try {
            firestore.collection("events").document(event.eventId).set(event).await()
        }catch (e: Exception){
            Log.e("EventRepo", "Failed to update", e)
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        try {
            firestore.collection("events").document(eventId).delete().await()
        }catch (e: Exception){
            Log.e("EventRepo", "Failed to delete", e)
        }
    }

    override suspend fun getEventsByKeyword(keyword: String): Flow<List<Event>> = callbackFlow {
        val listenerRegistration = firestore.collection("events")
            .whereGreaterThanOrEqualTo("title", keyword)
            .whereLessThanOrEqualTo("title", keyword + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val events = snapshot.toObjects(Event::class.java)
                    trySend(events)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun createTicket(ticket: Ticket){
        try{
            firestore.collection("tickets").document(ticket.ticketId).set(ticket).await()
        } catch(e: Exception){
            Log.e("EventRepo", "Failed to create ticket", e)
            // Consider re-throwing the exception, or wrapping it in a custom exception
            throw e
        }
    }

}