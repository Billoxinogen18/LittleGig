package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.toObject
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.domain.usecase.GetEventsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import com.partympakache.littlegig.domain.usecase.GetEventsByKeywordUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class EventListViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val getEventsByKeywordUseCase: GetEventsByKeywordUseCase
) : ViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList()) // Corrected: Initial value is emptyList()
    val events: StateFlow<List<Event>> = _events  // Expose as StateFlow

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var searchJob: Job? = null // Keep track of the search coroutine


    init {
        fetchEvents() // Initial fetch
    }

    fun fetchEvents() {
        viewModelScope.launch {
            _isLoading.value = true
            getEventsUseCase()
                .catch { e ->
                    Log.e("EventListViewModel", "Error fetching events: ${e.message}", e)
                    _isLoading.value = false
                    // Consider emitting an error state to the UI for user feedback
                }
                .onEach { events ->
                    _events.value = events
                    _isLoading.value = false
                }
                .launchIn(viewModelScope)
        }
    }

    fun searchEvents(query: String) {
        _searchQuery.value = query
        searchJob?.cancel() // Cancel any previous search job
        searchJob = viewModelScope.launch {
            delay(300) // Debounce
            _isLoading.value = true
            if (query.isBlank()) {
                fetchEvents() // If blank, fetch all
            } else {
                getEventsByKeywordUseCase(query)
                    .catch { e ->
                        Log.e("EventListViewModel", "Error searching events: ${e.message}", e)
                        _isLoading.value = false
                        //Handle error
                    }
                    .onEach { events ->
                        _events.value = events
                        _isLoading.value = false
                    }
                    .launchIn(viewModelScope)
            }
        }
    }



    fun toggleLike(event: Event) {
        viewModelScope.launch {
            val currentUserId = FirebaseHelper.getCurrentUserId() ?: return@launch

            val eventRef = FirebaseHelper.firestore.collection("events").document(event.eventId)

            try {
                FirebaseHelper.firestore.runTransaction { transaction ->
                    val snapshot = transaction.get(eventRef)
                    val currentLikes = snapshot.toObject<Event>()?.likedByUserIds ?: emptyList()

                    val updatedLikes = if (currentLikes.contains(currentUserId)) {
                        currentLikes - currentUserId // Remove like
                    } else {
                        currentLikes + currentUserId // Add like
                    }
                    transaction.update(eventRef, "likedByUserIds", updatedLikes)
                    null
                }.await()

                // Optimistically update the UI *before* the transaction completes.
                val updatedEvents = _events.value.map {
                    if (it.eventId == event.eventId) {
                        val updatedLikes = if (it.likedByUserIds.contains(currentUserId)) {
                            it.likedByUserIds - currentUserId
                        } else {
                            it.likedByUserIds + currentUserId
                        }
                        it.copy(likedByUserIds = updatedLikes)
                    } else {
                        it
                    }
                }
                _events.value = updatedEvents // Use .value for StateFlow

            } catch (e: Exception) {
                Log.e("EventListViewModel", "Error toggling like", e)
                // If the transaction fails, you might want to revert the UI change
                // or show an error message.
            }
        }
    }
}