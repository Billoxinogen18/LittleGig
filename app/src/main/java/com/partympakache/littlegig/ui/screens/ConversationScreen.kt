package com.partympakache.littlegig.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.partympakache.littlegig.data.model.ChatMessage
import com.partympakache.littlegig.ui.viewmodels.ConversationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(navController: NavController, viewModel: ConversationViewModel, receiverId: String, receiverName: String) {
    val messages by viewModel.messages.observeAsState(initial = emptyList())
    var newMessageText by remember { mutableStateOf("") }

    LaunchedEffect(receiverId) {
        viewModel.loadMessages(receiverId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chat with $receiverName") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true // Show newest messages at the bottom
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }

            // Message Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessageText,
                    onValueChange = { newMessageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (newMessageText.isNotBlank()) {
                         viewModel.sendMessage(receiverId, newMessageText)
                         newMessageText = "" // Clear input after sending
                    }
                }) {
                    Text("Send")
                }
            }
        }
    }
}
@Composable
fun MessageBubble(message: ChatMessage) {
    val isCurrentUserMessage = message.senderId == com.partympakache.littlegig.utils.FirebaseHelper.getCurrentUserId()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isCurrentUserMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        // Chat Bubble
         androidx.compose.material3.Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isCurrentUserMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, // Different colors
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = message.messageText,
                modifier = Modifier.padding(8.dp),
                color = if (isCurrentUserMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant // Text color
            )
        }
    }
}