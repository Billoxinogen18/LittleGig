package com.partympakache.littlegig.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.partympakache.littlegig.domain.usecase.CreateUserIfNotExistsUseCase
import com.partympakache.littlegig.domain.usecase.GetLoginStateUseCase
import com.partympakache.littlegig.domain.usecase.SaveLoginStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan_tennert.supabase.gotrue.GoTrue
import io.github.jan_tennert.supabase.gotrue.OtpType
import io.github.jan_tennert.supabase.gotrue.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val saveLoginStateUseCase: SaveLoginStateUseCase,
    private val getLoginStateUseCase: GetLoginStateUseCase,
    private val createUserIfNotExistsUseCase: CreateUserIfNotExistsUseCase,
    private val supabaseAuth: GoTrue // Injected Supabase GoTrue via Hilt
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPhoneNumber: String = "" // Store the phone number used for OTP sending

    init {
        // Observe session status from Supabase GoTrue
        // This helps in automatically updating login state if session changes elsewhere
        viewModelScope.launch {
            supabaseAuth.sessionStatus.collectLatest { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        if (!_isLoggedIn.value) { // Only update if state is different
                            Log.d("LoginViewModel", "Supabase session authenticated. User: ${status.session.user?.id}")
                            saveLoginState(true) // Persist this state
                            _isLoggedIn.value = true
                            // Optionally, trigger user data fetch or other post-login actions
                            status.session.user?.let { user ->
                                createUserIfNotExistsUseCase(user.id, user.phone ?: currentPhoneNumber)
                            }
                        }
                    }
                    SessionStatus.NotAuthenticated -> {
                        if (_isLoggedIn.value) { // Only update if state is different
                            Log.d("LoginViewModel", "Supabase session not authenticated.")
                            saveLoginState(false)
                            _isLoggedIn.value = false
                        }
                    }
                    SessionStatus.LoadingFromStorage -> {
                        Log.d("LoginViewModel", "Supabase session loading from storage.")
                        // Can set an intermediate loading state if needed
                    }
                    is SessionStatus.NetworkError -> {
                        Log.e("LoginViewModel", "Supabase session network error: ${status.exception.message}")
                        // Handle network error, maybe show a toast
                        _errorMessage.value = "Network error. Please check your connection."
                    }
                }
            }
        }
        // Initial check from DataStore, Supabase sessionStatus will then take over
        checkLoginStatusFromDataStore()
    }

    private fun checkLoginStatusFromDataStore() {
        viewModelScope.launch {
            getLoginStateUseCase().collectLatest { storedLoginState ->
                // This sets the initial state. Supabase sessionStatus collector will refine it.
                _isLoggedIn.value = storedLoginState
                if (storedLoginState && supabaseAuth.currentUserOrNull() == null) {
                    // DataStore says logged in, but Supabase has no session. This could be an invalid state.
                    // Attempt to refresh or treat as logged out. For now, we defer to sessionStatus.
                    Log.w("LoginViewModel", "DataStore indicates logged in, but no active Supabase session found initially.")
                }
            }
        }
    }


    fun saveLoginState(isLoggedIn: Boolean) {
        viewModelScope.launch {
            saveLoginStateUseCase(isLoggedIn)
            _isLoggedIn.value = isLoggedIn // Update local state immediately
        }
    }

    fun sendOtp(phoneNumber: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPhoneNumber = phoneNumber // Store for OTP verification
            try {
                // Supabase GoTrue client handles sending OTP
                supabaseAuth.sendOtp(phone = phoneNumber) // Default type is SMS, createAccount is true by default
                _otpSent.value = true
                Log.d("LoginViewModel", "OTP Sent to $phoneNumber via Supabase")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error sending OTP (Supabase)", e)
                _errorMessage.value = "Failed to send OTP: ${e.localizedMessage ?: "Unknown error"}"
                _otpSent.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (currentPhoneNumber.isBlank()) {
                _errorMessage.value = "Phone number not set for OTP verification."
                _isLoading.value = false
                return@launch
            }
            try {
                // Supabase GoTrue client handles OTP verification
                val session = supabaseAuth.verifyOtp(
                    phone = currentPhoneNumber,
                    token = otp,
                    type = OtpType.SMS // Ensure this matches the type used for sending
                )
                // Session object contains user info upon successful verification
                val user = session.user
                if (user != null) {
                    Log.d("LoginViewModel", "OTP Verified with Supabase. User ID: ${user.id}, Phone: ${user.phone}")
                    // Create user in your own 'users' table if they don't exist
                    createUserIfNotExistsUseCase(user.id, user.phone ?: currentPhoneNumber)
                    saveLoginState(true) // Persist login state
                    // _isLoggedIn.value is already updated by sessionStatus collector
                    _otpSent.value = false // Reset OTP state
                } else {
                    // Should not happen if verifyOtp succeeds, but handle defensively
                    _errorMessage.value = "OTP Verification failed: User data not found in session."
                    saveLoginState(false)
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error verifying OTP (Supabase)", e)
                _errorMessage.value = "OTP Verification failed: ${e.localizedMessage ?: "Invalid OTP or an error occurred."}"
                saveLoginState(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetOtpSentState() {
        _otpSent.value = false
        currentPhoneNumber = ""
        _errorMessage.value = null // Clear any previous error messages
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Call this method when user explicitly logs out
    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                supabaseAuth.logout() // Clears Supabase session
                saveLoginState(false) // Update DataStore
                // _isLoggedIn.value will be updated by sessionStatus collector
                _otpSent.value = false
                currentPhoneNumber = ""
                Log.d("LoginViewModel", "User logged out from Supabase.")
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error during Supabase logout", e)
                _errorMessage.value = "Logout failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
