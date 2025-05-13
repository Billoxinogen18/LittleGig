package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _userRecaps = MutableLiveData<List<Recap>>() // LiveData for recaps
    val userRecaps: LiveData<List<Recap>> = _userRecaps


     fun fetchUserData(userId: String) {
        viewModelScope.launch {
            try {
                val userDocRef = FirebaseHelper.firestore.collection("users").document(userId)
                val snapshot = userDocRef.get().await()
                _user.value = snapshot.toObject<User>()
            } catch (e: Exception) {
                Log.e("UserProfileViewModel", "Error fetching user data", e)
                // Handle error
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
                Log.e("UserProfileViewModel", "Error fetching user recaps", e)
                // Handle errors, e.g., show an error message to the user
            }
        }
    }
}