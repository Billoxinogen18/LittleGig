package com.partympakache.littlegig.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer


@Composable
fun ShimmerListItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f) // Title
                    .height(20.dp).placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Description
                    .height(16.dp).placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )

             Spacer(modifier = Modifier.height(4.dp))
             Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Description
                    .height(16.dp).placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )

            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Description
                    .height(16.dp).placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )

             Spacer(modifier = Modifier.height(4.dp))
             Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f) // Description
                    .height(16.dp).placeholder(
                        visible = true,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.medium,
                        highlight = PlaceholderHighlight.shimmer(highlightColor = Color.White)
                    )
            )
        }
    }
}