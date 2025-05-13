package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.partympakache.littlegig.data.model.ChatMessage
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConversationViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private var messagesListener: ListenerRegistration? = null // For real-time updates

     fun loadMessages(receiverId: String) {
        val currentUserId = FirebaseHelper.getCurrentUserId() ?: return

          // Remove any existing listener
        messagesListener?.remove()

        messagesListener = FirebaseHelper.firestore.collection("chat_messages")
            .whereIn("senderId", listOf(currentUserId, receiverId))
            .whereIn("receiverId", listOf(currentUserId, receiverId))
            .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp
            .addSnapshotListener { snapshot, e -> // Use addSnapshotListener for real-time
                if (e != null) {
                    Log.w("ConversationViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chatMessages = snapshot.toObjects<ChatMessage>()
                    _messages.value = chatMessages // Update LiveData
                } else {
                    Log.d("ConversationViewModel", "Current data: null")
                }
            }
    }

      fun sendMessage(receiverId: String, messageText: String) {
        viewModelScope.launch {
             val currentUserId = FirebaseHelper.getCurrentUserId() ?: return@launch

            val message = ChatMessage(
                messageId = FirebaseHelper.firestore.collection("chat_messages").document().id, // Generate unique ID
                senderId = currentUserId,
                receiverId = receiverId,
                messageText = messageText,
                timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

            )
             try {
                FirebaseHelper.firestore.collection("chat_messages").document(message.messageId).set(message).await()
                // Optionally clear the input field or handle success
            } catch (e: Exception) {
                 Log.e("ConversationViewModel", "Error sending message", e)
            }
        }
    }
     // Clean up listener when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}