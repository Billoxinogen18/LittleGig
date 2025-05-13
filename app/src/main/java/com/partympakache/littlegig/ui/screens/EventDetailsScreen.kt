package com.partympakache.littlegig.ui.screens

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import kotlinx.coroutines.flow.first // Add this import
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.GeoPoint
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.ui.components.CustomToast
import com.partympakache.littlegig.ui.viewmodels.EventDetailsViewModel
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.compose.material.icons.filled.SwitchCamera // Import SwitchCamera Icon
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.hilt.navigation.compose.hiltViewModel
import com.partympakache.littlegig.data.model.User


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(navController: NavController, viewModel: EventDetailsViewModel, eventId: String) {
    // State variables
    val event by viewModel.event.observeAsState()
    val recaps by viewModel.latestRecapsByUser.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var capturedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var recapCaption by remember { mutableStateOf("") }
    val purchaseSuccess by viewModel.purchaseSuccess.observeAsState(initial = false)
    val purchaseError by viewModel.purchaseError.observeAsState(initial = null)

    // NEW: Get location state from ViewModel
    val userLocation by viewModel.userLocation
    val isLoadingLocation by viewModel.isLoadingLocation

    // Permission and UI states
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var showCamera by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val (showToast, setShowToast) = remember { mutableStateOf(false) }
    val (toastMessage, setToastMessage) = remember { mutableStateOf("") }


    // CameraX variables
    val lifecycleOwner = LocalLifecycleOwner.current

    // Define all the launcher references first
    lateinit var locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    lateinit var locationSettingsLauncher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
    lateinit var requestCameraPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>

    // Helper functions
    fun showDistanceError() {
        Toast.makeText(context, "You must be within 3km of the event to post a recap.", Toast.LENGTH_LONG).show()
    }

    fun showLocationError() {
        Toast.makeText(context, "Failed to get location.", Toast.LENGTH_LONG).show()
    }

    val checkLocationSettingsAndGetLocation = { onSuccess: () -> Unit ->
        if (userLocation == null) {
            viewModel.updateUserLocation(
                onSuccess = {
                    if (viewModel.canPostRecap(event?.locationCoordinates)) {
                        onSuccess()
                    } else {
                        showDistanceError()
                    }
                },
                onFailure = { showLocationError() }
            )
        } else {
            onSuccess()
        }
    }
    val requestCameraPermission = {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                showCamera = true  // Directly show camera if permission is granted.
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Request permission
            }
        }
    }
    val switchCamera = {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    // NOW we initialize the launchers
    locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted // Update permission state
        if (isGranted) {
            viewModel.updateUserLocation(
                onSuccess = { }, //Nothing, for now.
                onFailure = { Toast.makeText(context, "Failed to get location.", Toast.LENGTH_LONG).show() }
            )
        } else {
            viewModel.saveLocation(viewModel.getFallbackLocation()) // If permission is denied save fallback location
        }
    }

    locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.updateUserLocation(
                onSuccess = { }, //Nothing, for now.
                onFailure = { Toast.makeText(context, "Failed to get location.", Toast.LENGTH_LONG).show() }
            )
        } else {
            // User denied location services
            viewModel.saveLocation(viewModel.getFallbackLocation()) // If permission is denied save fallback location
            Toast.makeText(context, "Location services must be enabled to post recaps.", Toast.LENGTH_LONG).show()
        }
    }
    requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        showCamera = isGranted // Set the camera state
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to post recaps.", Toast.LENGTH_LONG).show()
        }
    }

    // Initial data loading (runs only once)
    LaunchedEffect(Unit) {
        viewModel.updateLocationInBackground() // Update location in background
        viewModel.fetchEventDetails(eventId)
        viewModel.fetchRecaps(eventId)
    }

    // UI Components
    if (showToast) {
        CustomToast(message = toastMessage, onDismiss = { setShowToast(false) })
    }
    Box(modifier = Modifier.fillMaxSize()) { // Use Box for layering
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Event Details") })
            },
            floatingActionButton = {
                if (event != null) { // Only show if event is loaded
                    FloatingActionButton(
                        onClick = {
                            if (userLocation != null) { // Proceed if the user location is stored.
                                requestCameraPermission()
                            } else {
                                checkLocationSettingsAndGetLocation { requestCameraPermission() } //Try again to fetch location
                            }

                        },
                        //Corrected logic for loading
                        modifier = Modifier.alpha(if (isLoadingLocation && userLocation == null) 0.5f else 1f),
                        containerColor = if (isLoadingLocation && userLocation == null)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.primary
                    ) {
                        //Corrected logic for showing loading
                        if (isLoadingLocation && userLocation == null) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.AddAPhoto, "Post Recap")
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Event details
                event?.let { currentEvent ->
                    EventDetailsContent(currentEvent)
                    RecapsSection(recaps, navController, viewModel)

                    Button(onClick = { coroutineScope.launch { viewModel.buyTicket(currentEvent.eventId) } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Buy Tickets")
                    }

                    if (FirebaseHelper.getCurrentUserId() == currentEvent.organizerId) {
                        Button(onClick = { navController.navigate("editEvent/${currentEvent.eventId}") }, modifier = Modifier.fillMaxWidth()) {
                            Text("Edit Event")
                        }
                    }

                } ?: Text("Loading event details...")

                // Location permission request section
                if (!hasLocationPermission) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Location Permission to Post Recaps")
                    }
                }

                // AddRecapDialog (if capturedMediaUri is not null)
                if (capturedMediaUri != null) {
                    AddRecapDialog(
                        mediaUri = capturedMediaUri!!,
                        onCaptionChange = { recapCaption = it },
                        onPost = {
                            coroutineScope.launch {
                                event?.let {
                                    viewModel.postRecap(eventId, recapCaption, capturedMediaUri!!)
                                    Toast.makeText(context, "Recap posted successfully!", Toast.LENGTH_SHORT).show()
                                }
                                capturedMediaUri = null
                                recapCaption = ""
                            }
                        },
                        onDismiss = { capturedMediaUri = null; recapCaption = "" }
                    )
                }
            }
        }

        // Full-screen camera (conditionally rendered on top)
        if (showCamera) {
            CameraOverlay( // Use the new CameraOverlay composable
                onImageCaptured = { capturedUri ->
                    capturedMediaUri = capturedUri
                    showCamera = false
                },
                onDismiss = { showCamera = false },
                switchCamera = { switchCamera() },
                lensFacing = lensFacing
            )
        }
    }

    // Display purchase success/failure messages
    if (purchaseSuccess) {
        LaunchedEffect(key1 = purchaseSuccess) {
            Toast.makeText(context, "Ticket Purchased Successfully", Toast.LENGTH_LONG).show()
            viewModel.resetPurchaseStatus()  // Reset state
        }
    }

    if (purchaseError != null) {
        LaunchedEffect(key1 = purchaseError) {
            Toast.makeText(context, purchaseError!!, Toast.LENGTH_LONG).show()
            viewModel.resetPurchaseStatus() // Reset state
        }
    }



}






@Composable
fun EventDetailsContent(event: Event) {
    // Format date to the required format: Monday, 13th 2020
    val formattedDate = remember(event.date) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(event.date)
            val outputFormat = SimpleDateFormat("EEEE, dd'th' MMMM yyyy", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // Fallback to original date if parsing fails
            event.date
        }
    }

    Column {
        AsyncImage(
            model = event.imageUrl,
            contentDescription = "Event Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = event.title, style = MaterialTheme.typography.headlineMedium)
        Text(text = event.description, style = MaterialTheme.typography.bodyLarge)
        Text(text = "$formattedDate ${event.time}", style = MaterialTheme.typography.bodyMedium)
        Text(text = event.locationName, style = MaterialTheme.typography.bodyMedium)
        Text(text = "KSH ${event.price}", style = MaterialTheme.typography.bodyMedium)
    }
}

// Inside EventDetailsScreen
@Composable
fun RecapsSection(recaps: List<Recap>, navController: NavController, viewModel: EventDetailsViewModel) {
    val currentUserId = FirebaseHelper.getCurrentUserId()

    if (recaps.isNotEmpty()) {
        Text("Recaps", style = MaterialTheme.typography.headlineSmall)

        // Sort recaps (most engaging first, user's recap at the start)
        val sortedRecaps = remember(recaps, currentUserId) {
            recaps.sortedByDescending { recap ->
                val score = recap.likes.size + recap.views
                if (recap.userId == currentUserId) Int.MAX_VALUE else score
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(sortedRecaps, key = { it.recapId }) { recap ->
                RecapGridItem(recap, navController)
            }
        }
    } else {
        Text("No recaps available for this event yet.")
    }
}

@Composable
fun RecapGridItem(recap: Recap, navController: NavController) {
    val isCurrentUserRecap = recap.userId == FirebaseHelper.getCurrentUserId()
    val viewModel: EventDetailsViewModel = hiltViewModel() //You don't need this here

    // Fetch user data using LaunchedEffect, handling null/errors correctly
    var user by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(recap.userId) {
        try {
            val fetchedUser = viewModel.getUserSingle(recap.userId)
            if (fetchedUser != null) {
                user = fetchedUser
            }
        } catch (e: Exception) {
            Log.e("RecapGridItem", "Error fetching user: ${e.message}", e) //Keep as Log.e
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { navController.navigate("recapScreen/${recap.userId}/${recap.eventId}") } // IMPORTANT CHANGE

            .then(
                if (isCurrentUserRecap) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RectangleShape
                ) else Modifier
            ) // Conditional border
    ) {
        AsyncImage(
            model = recap.mediaUrl,
            contentDescription = "Recap Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay for likes and views (bottom-left of the image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Likes",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "${recap.likes.size}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )
            Icon(
                Icons.Filled.Visibility,
                contentDescription = "Views",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "${recap.views}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Current user indicator (top-left)
        if (isCurrentUserRecap) {
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

//Remove Recaps and RecapItem, they are not needed, replace with those above.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecapDialog(
    mediaUri: Uri,
    onCaptionChange: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) } // Add loading state

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Recap",
                style = MaterialTheme.typography.titleLarge, // Use a larger title style
                modifier = Modifier.padding(bottom = 8.dp) // Add some padding
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(), // Fill width
                horizontalAlignment = Alignment.CenterHorizontally // Center content
            ) {
                // Image Preview (with rounded corners)
                AsyncImage(
                    model = mediaUri,
                    contentDescription = "Recap Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)  // Increased height
                        .clip(RoundedCornerShape(12.dp)) // Rounded corners!
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)), // Add a subtle border
                    contentScale = ContentScale.Crop // Crop to fill bounds
                )

                Spacer(modifier = Modifier.height(16.dp)) // More spacing

                OutlinedTextField(
                    value = caption,
                    onValueChange = { onCaptionChange(it); caption = it; },
                    label = { Text("Caption (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp)) // More Spacing
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isPosting) { // Prevent multiple clicks
                        isPosting = true // Set loading state
                        onPost()
                    }
                },
                enabled = !isPosting // Disable button while posting
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp)) // Show loading indicator
                } else {
                    Text("Post")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,

        )
}
@Composable
private fun CameraOverlay(
    onImageCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit,
    switchCamera: () -> Unit,
    lensFacing: Int
) {
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val takePhoto = takePhoto@{
        val imageCaptureInstance = imageCapture ?: return@takePhoto
        val photoFile = File(context.cacheDir, "recap_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCaptureInstance.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    onImageCaptured(savedUri) // Use the callback
                    // showCamera = false // Don't need this here
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(context, "Failed to capture photo: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }


    Surface( // Use Surface for proper theming and background
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Black background for the camera
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera preview (fills the entire Box)
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                imageCapture = { imageCapture = it },
                lensFacing = lensFacing
            )

            // Camera controls (overlayed on top of the preview)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp), // Add padding for better spacing
                verticalArrangement = Arrangement.SpaceBetween // Distribute space
            ) {
                // Top bar (Dismiss and Switch Camera)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp) // Good touch target size
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close, // Use a standard close icon
                            contentDescription = "Close camera",
                            tint = Color.White // White icon for visibility
                        )
                    }

                    IconButton(
                        onClick = switchCamera,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwitchCamera,
                            contentDescription = "Switch camera",
                            tint = Color.White
                        )
                    }
                }

                // Capture button (centered at the bottom)
                Box(modifier = Modifier.fillMaxWidth()) {
                    CaptureButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = {
                            imageCapture?.let { takePhoto() }
                        }
                    )
                }

            }
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: (ImageCapture) -> Unit,
    lensFacing: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER // Use FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView ->
                val executor = ContextCompat.getMainExecutor(context)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Optimize for low latency
                        .build()

                    imageCapture(capture) // Update the imageCapture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build(),
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.e("CameraPreview", "Failed to start camera", e)
                    }
                }, executor)
            }
        },
        modifier = modifier
    )
}
@Composable
private fun CaptureButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp) // Larger, easier to tap
            .border(2.dp, Color.White, CircleShape) // Clear visual indication
    ) {
        Box(
            modifier = Modifier
                .size(64.dp) // Inner circle
                .background(Color.White.copy(alpha = 0.3f), CircleShape) // Semi-transparent white
        )
    }
}
