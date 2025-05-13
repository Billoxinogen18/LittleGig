package com.partympakache.littlegig.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.partympakache.littlegig.data.model.ChatMessage
import com.partympakache.littlegig.ui.viewmodels.ChatViewModel
import com.partympakache.littlegig.utils.FirebaseHelper
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.ui.viewmodels.ConversationViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, viewModel: ChatViewModel) {
    val conversations by viewModel.conversations.observeAsState(initial = emptyMap())
    val users by viewModel.users.observeAsState(initial = emptyMap())
    val loading by viewModel.loading.observeAsState(initial = true)

    LaunchedEffect(Unit) {
        viewModel.fetchConversations()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chats") })
        }
    ) { innerPadding ->
        if (loading) {
            // Show loading indicator
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        }else{
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(conversations.toList()) { (receiverId, lastMessage) ->
                    val user = users[receiverId] // Get User object
                    if (user != null) {
                        ConversationItem(user, lastMessage) {
                            // Navigate to individual chat screen with receiver's ID and Name
                            navController.navigate("conversation/${user.userId}/${user.displayName}")
                        }
                    }

                }
            }

        }

    }
}

@Composable
fun ConversationItem(user: User, lastMessage: ChatMessage, onItemClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onItemClick),
        elevation =  CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Image(
                painter = rememberAsyncImagePainter(model = user.profileImageUrl),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = user.displayName ?: "Unknown User", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier= Modifier.height(4.dp))
                Text(
                    text = lastMessage.messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                // Show active now
                if (user.isActiveNow) {
                    Text(text = "Active now", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}