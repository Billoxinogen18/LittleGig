package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AccountViewModel : ViewModel() {
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _userRecaps = MutableLiveData<List<Recap>>()
    val userRecaps: LiveData<List<Recap>> = _userRecaps


    fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                val userDocRef = FirebaseHelper.firestore.collection("users").document(userId)
                val snapshot = userDocRef.get().await()
                _currentUser.value = snapshot.toObject<User>()
            } catch (e: Exception) {
                Log.e("AccountViewModel", "Error fetching user data", e)
                // Handle errors appropriately
            }
        }
    }

     fun fetchUserRecaps(userId: String) {
        viewModelScope.launch {
            try {
                val querySnapshot = FirebaseHelper.firestore.collection("recaps")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                _userRecaps.value = querySnapshot.toObjects()
            } catch (e: Exception) {
                 Log.e("AccountViewModel", "Error fetching user recaps", e)
            }
        }
    }

    fun updateVisibilitySetting(setting: String) {
        viewModelScope.launch {
            val userId = FirebaseHelper.getCurrentUserId() ?: return@launch
            try {
                FirebaseHelper.firestore.collection("users").document(userId)
                    .update("visibilitySetting", setting)
                    .await()

                 // Update the local user data as well:
                 _currentUser.value = _currentUser.value?.copy(visibilitySetting = setting)


            } catch (e: Exception) {
                Log.e("AccountViewModel", "Error updating visibility", e)
            }
        }
    }

      suspend fun logout() {
        try {
            Firebase.auth.signOut()
            _currentUser.value = null // Clear current user
            // Additional cleanup if needed
        } catch (e: Exception) {
             Log.e("AccountViewModel", "Error during logout", e)
        }
    }
}