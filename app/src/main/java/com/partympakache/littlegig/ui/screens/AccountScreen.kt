package com.partympakache.littlegig.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.ui.viewmodels.AccountViewModel
import com.partympakache.littlegig.utils.FirebaseHelper
import kotlinx.coroutines.launch
// Make sure this import is present if you use viewModel() directly
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(navController: NavController, viewModel: AccountViewModel) {
    val currentUser by viewModel.currentUser.observeAsState()
    val recaps by viewModel.userRecaps.observeAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        FirebaseHelper.getCurrentUserId()?.let { userId ->
            viewModel.fetchUserData(userId)
            viewModel.fetchUserRecaps(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentUser?.displayName ?: "Profile") }, // Dynamic title
                actions = {
                    Button(onClick = {
                        coroutineScope.launch {
                            viewModel.logout()
                            navController.navigate("login") {
                                popUpTo("eventList") { inclusive = true }
                            }
                        }
                    }) {
                        Text("Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Subtle elevation
                )
            )
        }
    ) { innerPadding ->
        currentUser?.let { user ->
            AccountContent(
                user = user,
                recaps = recaps,
                navController = navController,
                viewModel = viewModel, // Pass the viewModel instance
                modifier = Modifier.padding(innerPadding)
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AccountContent(
    user: User,
    recaps: List<Recap>,
    navController: NavController,
    viewModel: AccountViewModel, // Added viewModel parameter
    modifier: Modifier = Modifier
) {
    var isPrivate by remember { mutableStateOf(user.visibilitySetting == "private") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Consistent padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp)) // More space at the top

        // Profile Picture
        Image(
            painter = rememberAsyncImagePainter(model = user.profileImageUrl ?: ""), // Handle null URI
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp) // Larger profile picture
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), // Optional border
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display Name
        Text(
            text = user.displayName ?: "User", // Fallback name
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        // Username/Handle (Example - if you add this field to your User model)
        // Text(
        //     text = "@${user.username ?: "username"}",
        //     style = MaterialTheme.typography.bodyMedium,
        //     color = MaterialTheme.colorScheme.onSurfaceVariant
        // )

        Spacer(modifier = Modifier.height(8.dp))

        // Bio
        user.bio?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp) // Padding for longer bios
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Followers, Following, Posts/Recaps Count
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserStat(count = recaps.size.toString(), label = "Posts")
            UserStat(count = user.followers.size.toString(), label = "Followers")
            UserStat(count = user.following.size.toString(), label = "Following")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Edit Profile / Account Settings Button (Example)
        Button(
            onClick = { /* Navigate to edit profile or settings */ },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Account Settings") // Or "Edit Profile"
        }


        Spacer(modifier = Modifier.height(16.dp))
        // Active Now Visibility Setting
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Private Account", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isPrivate,
                onCheckedChange = {
                    isPrivate = it
                    viewModel.updateVisibilitySetting(if (isPrivate) "private" else "public")
                }
            )
        }


        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // User's Recaps Grid
        if (recaps.isNotEmpty()) {
            Text(
                "My Recaps",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3), // 3 items per row
                modifier = Modifier.fillMaxWidth(),// Removed fixed height to allow it to expand
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(recaps, key = { it.recapId }) { recap ->
                    RecapGridItemAc(recap = recap, navController = navController)
                }
            }
        } else {
            Text(
                "No Recaps Yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun UserStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp) // Adjusted style
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall, // Smaller label
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
fun RecapGridItemAc(recap: Recap, navController: NavController) { // Re-using from EventDetailsScreen for consistency
    Card( // Wrap in Card for better visual separation and click effect
        modifier = Modifier
            .aspectRatio(1f) // Make items square
            .clickable {
                navController.navigate("recapScreen/${recap.userId}/${recap.eventId}")
            },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = recap.mediaUrl,
                contentDescription = "Recap Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Optional: Add a small overlay for views/likes if needed, similar to RecapStoryItem
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon(Icons.Filled.Visibility, contentDescription = "Views", tint = Color.White, modifier = Modifier.size(12.dp))
                    // Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${recap.views} views", // Example text
                        color = Color.White,
                        fontSize = 10.sp, // Smaller font for overlay
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

// RecapStoryItem might not be needed if the grid is preferred,
// but keeping it here if you want a "stories" like row elsewhere.
@Composable
fun RecapStoryItem(recap: Recap, navController: NavController) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable {
                navController.navigate("eventDetails/${recap.eventId}")
            }
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = recap.mediaUrl),
            contentDescription = "Recap Thumbnail",
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(2.dp)
        ) {
            Text(
                text = "${recap.views}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}