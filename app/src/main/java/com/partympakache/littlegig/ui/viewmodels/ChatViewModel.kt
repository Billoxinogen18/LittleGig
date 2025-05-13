package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.partympakache.littlegig.data.model.ChatMessage
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
   // Conversations LiveData now holds a Map of receiverId to last ChatMessage
    private val _conversations = MutableLiveData<Map<String, ChatMessage>>()
    val conversations: LiveData<Map<String, ChatMessage>> = _conversations

    private val _users = MutableLiveData<Map<String, User>>()
    val users: LiveData<Map<String, User>> = _users

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // Fetches conversations
    fun fetchConversations() {
        viewModelScope.launch {
              _loading.value = true
            val currentUserId = FirebaseHelper.getCurrentUserId() ?: return@launch
            try {
               // Fetch sent messages
                val sentMessages = FirebaseHelper.firestore.collection("chat_messages")
                    .whereEqualTo("senderId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects<ChatMessage>()

                // Fetch received messages
                val receivedMessages = FirebaseHelper.firestore.collection("chat_messages")
                    .whereEqualTo("receiverId", currentUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .toObjects<ChatMessage>()

               // Combine and extract unique conversations
                val allMessages = (sentMessages + receivedMessages).distinctBy {
                    if (it.senderId == currentUserId) it.receiverId else it.senderId
                }.sortedByDescending { it.timestamp }


               val conversationsMap = mutableMapOf<String, ChatMessage>()
                allMessages.forEach { message ->
                    val otherUserId = if (message.senderId == currentUserId) message.receiverId else message.senderId
                    if (!conversationsMap.containsKey(otherUserId)) {
                        conversationsMap[otherUserId] = message
                    }
                }

                _conversations.value = conversationsMap

                  // Fetch user details for each conversation
                fetchUserDetails(conversationsMap.keys.toList())


            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching conversations", e)
                 _loading.value = false
            }
        }
    }

      // New function to fetch user details
    private fun fetchUserDetails(userIds: List<String>) {
        viewModelScope.launch {
            try {
                val usersMap = mutableMapOf<String, User>()
                userIds.forEach { userId ->
                    val userDoc = FirebaseHelper.firestore.collection("users").document(userId).get().await()
                    userDoc.toObject<User>()?.let { user ->
                        usersMap[userId] = user
                    }
                }
                _users.value = usersMap
                 _loading.value = false // Set loading to false here

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching user details", e)
                 _loading.value = false
            }
        }
    }
}