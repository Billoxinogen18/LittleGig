//data/repository/impl/RecapRepositoryImpl.kt
package com.partympakache.littlegig.data.repository.impl

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.partympakache.littlegig.data.local.DataStoreManager
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.repository.RecapRepository
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class RecapRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val dataStoreManager: DataStoreManager // Inject DataStoreManager
) : RecapRepository {
    override suspend fun postRecap(eventId: String, caption: String?, mediaUri: Uri) {
        val userId = FirebaseHelper.getCurrentUserId() ?: return
        val recapId = UUID.randomUUID().toString() // Generate a unique ID
        val storageRef = FirebaseHelper.storage.reference
        val mediaRef: StorageReference = storageRef.child("recaps/$eventId/$recapId") // Path in storage

        try {
            // Upload the media
            val uploadTask = mediaRef.putFile(mediaUri).await()
            val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

            // Create the Recap object
            val recap = Recap(
                recapId = recapId,
                eventId = eventId,
                userId = userId,
                mediaUrl = downloadUrl, // Store the download URL
                caption = caption,
                timestamp = com.google.firebase.Timestamp.now().toDate().toString(), // Use Firebase Timestamp
                likes = emptyList(),
                views = 0
            )

            // Save to Firestore
            firestore.collection("recaps").document(recapId).set(recap).await()


        } catch (e: Exception) {
            Log.e("EventDetailsViewModel", "Error posting recap", e)
            // Handle upload failure
        }
    }

    override suspend fun getRecapsForEvent(eventId: String): Flow<List<Recap>> = callbackFlow {
        val listenerRegistration = firestore.collection("recaps")
            .whereEqualTo("eventId", eventId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val recaps = snapshot.toObjects<Recap>()
                    trySend(recaps)
                } else {
                    trySend(emptyList()) // Send empty list if no data
                }
            }
        awaitClose { listenerRegistration.remove() }
    }
    override suspend fun likeRecap(recapId: String, userId: String) {
        val recapRef = firestore.collection("recaps").document(recapId)
        recapRef.update("likes", FieldValue.arrayUnion(userId)).await()
    }

    override suspend fun unlikeRecap(recapId: String, userId: String) {
        val recapRef = firestore.collection("recaps").document(recapId)
        recapRef.update("likes", FieldValue.arrayRemove(userId)).await()
    }


    override suspend fun incrementViewCount(recapId: String, userId: String) {
        val hasViewed = dataStoreManager.hasRecapBeenViewed(recapId, userId).first() // Await the result
        if (!hasViewed) {
            val recapRef = firestore.collection("recaps").document(recapId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(recapRef)
                val currentViews = snapshot.getLong("views") ?: 0
                transaction.update(recapRef, "views", currentViews + 1)
            }.await()
            dataStoreManager.setRecapViewed(recapId, userId) // Mark as viewed
        }
    }
    override suspend fun hasUserViewedRecap(recapId: String, userId: String): Flow<Boolean> {
        return dataStoreManager.hasRecapBeenViewed(recapId, userId)
    }
}