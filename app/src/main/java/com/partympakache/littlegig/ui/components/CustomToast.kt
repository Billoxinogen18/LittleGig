package com.partympakache.littlegig.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay


@Composable
fun CustomToast(message: String, onDismiss: () -> Unit) {
    // Use a Snackbar.  It's more flexible than a Toast, and integrates well with Compose.
     Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) { //So that toast appears at the bottom
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                // You could add an action button here (e.g., "Dismiss")
            }
        ) {
            Text(message)
        }
     }


    // Automatically dismiss after a delay
    LaunchedEffect(message) {
        delay(3000) // 3 seconds
        onDismiss()
    }
}