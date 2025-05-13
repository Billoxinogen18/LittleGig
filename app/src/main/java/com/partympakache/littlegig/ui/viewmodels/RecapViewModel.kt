package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.domain.usecase.GetAllRecapsForEventUseCase
import com.partympakache.littlegig.domain.usecase.GetRecapsForEventUseCase
import com.partympakache.littlegig.domain.usecase.GetUserUseCase
import com.partympakache.littlegig.domain.usecase.LikeRecapUseCase
import com.partympakache.littlegig.domain.usecase.UnLikeRecapUseCase
import com.partympakache.littlegig.domain.usecase.ViewRecapUseCase
import com.partympakache.littlegig.utils.FirebaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecapViewModel @Inject constructor(
    private val getRecapsForEventUseCase: GetRecapsForEventUseCase,
    private val getAllRecapsForEventUseCase: GetAllRecapsForEventUseCase, // For grouping
    private val getUserUseCase: GetUserUseCase,
    private val likeRecapUseCase: LikeRecapUseCase,
    private val unLikeRecapUseCase: UnLikeRecapUseCase,
    private val viewRecapUseCase: ViewRecapUseCase
) : ViewModel() {

    private val _currentRecapIndex = MutableStateFlow(0)
    val currentRecapIndex: StateFlow<Int> = _currentRecapIndex.asStateFlow()

    private val _recaps = MutableStateFlow<List<Recap>>(emptyList())
    val recaps: StateFlow<List<Recap>> = _recaps.asStateFlow()

    private val _allRecaps = MutableStateFlow<List<Recap>>(emptyList())
    val allRecaps: StateFlow<List<Recap>> = _allRecaps.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private var eventId: String? = null  // Store eventId
    private var userId: String? = null // Store userId


    fun fetchRecaps(eventId: String, userId: String) {
        this.eventId = eventId // Store for later use
        this.userId = userId

        viewModelScope.launch {
            getRecapsForEventUseCase(eventId).collect { recapList ->
                // Filter recaps for the current user.
                val userRecaps = recapList.filter { it.userId == userId }
                _recaps.value = userRecaps
                if (userRecaps.isNotEmpty()) {
                    // Fetch and set user information
                    viewModelScope.launch {
                        _currentUser.value = getUser(userId)
                    }
                }
            }
        }
    }

    //Fetches all recaps for grouping
    fun fetchAllRecapsForEvent(eventId: String) {
        viewModelScope.launch {
            getAllRecapsForEventUseCase(eventId).collect { allRecaps ->
                _allRecaps.value = allRecaps
            }
        }
    }

    suspend fun getUser(userId: String): User? {
        return try {
            getUserUseCase(userId).first()
        } catch (e: Exception) {
            Log.e("RecapViewModel", "Error fetching user", e)
            null
        }
    }

    fun nextRecap() {
        if (_currentRecapIndex.value < _recaps.value.size - 1) {
            _currentRecapIndex.value++
        }
    }

    fun previousRecap() {
        if (_currentRecapIndex.value > 0) {
            _currentRecapIndex.value--
        }
    }

    // Reset recap index to 0 (used when switching users)
    fun resetRecapIndex() {
        _currentRecapIndex.value = 0
    }
    //Set to a new index
    fun setRecapIndex(index: Int) {
        _currentRecapIndex.value = index
    }

    fun likeRecap(recapId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseHelper.getCurrentUserId()
                if(userId != null){
                    likeRecapUseCase(recapId, userId) // Use Case

                }

            } catch (e: Exception) {
                Log.e("RecapViewModel", "Error liking recap", e)
            }
        }
    }

    fun unlikeRecap(recapId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseHelper.getCurrentUserId()
                if(userId != null){
                    unLikeRecapUseCase(recapId, userId)// Use Case
                }

            } catch (e: Exception) {
                Log.e("RecapViewModel", "Error unliking recap", e)
            }
        }
    }
    //View Recap Use Case
    fun viewRecap(recapId: String) {
        viewModelScope.launch {
            try {
                val userId = FirebaseHelper.getCurrentUserId()
                userId?.let {
                    viewRecapUseCase(recapId, it)
                }
            } catch (e: Exception) {
                Log.e("RecapViewModel", "Error viewing recap", e)
            }
        }
    }
}