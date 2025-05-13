package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.toObject
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditEventViewModel : ViewModel() {
    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData(false)
    val saveSuccess: LiveData<Boolean> = _saveSuccess

    fun fetchEvent(eventId: String) {
        _isLoading.value = true // Start loading
        viewModelScope.launch {
            try {
                val docRef = FirebaseHelper.firestore.collection("events").document(eventId)
                val snapshot = docRef.get().await()
                _event.value = snapshot.toObject<Event>()
            } catch (e: Exception) {
                 Log.e("EditEventViewModel", "Error fetching event", e)
                // Handle error (e.g., show error message)
            } finally {
                 _isLoading.value = false
            }
        }
    }

    fun saveEvent(updatedEvent: Event) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                FirebaseHelper.firestore.collection("events").document(updatedEvent.eventId)
                    .set(updatedEvent)
                    .await()  // Use .set() for updating existing documents
                _saveSuccess.value = true
            } catch (e: Exception) {
                  Log.e("EditEventViewModel", "Error saving event", e)
                // Handle error - show message, etc.
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
}