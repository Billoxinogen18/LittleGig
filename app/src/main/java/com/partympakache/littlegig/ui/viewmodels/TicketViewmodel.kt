package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.toObjects
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TicketViewModel : ViewModel() {

    private val _tickets = MutableLiveData<List<Ticket>>()
    val tickets: LiveData<List<Ticket>> = _tickets

    fun fetchTickets() {
        viewModelScope.launch {
            val currentUserId = FirebaseHelper.getCurrentUserId()
            if (currentUserId == null) {
                // Handle not logged in case
                return@launch
            }
            try {
                val querySnapshot = FirebaseHelper.firestore.collection("tickets")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()

                _tickets.value = querySnapshot.toObjects()
            } catch (e: Exception) {
                Log.e("TicketViewModel", "Error fetching tickets", e)
                // Handle the error
            }
        }
    }
            }