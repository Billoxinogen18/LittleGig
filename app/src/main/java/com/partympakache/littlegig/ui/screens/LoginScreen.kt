package com.partympakache.littlegig.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.partympakache.littlegig.data.model.CountryData
import com.partympakache.littlegig.ui.components.CountryCodePicker
import com.partympakache.littlegig.ui.components.CustomToast
import com.partympakache.littlegig.ui.viewmodels.LoginViewModel
import com.partympakache.littlegig.utils.getCountryCode
import com.partympakache.littlegig.utils.getCountryFlag
import com.partympakache.littlegig.utils.getCountryISOFromCode
import com.partympakache.littlegig.utils.getCountryName
import com.partympakache.littlegig.utils.formatPhoneNumberWithLibrary
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel() // Can be injected or use default hiltViewModel
) {
    var phoneNumberInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    // Observe states from ViewModel
    val isOtpSent by viewModel.otpSent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val context = LocalContext.current // For Toasts or other context-dependent operations
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Initialize country picker with user's current locale
    val initialCountryIsoCode = remember { Locale.getDefault().country }
    var selectedCountry by remember {
        mutableStateOf(
            CountryData(
                countryCode = getCountryCode(initialCountryIsoCode), // e.g., "254" for KE
                countryName = initialCountryIsoCode, // Store ISO code like "KE", "US"
                countryFlag = getCountryFlag(initialCountryIsoCode)
            )
        )
    }

    // Toast state
    val (showToast, setShowToast) = remember { mutableStateOf(false) }
    var toastMessageText by remember { mutableStateOf("") }

    // Effect to show toast when errorMessage changes
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            toastMessageText = it
            setShowToast(true)
            viewModel.clearErrorMessage() // Optionally clear error in VM after showing
        }
    }

    // Effect to navigate when isLoggedIn state changes to true
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate("eventList") { // Navigate to your main screen
                popUpTo("login") { inclusive = true } // Clear login from back stack
            }
        }
    }

    if (showToast) {
        CustomToast(message = toastMessageText, onDismiss = { setShowToast(false) })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login with Phone") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Box( // Use Box to overlay loading indicator if needed
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isOtpSent) {
                    // --- Phone Number Input Stage ---
                    Text("Enter your phone number:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CountryCodePicker(
                            selectedCountry = selectedCountry,
                            onCountrySelected = { country -> selectedCountry = country },
                            defaultSelectedCountry = selectedCountry, // Pass the current state
                            pickedCountry = { country -> selectedCountry = country } // Update state on pick
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = { phoneNumberInput = it },
                            label = { Text("Phone Number") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone Icon"
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            // Ensure countryName (which we're using as ISO code) is correct
                            Log.d("LoginScreen", "Selected Country ISO for formatting: ${selectedCountry.countryName}")
                            val fullPhoneNumber = formatPhoneNumberWithLibrary(phoneNumberInput, selectedCountry.countryName)

                            if (fullPhoneNumber != null) {
                                Log.d("LoginScreen", "Formatted number for OTP: $fullPhoneNumber")
                                viewModel.sendOtp(fullPhoneNumber)
                            } else {
                                toastMessageText = "Invalid phone number format for ${selectedCountry.countryName}."
                                setShowToast(true)
                                Log.w("LoginScreen", "Invalid phone number input: $phoneNumberInput with country ISO ${selectedCountry.countryName}")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading && !isOtpSent) { // Show loading only for this stage
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Send OTP")
                        }
                    }
                } else {
                    // --- OTP Input Stage ---
                    Text("Enter the OTP sent to your phone:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it.filter { char -> char.isDigit() } }, // Limit OTP length and allow only digits
                        label = { Text("OTP (6 digits)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            if (otpInput.length == 6) {
                                viewModel.verifyOtp(otpInput)
                            } else {
                                toastMessageText = "OTP must be 6 digits."
                                setShowToast(true)
                            }
                        }),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            if (otpInput.length == 6) {
                                viewModel.verifyOtp(otpInput)
                            } else {
                                toastMessageText = "OTP must be 6 digits."
                                setShowToast(true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading && isOtpSent) { // Show loading only for this stage
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Verify OTP")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.resetOtpSentState() }, enabled = !isLoading) {
                        Text("Enter a different number?")
                    }
                }
            }

            // General loading overlay (covers the whole screen if isLoading is true globally)
            // This is useful if there's loading not specific to OTP send/verify stages
            if (isLoading && LocalInspectionMode.current) { // Check LocalInspectionMode to avoid issues in previews
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)) // Semi-transparent overlay
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
