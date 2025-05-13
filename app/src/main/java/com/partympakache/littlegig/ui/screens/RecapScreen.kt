package com.partympakache.littlegig.ui.screens

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.partympakache.littlegig.data.model.Recap
import com.partympakache.littlegig.ui.viewmodels.RecapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.core.graphics.drawable.toBitmap


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapScreen(
    navController: NavController,
    userId: String,
    eventId: String,
    viewModel: RecapViewModel = hiltViewModel()
) {
    val recaps by viewModel.recaps.collectAsState()
    val currentRecapIndex by viewModel.currentRecapIndex.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allRecaps by viewModel.allRecaps.collectAsState()  // Get all recaps

    // Fetch recaps when userId or eventId changes.
    LaunchedEffect(userId, eventId) {
        viewModel.fetchRecaps(eventId, userId)
        viewModel.fetchAllRecapsForEvent(eventId) // Fetch all recaps, needed for user switching
    }

    // Group recaps by user
    val groupedRecaps = remember(allRecaps) {
        allRecaps.groupBy { it.userId }
    }

    // Find the index of the current user's recaps within the grouped recaps
    val currentUserRecapGroupIndex = remember(groupedRecaps, userId) {
        groupedRecaps.keys.toList().indexOf(userId)
    }

    // Track the current group index
    var currentGroupIndex by remember { mutableStateOf(currentUserRecapGroupIndex) }

    // Keep track if we're at the start/end. These are now correct.
    val isAtStart = remember(currentGroupIndex, currentRecapIndex) {
        currentGroupIndex == 0 && currentRecapIndex == 0
    }
    val isAtEnd = remember(groupedRecaps, currentGroupIndex, currentRecapIndex) {
        currentGroupIndex == groupedRecaps.size - 1 &&
                currentRecapIndex == (groupedRecaps.values.elementAtOrNull(currentGroupIndex)?.size ?: 0) - 1
    }

    // Get the *current user's* recap list based on the group index.
    val currentUserRecaps = remember(groupedRecaps, currentGroupIndex) {
        groupedRecaps.values.elementAtOrNull(currentGroupIndex) ?: emptyList()
    }

    // Determine whether to use all recaps or just current user's recaps for progress bar
    val displayRecaps = if (groupedRecaps.size > 1) currentUserRecaps else recaps

    // Animation for progress bar
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing), label = ""
    )

    // Reset progress when recap changes or like status changes.  Crucially, this now also
    // considers the *group* index, not just the individual recap index within a user's list.
    LaunchedEffect(currentGroupIndex, currentRecapIndex, displayRecaps) {
        if (displayRecaps.isNotEmpty()) {  // Prevent IndexOutOfBounds
            progress = 0f // Reset to 0
            delay(50)  //Slight delay
            progress = 1f // Start the animation

            val currentRecap = displayRecaps.getOrNull(currentRecapIndex)
            currentRecap?.let {
                viewModel.viewRecap(it.recapId)
            }
        }
    }

    //Auto play
    LaunchedEffect(animatedProgress, displayRecaps, isAtEnd) {
        if (animatedProgress >= 1f && displayRecaps.isNotEmpty()) {
            // Check if at last recap
            if (currentRecapIndex < displayRecaps.size - 1) {
                viewModel.nextRecap() // Next recap
            } else if (currentGroupIndex < groupedRecaps.size - 1) {
                // Move to the next group
                currentGroupIndex++
                viewModel.resetRecapIndex()  // Reset index to 0
            } else if (isAtEnd) {
                navController.popBackStack() // Exit when done
            }
        }
    }


    // Swipe Handling
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeight = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() } // For swipe down

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Transition for the swipe animation
    val transition = updateTransition(targetState = Pair(offsetX, offsetY), label = "OffsetTransition")
    val animatedOffsetX by transition.animateFloat(
        transitionSpec = { spring() }, label = "OffsetX"
    ) { it.first }
    val animatedOffsetY by transition.animateFloat(
        transitionSpec = { spring() }, label = "OffsetY"
    ) { it.second }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .offset(x = with(LocalDensity.current) { animatedOffsetX.toDp() }, y = with(LocalDensity.current) { animatedOffsetY.toDp() }) // Offset for swipe

            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        offsetY += dragAmount
                    },
                    onDragEnd = {
                        if (offsetY > screenHeight / 3) {
                            navController.popBackStack() //Exit

                        }
                        offsetY = 0f // Reset
                    }
                )
            }
            .pointerInput(Unit) { //Horizontal
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    },
                    onDragEnd = {
                        if (offsetX > screenWidth / 3 && !isAtStart) {
                            // Swiping right (previous)
                            if (currentRecapIndex > 0) {
                                viewModel.previousRecap()
                            } else if (currentGroupIndex > 0) {
                                currentGroupIndex--
                                // Go to the last recap of the previous group.
                                viewModel.setRecapIndex(
                                    (groupedRecaps.values.elementAtOrNull(currentGroupIndex)?.size ?: 1) - 1
                                )
                            }
                        } else if (offsetX < -screenWidth / 3 ) {
                            // Swiping left (next)
                            if(isAtEnd){
                                navController.popBackStack()
                            }
                            else if (currentRecapIndex < currentUserRecaps.size - 1) {
                                viewModel.nextRecap()
                            } else if (currentGroupIndex < groupedRecaps.keys.size - 1) {
                                //Move to next group
                                currentGroupIndex++
                                viewModel.resetRecapIndex() //Start at beginning of new group
                            }

                        }
                        // Reset offset after swipe
                        offsetX = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (tapOffset.x < size.width / 3) {
                            if (currentRecapIndex > 0) {
                                viewModel.previousRecap()
                            } else if (currentGroupIndex > 0) {
                                currentGroupIndex--
                                viewModel.setRecapIndex((groupedRecaps.values.elementAtOrNull(currentGroupIndex)?.size ?: 1) -1)
                            }
                        } else if (tapOffset.x > 2 * size.width / 3) {
                            if (currentRecapIndex < currentUserRecaps.size - 1) {
                                viewModel.nextRecap()
                            } else if (currentGroupIndex < groupedRecaps.keys.size - 1) {
                                // Move to the next group
                                currentGroupIndex++
                                viewModel.resetRecapIndex()
                            }
                        }
                    }
                )
            }

        ,
        contentAlignment = Alignment.Center
    ) {
        if (displayRecaps.isNotEmpty() && currentRecapIndex in displayRecaps.indices) {
            // Display Recaps (with transition)
            AnimatedContent(
                targetState = currentRecapIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                }, label = ""

            ) { targetRecapIndex ->
                // Make sure you use targetRecapIndex
                val currentRecap = displayRecaps.getOrNull(targetRecapIndex)
                if (currentRecap != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    )
                    {
                        // Blurred Background (only for images for now)
                        if (!currentRecap.mediaUrl.endsWith(".mp4", ignoreCase = true)) {
                            BlurredImageBackground(
                                imageUrl = currentRecap.mediaUrl,
                                isVideo = false
                            )
                        }

                        // Progress Bars
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .align(Alignment.TopCenter),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            displayRecaps.forEachIndexed { index, _ ->
                                LinearProgressIndicator(
                                    progress = if (index < targetRecapIndex) {
                                        1f // Already viewed
                                    } else if (index == targetRecapIndex) {
                                        animatedProgress // Current one, animate
                                    } else {
                                        0f // Not viewed yet
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp),
                                    color = Color.White,
                                    trackColor = Color.Gray
                                )
                            }
                        }

                        // Display the recap image or video.
                        RecapContent(recap = currentRecap)

                        // Like Button (Bottom Right) and views (Bottom Left)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, // Use SpaceBetween
                        ) {
                            // Views (Left)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp) // Smaller icon

                                )
                                Text(text = "${currentRecap.views}", color = Color.White)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally){
                                IconButton(onClick = {
                                    if (currentRecap.likes.contains(currentUser?.userId)) {
                                        viewModel.unlikeRecap(currentRecap.recapId)
                                    } else {
                                        viewModel.likeRecap(currentRecap.recapId)
                                    }

                                }) {
                                    val likeIconTint =
                                        if (currentRecap.likes.contains(currentUser?.userId)) Color.Red else Color.White
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Like",
                                        tint = likeIconTint,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                                Text(text =  "${currentRecap.likes.size}", color = Color.White)
                            }
                        }
                        // User Profile (Top Left) - Clickable and close
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    navController.navigate("userProfile/${currentRecap.userId}")
                                }
                            ) {
                                AsyncImage(
                                    model = currentUser?.profileImageUrl,
                                    contentDescription = "User Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentUser?.displayName ?: "Unknown User",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            IconButton(
                                onClick = { navController.popBackStack() },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

            }
        } else {
            // Handle loading/empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black), //Consistent
                contentAlignment = Alignment.Center
            ){
                CircularProgressIndicator(color = Color.White)
            }
        }

        // "Card" swipe effect visuals (outside the AnimatedContent)
        if (offsetX > 0 && !isAtStart) {  // Show previous recap peek
            val prevGroupIndex = if (currentRecapIndex > 0) currentGroupIndex else currentGroupIndex - 1
            val prevRecapIndex = if (currentRecapIndex > 0) currentRecapIndex - 1 else (groupedRecaps.values.elementAtOrNull(prevGroupIndex)?.size ?: 1) -1
            val previousRecap = groupedRecaps.values.elementAtOrNull(prevGroupIndex)?.getOrNull(prevRecapIndex)

            previousRecap?.let {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .offset(x = with(LocalDensity.current) { (offsetX - screenWidth).toDp() })
                ) {
                    RecapContent(recap = it)
                }
            }
        } else if (offsetX < 0 && !isAtEnd) {  // Show next recap peek
            val nextGroupIndex = if (currentRecapIndex < displayRecaps.size - 1) currentGroupIndex else currentGroupIndex + 1
            val nextRecapIndex = if(currentRecapIndex < displayRecaps.size -1) currentRecapIndex + 1 else 0
            val nextRecap = groupedRecaps.values.elementAtOrNull(nextGroupIndex)?.getOrNull(nextRecapIndex)

            nextRecap?.let {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .offset(x = with(LocalDensity.current) { (screenWidth + offsetX).toDp() })
                ) {
                    RecapContent(recap = it)
                }
            }
        }
    }
}











@Composable
fun BlurredImageBackground(imageUrl: String, isVideo: Boolean) {
    val context = LocalContext.current
    var blurredBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Use rememberAsyncImagePainter for images, but handle video differently
    val painter = if (!isVideo) {
        rememberAsyncImagePainter(
            ImageRequest.Builder(context)
                .data(imageUrl)
                .size(Size.ORIGINAL)
                .build()
        )
    } else {
        null // We don't need a painter for videos in this case
    }

    LaunchedEffect(if (!isVideo) painter?.state else imageUrl) { // Key change: depend on imageUrl for videos
        withContext(Dispatchers.IO) { // Perform blurring off the main thread
            val originalBitmap: Bitmap? = if (isVideo) {
                // Get video thumbnail
                getVideoThumbnail(context, Uri.parse(imageUrl))
            } else {
                // Load image bitmap (only if it's not a video)
                if (painter?.state is AsyncImagePainter.State.Success) {
                    (painter.state as AsyncImagePainter.State.Success).result.drawable.toBitmap()
                } else {
                    null // Handle loading/error states if needed
                }
            }

            originalBitmap?.let {
                // Use the modern blur approach
                val blurred = applyModernBlur(it)
                blurredBitmap = blurred.asImageBitmap()
            }
        }
    }

    blurredBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "Blurred Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Or FillBounds
        )
    }
}

// Get Video thumbnail
fun getVideoThumbnail(context: android.content.Context, uri: Uri): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.getFrameAtTime(0) // Get frame at 0ms (first frame)
    } catch (e: Exception) {
        // Handle exceptions (e.g., file not found, invalid video format)
        e.printStackTrace()
        null
    } finally {
        try {
            retriever.release()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

// Modern blur implementation that doesn't use the deprecated RenderScript
fun applyModernBlur(originalBitmap: Bitmap, radius: Float = 25f): Bitmap {
    // Create a new bitmap with the same dimensions
    val width = originalBitmap.width
    val height = originalBitmap.height

    // Scale down for better performance (optional)
    val scaleFactor = 0.2f
    val scaledWidth = (width * scaleFactor).toInt()
    val scaledHeight = (height * scaleFactor).toInt()

    // Create a scaled bitmap first (reduces processing time)
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

    // Create output bitmap (transparent to start)
    val outputBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

    // Apply blur using Canvas and Paint
    val canvas = Canvas(outputBitmap)
    val paint = Paint().apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(radius, BlurMaskFilter.Blur.NORMAL)
        alpha = 200 // Some transparency for better blur effect
    }

    // Draw the scaled bitmap onto the output with the blur paint
    canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

    // Optional: Add a semi-transparent overlay for more blurred look
    val overlayPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#55000000") // Semi-transparent black
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, scaledWidth.toFloat(), scaledHeight.toFloat(), overlayPaint)

    // Scale back up to original size
    return Bitmap.createScaledBitmap(outputBitmap, width, height, true)
}

// Alternative blur implementation (stack blur algorithm - more efficient)
fun stackBlur(sentBitmap: Bitmap, radius: Int = 25): Bitmap {
    if (radius < 1) return sentBitmap

    val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val div = radius + radius + 1
    val r = IntArray(w * h)
    val g = IntArray(w * h)
    val b = IntArray(w * h)

    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(Math.max(w, h))

    var divsum = (div + 1) shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    for (i in 0 until 256 * divsum) {
        dv[i] = i / divsum
    }

    yi = 0
    yw = yi

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    for (y in 0 until h) {
        rinsum = 0
        ginsum = 0
        binsum = 0
        routsum = 0
        goutsum = 0
        boutsum = 0
        rsum = 0
        gsum = 0
        bsum = 0
        for (i in -radius..radius) {
            p = pixels[yi + Math.min(wm, Math.max(i, 0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - Math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
        }
        stackpointer = radius

        for (x in 0 until w) {
            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) {
                vmin[x] = Math.min(x + radius + 1, wm)
            }
            p = pixels[yw + vmin[x]]

            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]

            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]

            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]

            yi++
        }
        yw += w
    }

    for (x in 0 until w) {
        rinsum = 0
        ginsum = 0
        binsum = 0
        routsum = 0
        goutsum = 0
        boutsum = 0
        rsum = 0
        gsum = 0
        bsum = 0
        yp = -radius * w
        for (i in -radius..radius) {
            yi = Math.max(0, yp) + x
            sir = stack[i + radius]
            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]
            rbs = r1 - Math.abs(i)
            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            if (i < hm) {
                yp += w
            }
        }
        yi = x
        stackpointer = radius
        for (y in 0 until h) {
            pixels[yi] = -0x1000000 or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum
            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]
            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]
            if (x == 0) {
                vmin[y] = Math.min(y + r1, hm) * w
            }
            p = x + vmin[y]
            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]
            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]
            rsum += rinsum
            gsum += ginsum
            bsum += binsum
            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]
            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]
            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]
            yi += w
        }
    }

    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    return bitmap
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun RecapContent(recap: Recap) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    val isVideo = recap.mediaUrl.endsWith(".mp4", ignoreCase = true) // Simple check

    // Use a DisposableEffect to create/release the player when the recap changes
    DisposableEffect(recap) {
        if (isVideo) {
            player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(Uri.parse(recap.mediaUrl))
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true // Autoplay
                repeatMode = Player.REPEAT_MODE_ONE // Loop for now
            }
        } else {
            player?.release()
            player = null
        }

        onDispose {
            player?.release()
            player = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        useController = false
                        resizeMode =  androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(recap.mediaUrl)
                    .crossfade(true)
                    .size(Size.ORIGINAL)
                    .build(),
                contentDescription = "Recap Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}