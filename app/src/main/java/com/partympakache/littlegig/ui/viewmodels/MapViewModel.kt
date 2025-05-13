package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.utils.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() { //Hilt

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _activeUsers = MutableStateFlow<List<Pair<User, GeoPoint?>>>(emptyList())
    val activeUsers: StateFlow<List<Pair<User, GeoPoint?>>> = _activeUsers

    // Function to fetch event locations
    fun fetchEvents() {
        viewModelScope.launch {
            try {
                val querySnapshot = FirebaseHelper.firestore.collection("events").get().await()
                _events.value = querySnapshot.toObjects()
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching events", e)
            }
        }
    }

    //Combine fetching users and their location
    fun fetchActiveUsers() {
        viewModelScope.launch {
            try {
                val querySnapshot = FirebaseHelper.firestore.collection("users")
                    .whereEqualTo("isActiveNow", true)
                    .get()
                    .await()

                val usersWithLocations = querySnapshot.documents.mapNotNull { document ->
                    val user = document.toObject<User>()
                    if (user != null && user.lastActiveTimestamp != null) {
                        val location = getUserLocation(user.userId)
                        Pair(user, location) // Return a Pair
                    } else {
                        null // Filter out users without location or not active
                    }
                }
                _activeUsers.value = usersWithLocations
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error fetching active users", e)
            }
        }
    }

    //Get User location using userId
    private suspend fun getUserLocation(userId: String): GeoPoint? { //Made it private
        return try {
            val userDoc = FirebaseHelper.firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject<User>()
            if (user?.isActiveNow == true) {
                val userLocation = FirebaseHelper.firestore.collection("user_locations").document(userId).get().await()
                userLocation.toObject<UserLocation>()?.location
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MapViewModel", "Error fetching user location", e)
            null
        }
    }
}

//Create data class for the user location, since we store locations
data class UserLocation(
    val location: GeoPoint = GeoPoint(0.0, 0.0) // Provide a default value
)