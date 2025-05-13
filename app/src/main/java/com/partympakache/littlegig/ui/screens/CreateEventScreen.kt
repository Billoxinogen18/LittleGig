package com.partympakache.littlegig.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.GeoPoint
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.clock.ClockDialog
import com.maxkeppeler.sheets.clock.models.ClockSelection
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.ui.components.CustomToast
import com.partympakache.littlegig.ui.viewmodels.CreateEventViewModel
import com.partympakache.littlegig.utils.getCountryCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import android.util.Log
import androidx.compose.material.icons.filled.AddAPhoto
import com.maxkeppeker.sheets.core.models.base.rememberSheetState

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(navController: NavController, viewModel: CreateEventViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Event details state
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var availableTickets by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    // Collect the placePredictions and placeDetails flows from the ViewModel.
    val placePredictions by viewModel.placePredictions.collectAsState()
    val placeDetails by viewModel.placeDetails.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState() // Collect loading state

    // Places API
    val placesClient = Places.createClient(context)  // Create PlacesClient here

    // --- Debouncing for Place Autocomplete ---
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val debounceDuration = 300L // Milliseconds


    val calendarState = rememberSheetState() //For the Calendar
    val clockState = rememberSheetState() //For the clock

    // Image Picker
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            imageUri = uri
        }
    )
    // Calendar and Time Dialogs
    CalendarDialog(
        state = calendarState,
        config = CalendarConfig(
            yearSelection = true,
            monthSelection = true,
            //style = CalendarStyle.MONTH, // UIMode.Companion.Dialog
        ),
        selection = CalendarSelection.Date { localDate ->
            date = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            clockState.show() // Show time picker after date
        },
    )
    ClockDialog(
        state = clockState,
        selection = ClockSelection.HoursMinutes { hours, minutes ->
            val selectedTime = LocalTime.of(hours, minutes)
            time = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        }

    )

    // State for Toast
    val (showToast, setShowToast) = remember { mutableStateOf(false) }
    val (toastMessage, setToastMessage) = remember { mutableStateOf("") }


    if (showToast) {
        CustomToast(message = toastMessage, onDismiss = {setShowToast(false)})
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ), //Added colors
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank() || description.isBlank() || date.isBlank() ||
                                time.isBlank() || placeDetails == null || imageUri == null || availableTickets.isBlank()) {

                                coroutineScope.launch{
                                    setToastMessage("Please fill all fields and select an image.")
                                    setShowToast(true)
                                }
                                return@Button
                            }
                            try {
                                val priceDouble = price.toDouble()
                                val latDouble = placeDetails!!.latLng!!.latitude //Now you have it
                                val lngDouble = placeDetails!!.latLng!!.longitude
                                val tickets = availableTickets.toInt()

                                //Create event object
                                val event = Event(
                                    title = title,
                                    description = description,
                                    date = date,
                                    time = time,
                                    locationName = placeDetails?.name ?: "",  // Use Place name
                                    locationCoordinates = GeoPoint(latDouble, lngDouble), //Now you have them
                                    price = priceDouble,
                                    availableTickets = tickets
                                    // imageUrl will be set after uploading in the ViewModel
                                )
                                isUploading = true
                                coroutineScope.launch {
                                    viewModel.createEvent(event, imageUri,
                                        onSuccess = {
                                            isUploading = false
                                            navController.popBackStack() // Navigate back
                                        },
                                        onFailure = {
                                            isUploading = false
                                            // Optionally show an error message
                                        }
                                    )
                                }


                            } catch (e: NumberFormatException) {
                                // Show error if number conversion
                                coroutineScope.launch{
                                    setToastMessage("Invalid number format for price, latitude, longitude, or tickets.")
                                    setShowToast(true)
                                }

                                return@Button
                            }

                        },
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Post")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column( //Main column
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()), // Enable vertical scrolling
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Image Selection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Increased height
                    .clickable { pickImageLauncher.launch("image/*") } // Launch the picker
                ,
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Event Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)), // Rounded corners
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder:  A simple text, and an Icon.  Much better than just text.
                    Column (horizontalAlignment = Alignment.CenterHorizontally){
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Add Photo",
                            modifier = Modifier.size(48.dp), // Nice big icon
                            tint = MaterialTheme.colorScheme.primary  // Use a color from your theme
                        )
                        Text("Tap to select image", style = MaterialTheme.typography.bodyLarge)
                    }

                }
            }

            // All input fields are now inside Cards, for visual grouping
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                // No elevation to avoid the glitch
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Consistent spacing
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp) // Rounded corners on text fields!
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Event Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        singleLine = false,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date and Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = {},
                            label = { Text("Date") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { calendarState.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = time,
                            onValueChange = {  },
                            label = { Text("Time") },
                            modifier = Modifier.weight(1f),
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { clockState.show() }) {
                                    Icon(Icons.Default.DateRange, contentDescription = null) //Re-used same icon
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    // Location Autocomplete
                    OutlinedTextField(
                        value = locationName,
                        onValueChange = {
                            locationName = it
                            viewModel.searchPlaces(placesClient, it) // Call searchPlaces
                        },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Location")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    // Suggestions (LazyColumn)
                    if (placePredictions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp), // Limit height
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(placePredictions) { prediction ->
                                Text(
                                    text = prediction.getFullText(null).toString(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            // Fetch place details and clear suggestions
                                            viewModel.fetchPlaceDetails(placesClient, prediction.placeId)
                                            locationName = prediction.getPrimaryText(null).toString() // Set the location name
                                            viewModel.clearPredictions()  // Call the ViewModel's method
                                        }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (KSH)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = availableTickets,
                        onValueChange = {availableTickets = it},
                        label = { Text("Available Tickets")},
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }



            if (isUploading) {
                // Use a LinearProgressIndicator for better visual feedback
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}



// --- Helper functions for Places API are now in the ViewModel ---
private fun fetchPlacePredictions(placesClient: PlacesClient, query: String) {
    //Removed since in viewmodel
}

private fun fetchPlaceDetails(placesClient: PlacesClient, placeId: String) {
    //Removed since in view model
}