package com.partympakache.littlegig.ui.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.domain.usecase.CreateEventUseCase
import com.partympakache.littlegig.utils.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.partympakache.littlegig.utils.getCountryCode
import java.util.*

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val createEventUseCase: CreateEventUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _placePredictions = MutableStateFlow<List<AutocompletePrediction>>(emptyList())
    val placePredictions: StateFlow<List<AutocompletePrediction>> = _placePredictions

    private val _placeDetails = MutableStateFlow<Place?>(null)
    val placeDetails: StateFlow<Place?> = _placeDetails

    private var searchJob: Job? = null
    private val debounceDuration = 300L

    // Function to fetch place predictions, now part of the ViewModel
    fun fetchPlacePredictions(placesClient: PlacesClient, query: String) {
        if (query.isBlank()) {
            _placePredictions.value = emptyList()
            return
        }

        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setCountries(getCountryCode(Locale.getDefault().country))
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response: FindAutocompletePredictionsResponse ->
                _placePredictions.value = response.autocompletePredictions
            }
            .addOnFailureListener { exception: Exception ->
                Log.e("CreateEventViewModel", "Autocomplete prediction error: ${exception.message}", exception)
                // Consider emitting an error state to the UI
            }
    }
    // Function to fetch place details, now part of the ViewModel
    fun fetchPlaceDetails(placesClient: PlacesClient, placeId: String) {
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response: FetchPlaceResponse ->
                _placeDetails.value = response.place
            }
            .addOnFailureListener { exception: Exception ->
                Log.e("CreateEventViewModel", "Place details fetch failed: ${exception.message}", exception)
                // Handle failure
            }
    }

    //Debounce function
    fun searchPlaces(placesClient: PlacesClient, query: String) {
        searchJob?.cancel() // Cancel any existing job
        searchJob = viewModelScope.launch {
            delay(debounceDuration)
            fetchPlacePredictions(placesClient, query)
        }
    }

    //Reset place details and predictions
    fun clearPlaceDetails() {
        _placeDetails.value = null
    }
    fun clearPredictions(){
        _placePredictions.value = emptyList()
    }

    suspend fun createEvent(event: Event, imageUri: Uri?, onSuccess: () -> Unit, onFailure: () -> Unit) {
        try {
            //Upload image and then update event details
            val imageUrl = if (imageUri != null) {
                uploadImageToFirebaseStorage(imageUri)
            } else {
                "" // Or a default image URL
            }
            val updatedEvent = event.copy(imageUrl = imageUrl)
            val newEventId = createEventUseCase(updatedEvent)  //Use case!
            onSuccess()
        } catch (e: Exception) {
            Log.e("CreateEventViewModel", "Error creating event: ${e.message}", e)
            onFailure() // Notify of failure

        }
    }

    private suspend fun uploadImageToFirebaseStorage(imageUri: Uri): String {
        return withContext(Dispatchers.IO) {
            val storageRef = FirebaseHelper.storage.reference
            val imageRef: StorageReference = storageRef.child("event_images/${FirebaseHelper.getCurrentUserId()}/${System.currentTimeMillis()}_${imageUri.lastPathSegment}")

            try {
                val uploadTask = imageRef.putFile(imageUri).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await()
                downloadUrl.toString()
            } catch (e: Exception){
                Log.e("CreateEvent", "Image upload failed: ${e.message}",e)
                "" // Or throw the exception, or a default image URL
            }
        }

    }

}