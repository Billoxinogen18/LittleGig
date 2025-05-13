package com.partympakache.littlegig.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.ui.viewmodels.TicketViewModel
import com.partympakache.littlegig.utils.FirebaseHelper
import com.partympakache.littlegig.utils.QrCodeUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(viewModel: TicketViewModel) {
    val tickets by viewModel.tickets.observeAsState(initial = emptyList())
    val pagerState = rememberPagerState(pageCount = { tickets.size })
    val coroutineScope = rememberCoroutineScope()

    var selectedTicketIndex by remember { mutableStateOf(-1) } // -1 means no ticket selected, focus on current Pager item

    LaunchedEffect(Unit) {
        viewModel.fetchTickets()
    }
    // Update selectedTicketIndex when pagerState.currentPage changes
    // This makes the "selected" ticket always the one in the center of the pager
    LaunchedEffect(pagerState.currentPage) {
        selectedTicketIndex = pagerState.currentPage
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Tickets") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) // A subtle background
        ) {
            if (tickets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (viewModel.tickets.value == null) { // Still loading
                        CircularProgressIndicator()
                    } else { // Loaded but empty
                        Text("No tickets found.", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // To make the next item peek, contentPadding horizontal should be set.
                    // The amount of peek depends on screen width and card width.
                    // Example: If cards take 80% of screen width, padding is 10% on each side.
                    contentPadding = PaddingValues(horizontal = 40.dp), // Adjust for desired peek
                    pageSpacing = 16.dp
                ) { page ->
                    val ticket = tickets[page]
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

                    // Apply transformations for the carousel effect
                    val scale = lerp(
                        start = 0.85f, // Non-center items are smaller
                        stop = 1f,    // Center item is full size
                        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                    )
                    val  ty = lerp(
                        start = 40.dp.value,
                        stop = 0.dp.value,
                        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                    )

                    TicketCardItem(
                        ticket = ticket,
                        isSelected = (selectedTicketIndex == page), // Determine if this card is "selected"
                        modifier = Modifier
                            .graphicsLayer {
                                // Apply scale and slight Y offset for a card stack effect
                                scaleX = scale
                                scaleY = scale
                                translationY = ty

                                // Fade out items further away
                                alpha = lerp(
                                    start = 0.5f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                                )
                                // Optional: Add rotation for a more dynamic feel
                                // rotationZ = lerp(start = if (pageOffset > 0) -20f else 20f, stop = 0f, fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                                transformOrigin = TransformOrigin.Center
                            }
                            .fillMaxHeight(0.75f) // Let cards take up a significant portion of height
                            .aspectRatio(0.70f) // Typical card aspect ratio
                            .clickable {
                                // When a card is clicked, scroll to it and mark it as selected
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                                // selectedTicketIndex = page // The LaunchedEffect for pagerState.currentPage handles this
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun TicketCardItem(
    ticket: Ticket,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    // Fetch event details for the ticket
    // This is a simplified way; in a real app, you might get event title/image from a ViewModel
    var eventTitle by remember { mutableStateOf("Loading Event...") }
    var eventImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ticket.eventId) {
        try {
            val eventDoc = FirebaseHelper.firestore.collection("events").document(ticket.eventId).get().await()
            eventTitle = eventDoc.getString("title") ?: "Event"
            eventImageUrl = eventDoc.getString("imageUrl")
        } catch (e: Exception) {
            eventTitle = "Event Info Unavailable"
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp), // More rounded corners
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 12.dp else 6.dp), // More pronounced elevation for selected
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize() // Ticket content fills the card
            // .background(Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)))) // Example Gradient
        ) {
            // Event Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f) // Image takes top 40%
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                AsyncImage(
                    model = eventImageUrl,
                    contentDescription = "Event Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay with Event Title
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = eventTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        maxLines = 2
                    )
                }
            }

            // Ticket Details Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ticket ID: ${ticket.ticketId.substring(0,8)}...", // Shortened ID
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // QR Code (only if selected, or always if design prefers)
                val qrCodeBitmap = remember(ticket.qrCodeData) {
                    QrCodeUtils.generateQrCodeBitmap(ticket.qrCodeData, size = 256) // Smaller QR for card
                }
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size(160.dp) // QR code size
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))

                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan at Venue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )


                Spacer(modifier = Modifier.weight(1f)) // Pushes date to bottom

                Text(
                    text = "Purchased: ${ticket.purchaseDate.split("T").firstOrNull() ?: ticket.purchaseDate}", // Show only date part
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Original TicketItem - can be removed or kept for reference
@Composable
fun TicketItem(ticket: Ticket, modifier: Modifier = Modifier, isSelected: Boolean) {
    Card(
        modifier = modifier.fillMaxSize(), // Changed from fillMaxWidth to fillMaxSize
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Ticket ID: ${ticket.ticketId}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Event ID: ${ticket.eventId}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "User ID: ${ticket.userId}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Purchase Date: ${ticket.purchaseDate}", style = MaterialTheme.typography.bodyMedium)

            if (isSelected) {
                val qrCodeBitmap = remember(ticket.qrCodeData) {
                    QrCodeUtils.generateQrCodeBitmap(ticket.qrCodeData)
                }
                Image(
                    bitmap = qrCodeBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(256.dp)
                )
            }
        }
    }
}