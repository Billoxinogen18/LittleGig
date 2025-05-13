package com.partympakache.littlegig.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.ui.viewmodels.EditEventViewModel
import com.google.firebase.firestore.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventScreen(navController: NavController, viewModel: EditEventViewModel, eventId: String) {
    val event by viewModel.event.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = false)
    val saveSuccess by viewModel.saveSuccess.observeAsState(initial = false)

    // Fetch event details
    LaunchedEffect(eventId) {
        viewModel.fetchEvent(eventId)
    }

    // Show a loading indicator while fetching or saving
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return // Important: Exit the composable while loading
    }

    // Show a success message if saving was successful
    if (saveSuccess) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismiss */ },
            title = { Text("Success") },
            text = { Text("Event updated successfully!") },
            confirmButton = {
                Button(onClick = {
                    navController.popBackStack() // Navigate back
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Display the edit form
    event?.let { currentEvent ->
        EditEventForm(
            event = currentEvent,
            onSave = { updatedEvent ->
                viewModel.saveEvent(updatedEvent)
            },
            onCancel = { navController.popBackStack() }
        )
    } ?: Text("Event not found") // Display an error if event is null

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventForm(event: Event, onSave: (Event) -> Unit, onCancel: () -> Unit) {
    // Use mutableStateOf for each field, initialized with the event's current values.
    var title by remember { mutableStateOf(event.title) }
    var description by remember { mutableStateOf(event.description) }
    var date by remember { mutableStateOf(event.date) }
    var time by remember { mutableStateOf(event.time) }
    var locationName by remember { mutableStateOf(event.locationName) }
    // For simplicity, we'll use String for lat/long.  In a real app, use a proper location picker.
    var latitude by remember { mutableStateOf(event.locationCoordinates.latitude.toString()) }
    var longitude by remember { mutableStateOf(event.locationCoordinates.longitude.toString()) }
    var price by remember { mutableStateOf(event.price.toString()) }
    //imageUrl is not added, since image upload is handled by the user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Enable scrolling if content overflows
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time (HH:mm)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Location Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Latitude") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Price") },
            modifier = Modifier.fillMaxWidth()
        )

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = {
                // Create an updated Event object.  Handle potential errors (e.g., invalid numbers).
                val updatedEvent = try {
                    event.copy(
                        title = title,
                        description = description,
                        date = date,
                        time = time,
                        locationName = locationName,
                        locationCoordinates = GeoPoint(latitude.toDouble(), longitude.toDouble()),
                        price = price.toDouble()
                    )
                } catch (e: NumberFormatException) {
                    // Handle the error (e.g., show a Snackbar)
                    // Toast or Snackbar or some other message
                    return@Button
                }
                onSave(updatedEvent)
            }) {
                Text("Save")
            }
        }
    }
}