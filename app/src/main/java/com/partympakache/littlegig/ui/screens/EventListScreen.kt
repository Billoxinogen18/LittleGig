package com.partympakache.littlegig.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.partympakache.littlegig.data.model.Event
import com.partympakache.littlegig.ui.components.EmptyState
import com.partympakache.littlegig.ui.components.ShimmerListItem
import com.partympakache.littlegig.ui.viewmodels.EventListViewModel
import com.partympakache.littlegig.utils.FirebaseHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventListScreen(navController: NavController, viewModel: EventListViewModel) {
    val events by viewModel.events.collectAsState()
    val currentUser = FirebaseHelper.getCurrentUserId()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                actions = {
                    IconButton(onClick = { navController.navigate("createEvent") }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create Event")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchEvents(it)
                },
                label = { Text("Search events") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            viewModel.searchEvents("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchEvents(searchQuery)
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 12.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // Shimmer while loading
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLoading,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(3) {
                            ShimmerFullScreenEventItem()
                        }
                    }
                }

                // Actual content when not loading
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    if (events.isEmpty()) {
                        EmptyState(
                            message = if (searchQuery.isNotEmpty())
                                "No events found for \"$searchQuery\""
                            else "No events available"
                        )
                    } else {
                        val pagerState = rememberPagerState(pageCount = { events.size })
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                        ) { pageIndex ->
                            if (pageIndex < events.size) {
                                val event = events[pageIndex]
                                FullScreenEventItem(
                                    event = event,
                                    currentUserId = currentUser,
                                    onItemClick = {
                                        navController.navigate("eventDetails/${event.eventId}")
                                    },
                                    onLikeClick = { viewModel.toggleLike(event) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenEventItem(
    event: Event,
    currentUserId: String?,
    onItemClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onItemClick),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = "Event Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                    maxLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Date: ${event.date} at ${event.time}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Venue: ${event.locationName}",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Price: KSH ${event.price}",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                        )
                    }
                    val isLiked = remember(event.likedByUserIds, currentUserId) {
                        event.likedByUserIds.contains(currentUserId)
                    }
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) Color.Red else Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                if (event.status == "past") {
                    Text(
                        text = "This event has passed",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ShimmerFullScreenEventItem() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .placeholder(
                visible = true,
                color = MaterialTheme.colorScheme.surfaceVariant,
                highlight = PlaceholderHighlight.shimmer(
                    highlightColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun EventItem(
    event: Event,
    currentUserId: String?,
    onItemClick: () -> Unit,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = "Event Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(text = event.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(text = "${event.date} ${event.time}", style = MaterialTheme.typography.bodyMedium)
                Text(text = event.locationName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "KSH ${event.price}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                val isLiked = remember(event.likedByUserIds, currentUserId) {
                    event.likedByUserIds.contains(currentUserId)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike" else "Like",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${event.likedByUserIds.size} ${if (event.likedByUserIds.size == 1) "like" else "likes"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (event.status == "past") {
                    Text(
                        text = "Event has passed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShimmerLoadingList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(all = 16.dp)
    ) {
        items(5) {
            ShimmerListItem()
        }
    }
}
