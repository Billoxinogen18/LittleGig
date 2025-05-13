package com.partympakache.littlegig.ui.viewmodels

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.StorageReference
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.data.repository.EventRepository

import com.partympakache.littlegig.domain.usecase.BuyTicketUseCase
import com.partympakache.littlegig.utils.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import com.partympakache.littlegig.domain.usecase.GetUserUseCase  // Import the Use Case
import com.partympakache.littlegig.domain.usecase.PostRecapUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val buyTicketUseCase: BuyTicketUseCase,
    private val getUserUseCase: GetUserUseCase,
    private val postRecapUseCase: PostRecapUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _event = MutableLiveData<Event?>()
    val event: LiveData<Event?> = _event

    private val _recaps = MutableLiveData<List<Recap>>()
    val recaps: LiveData<List<Recap>> = _recaps

    private val _purchaseSuccess = MutableLiveData<Boolean>()
    val purchaseSuccess: LiveData<Boolean> = _purchaseSuccess

    private val _purchaseError = MutableLiveData<String?>()
    val purchaseError: LiveData<String?> = _purchaseError

    // --- Location Handling ---
    private val sharedPreferences = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    private val _userLocation = mutableStateOf<GeoPoint?>(loadStoredLocation())
    val userLocation: State<GeoPoint?> = _userLocation

    private val _isLoadingLocation = mutableStateOf(false)
    val isLoadingLocation: State<Boolean> = _isLoadingLocation

    private val _latestRecapsByUser = MutableStateFlow<List<Recap>>(emptyList())
    val latestRecapsByUser: StateFlow<List<Recap>> = _latestRecapsByUser.asStateFlow()

    private fun loadStoredLocation(): GeoPoint? {
        val lat = sharedPreferences.getFloat("last_lat", Float.NaN).toDouble()
        val lng = sharedPreferences.getFloat("last_lng", Float.NaN).toDouble()
        return if (!lat.isNaN() && !lng.isNaN()) {
            GeoPoint(lat, lng)
        } else {
            null
        }
    }
    // Save location to SharedPreferences and update state.
    fun saveLocation(location: GeoPoint) {
        sharedPreferences.edit()
            .putFloat("last_lat", location.latitude.toFloat())
            .putFloat("last_lng", location.longitude.toFloat())
            .apply()
        _userLocation.value = location
    }


    // Check for location permission.
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun updateLocationInBackground() {
        if (!hasLocationPermission()) return // Early return if no permission

        viewModelScope.launch {
            try {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .await()

                location?.let {
                    val geoPoint = GeoPoint(it.latitude, it.longitude)
                    saveLocation(geoPoint) // Save the updated location.
                }
            } catch (e: Exception) {
                Log.e("LocationUpdate", "Background update failed", e)
                // Fail silently - don't interrupt user.
            }
        }
    }


    fun fetchEventDetails(eventId: String) {
        viewModelScope.launch {
            try {
                eventRepository.getEvent(eventId).collectLatest { event ->
                    _event.value = event
                }
            } catch (e: Exception) {
                Log.e("EventDetailsViewModel", "Error fetching event details", e)
                // Consider setting an error state that the UI can observe.
            }
        }
    }


    fun fetchRecaps(eventId: String) {
        viewModelScope.launch {
            try {
                val querySnapshot = FirebaseHelper.firestore.collection("recaps")
                    .whereEqualTo("eventId", eventId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                val allRecaps = querySnapshot.toObjects<Recap>()
                _recaps.value = allRecaps

                // Group recaps by user and get the *latest* one from each user
                val groupedRecaps = allRecaps.groupBy { it.userId }
                val latestRecaps = groupedRecaps.mapValues { entry -> entry.value.first() } // first() is newest
                _latestRecapsByUser.value = latestRecaps.values.toList()


            } catch (e: Exception) {
                Log.e("EventDetailsViewModel", "Error fetching recaps", e)
            }
        }
    }


    fun canPostRecap(eventLocation: GeoPoint?): Boolean {
        return userLocation.value?.let { currentUserLocation ->
            eventLocation?.let { eventLoc ->
                calculateDistance(
                    eventLoc.latitude,
                    eventLoc.longitude,
                    currentUserLocation.latitude,
                    currentUserLocation.longitude
                ) <= 3.0
            } ?: false
        } ?: false
    }

    // Function to calculate distance between two points in km.
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (sin(deg2rad(lat1))
                * sin(deg2rad(lat2))
                + (cos(deg2rad(lat1))
                * cos(deg2rad(lat2))
                * cos(deg2rad(theta))))
        dist = acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        return dist * 1.609344 //convert to kms
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    fun postRecap(eventId: String, caption: String?, mediaUri: Uri) {
        viewModelScope.launch {
            try {
                postRecapUseCase(eventId, caption, mediaUri)
            }  catch (e: Exception) {
                Log.e("EventDetailsViewModel", "Error posting recap", e)
            }
        }

    }


    fun buyTicket(eventId: String) {
        viewModelScope.launch {
            try {
                buyTicketUseCase(eventId)
                _purchaseSuccess.value = true
                _purchaseError.value = null  // Clear any previous error
            } catch (e: Exception) {
                _purchaseSuccess.value = false
                _purchaseError.value = e.message ?: "An error occurred"
            }
        }
    }
    //Add Get User Use Case
    suspend fun getUser(userId: String): User?{
        return try {
            getUserUseCase(userId).first() // Use the UseCase
        } catch (e:Exception){
            null
        }
    }
    // In EventDetailsViewModel
    // Corrected implementation
    suspend fun getUserSingle(userId: String): User? {
        return try {
            getUserUseCase(userId).first()  // Call the use case directly
        } catch (e: Exception) {
            Log.e("EventDetailsViewModel", "Error getting user: ${e.message}", e)
            null
        }
    }

    fun resetPurchaseStatus() {
        _purchaseSuccess.value = false
        _purchaseError.value = null
    }

    fun getFallbackLocation(): GeoPoint {
        return loadStoredLocation() ?: GeoPoint(-1.286389, 36.817223) // Nairobi CBD
    }

    fun updateUserLocation(onSuccess: (GeoPoint) -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            _isLoadingLocation.value = true  // Start loading
            try {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null) // Use high accuracy
                    .await()

                val finalLocation = location?.let{
                    GeoPoint(it.latitude, it.longitude) // Create GeoPoint
                } ?: getFallbackLocation() //Use Fallback

                saveLocation(finalLocation)
                onSuccess(finalLocation) // Success Callback

            } catch (e: Exception){
                val fallback = getFallbackLocation() // Fallback
                saveLocation(fallback)
                onFailure() // Failure Callback

            } finally {
                _isLoadingLocation.value = false // Stop Loading
            }
        }
    }
}