package com.partympakache.littlegig.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

import com.partympakache.littlegig.com.R
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.ui.viewmodels.MapViewModel
import com.partympakache.littlegig.utils.FirebaseHelper
import com.partympakache.littlegig.utils.MapUtils
import com.partympakache.littlegig.utils.MapUtils.bitmapDescriptorFromVector
import com.partympakache.littlegig.utils.MapUtils.toBitmap
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: MapViewModel) {
    val events by viewModel.events.observeAsState(initial = emptyList())
    val activeUsers by viewModel.activeUsers.collectAsState()
    val context = LocalContext.current

    val cameraPositionState = rememberCameraPositionState {
        // Initially, we don't know the location. Don't set a position here.
        //position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 10f) //Initial, we will set it on user location
    }

    val sheetState = rememberModalBottomSheetState()
    val (selectedUser, setSelectedUser) = remember<MutableState<User?>> { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    // Location Permission Handling (Same as before, but we'll use the result)
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            // Get and center on location *after* permission is granted
            coroutineScope.launch {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                try {
                    val location = fusedLocationClient.lastLocation.await() // Get last known location
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 15f) // Zoom level 15
                            , 1000) //Animate within 1 second
                    }
                } catch (e: SecurityException) {
                    //Toast
                }
            }
        }
    }

    // Request permission and fetch data on initial composition
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        viewModel.fetchEvents()
        viewModel.fetchActiveUsers() // Add this
    }


    //Observe events and update camera bounds
    LaunchedEffect(events, activeUsers, hasLocationPermission) { // Key on hasLocationPermission
        if (!hasLocationPermission) return@LaunchedEffect // Don't do anything if no permission

        //Existing logic for events.
        if (events.isNotEmpty() || activeUsers.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.builder()

            // Include event locations
            events.forEach { event ->
                boundsBuilder.include(LatLng(event.locationCoordinates.latitude, event.locationCoordinates.longitude))
            }

            // Include active user locations
            activeUsers.forEach { (user, location) ->
                location?.let {
                    boundsBuilder.include(LatLng(it.latitude, it.longitude))
                }
            }

            val bounds = boundsBuilder.build()

            if (bounds.northeast != bounds.southwest) {
                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } else {
                val center = if (events.isNotEmpty()) {
                    LatLng(events.first().locationCoordinates.latitude, events.first().locationCoordinates.longitude)
                } else if (activeUsers.isNotEmpty()) {
                    activeUsers.firstOrNull()?.second?.let {
                        LatLng(it.latitude, it.longitude)
                    } ?: LatLng(0.0, 0.0)
                } else {
                    LatLng(0.0, 0.0)
                }
                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(center, 10f))
            }
        } else { //If no locations, still update
            // Permission is granted, get the location and update the camera.
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try{
                val location = fusedLocationClient.lastLocation.await()
                if(location != null){
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f), 1000) // Zoom level 15
                }
            } catch(e: SecurityException){
                //Toast
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Display Event Markers
            events.forEach { event ->
                val eventLatLng = LatLng(event.locationCoordinates.latitude, event.locationCoordinates.longitude)
                Marker(
                    state = MarkerState(position = eventLatLng),
                    title = event.title,
                    snippet = event.locationName,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) // Default Icon
                )
            }

            // Display Active User Markers
            activeUsers.forEach { (user, location) ->
                location?.let { geoPoint ->
                    val userLatLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    val currentUserId = FirebaseHelper.getCurrentUserId()

                    val shouldDisplay = when {
                        user.userId == currentUserId -> true
                        user.visibilitySetting == "public" -> true
                        user.visibilitySetting == "private" && user.followers.contains(currentUserId) -> true
                        else -> false
                    }

                    if (shouldDisplay) {
                        val profilePictureUrl = remember { mutableStateOf(user.profileImageUrl) }
                        val context = LocalContext.current
                        val painter = rememberAsyncImagePainter(model = profilePictureUrl.value)

                        Box(
                            modifier = Modifier.size(52.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Image(
                                painter = painter,
                                contentDescription = "User Profile Picture",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        setSelectedUser(user)
                                        coroutineScope.launch { sheetState.show() }
                                    }
                            )
                            // Create the BitmapDescriptor *once* and reuse it.
                            val activeNowIcon = remember(context) {  // Keyed to the context
                                MapUtils.bitmapDescriptorFromVector(context, R.drawable.ic_active_now)
                            }
                            Image(
                                bitmap = activeNowIcon.toBitmap(context).asImageBitmap(), // Convert and use asImageBitmap()
                                contentDescription = "Active Now",
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 4.dp, y = 4.dp)
                            )
                        }

                        Marker(
                            state = MarkerState(position = userLatLng),
                            title = user.displayName,
                            snippet = "Active Now",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) // Use an invisible icon
                        ) {
                            true // Consume
                        }
                    }
                }
            }
        }

        if (events.isEmpty() && activeUsers.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Show BottomSheet when a user is selected
        if (sheetState.isVisible) {
            selectedUser?.let { user ->
                ModalBottomSheet(
                    onDismissRequest = {
                        coroutineScope.launch {
                            sheetState.hide()
                            setSelectedUser(null)
                        }
                    },
                    sheetState = sheetState
                ) {
                    UserProfileBottomSheetContent(user, navController, viewModel)
                }
            }
        }
    }

}

@Composable
fun UserProfileBottomSheetContent(user: User, navController: NavController, viewModel: MapViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // User's Profile Picture (Circular)
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImageUrl),
            contentDescription = "User Profile Picture",
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // User's Display Name
        Text(
            text = user.displayName ?: "No Name",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // User's Bio (Optional)
        user.bio?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Buttons (Message and View Profile)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                // Navigate to chat with this user
                user.userId.let { userId ->
                    navController.navigate("conversation/$userId/${user.displayName}")
                }
            }) {
                Text("Message")
            }

            Button(onClick = {
                // Navigate to user's profile
                navController.navigate("userProfile/${user.userId}")
            }) {
                Text("View Profile")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}