package com.partympakache.littlegig.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.ui.viewmodels.UserProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(navController: NavController, userId: String, viewModel: UserProfileViewModel) {
    val user by viewModel.user.observeAsState()
    val recaps by viewModel.userRecaps.observeAsState(initial = emptyList())

    LaunchedEffect(userId) {
        viewModel.fetchUserData(userId)
        viewModel.fetchUserRecaps(userId) // Fetch recaps specific to this user
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("User Profile") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            user?.let {
                UserProfileContent(it, recaps, navController) // Pass recaps to content
            } ?: Text("Loading user data...") // Show loading
        }
    }
}

// This composable displays the user's information and recaps.
@Composable
fun UserProfileContent(user: User, recaps: List<Recap>, navController: NavController) {
     // Profile Picture
    Image(
        painter = rememberAsyncImagePainter(model = user.profileImageUrl),
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape),
        contentScale = ContentScale.Crop // Crop the image
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(text = user.displayName ?: "No Name", style = MaterialTheme.typography.headlineSmall)

    Spacer(modifier = Modifier.height(8.dp))

    // User Rank
    Text(text = "Rank: ${user.rank}", style = MaterialTheme.typography.bodyMedium)

    Spacer(modifier = Modifier.height(8.dp))

     // Bio (Optional)
    user.bio?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Followers and Following
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = user.followers.size.toString(), style = MaterialTheme.typography.titleMedium)
            Text(text = "Followers", style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = user.following.size.toString(), style = MaterialTheme.typography.titleMedium)
            Text(text = "Following", style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))


    // Display recaps as stories
    if (recaps.isNotEmpty()) {
        Text("Recaps", style = MaterialTheme.typography.titleMedium)
        LazyRow(
           horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(recaps) { recap ->
                RecapStoryItem(recap = recap, navController = navController)
            }
        }
    } else {
        Text("No Recaps Available", style = MaterialTheme.typography.titleMedium)
    }

}