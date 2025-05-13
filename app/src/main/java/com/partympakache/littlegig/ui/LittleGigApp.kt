// app/src/main/java/com/partympakache/littlegig/ui/LittleGigApp.kt
package com.partympakache.littlegig.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.partympakache.littlegig.ui.screens.*
import com.partympakache.littlegig.ui.viewmodels.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LittleGigApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel() //Get Login View Model
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState() //Collect login state

    //Check the login state before navigating.
    val startDestination = if (isLoggedIn) "eventList" else "login"

    NavHost(navController = navController, startDestination = startDestination) { // Start with correct screen
        composable("eventList") {
            val viewModel: EventListViewModel = hiltViewModel() // Use hiltViewModel()
            EventListScreen(navController = navController, viewModel = viewModel)
        }
        composable("eventDetails/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: EventDetailsViewModel = hiltViewModel()
            EventDetailsScreen(navController = navController, viewModel = viewModel, eventId = eventId)
        }
        composable("map") {
            val viewModel: MapViewModel = hiltViewModel()
            MapScreen(navController = navController, viewModel = viewModel)
        }
        composable("tickets") {
            val viewModel: TicketViewModel = hiltViewModel()
            TicketScreen(viewModel = viewModel)
        }
        composable("account") {
            val viewModel: AccountViewModel = hiltViewModel()
            AccountScreen(navController = navController, viewModel = viewModel)
        }
        composable("chat") {
            val viewModel: ChatViewModel = hiltViewModel()
            ChatScreen(navController = navController, viewModel = viewModel)
        }
        composable("conversation/{receiverId}/{receiverName}") { backStackEntry ->
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""
            val viewModel: ConversationViewModel = hiltViewModel()
            ConversationScreen(navController = navController, viewModel = viewModel, receiverId = receiverId, receiverName = receiverName)
        }
        composable("login") {
            //Pass viewmodel and navController
            LoginScreen(navController = navController, viewModel = loginViewModel) // LoginScreen gets ViewModel now
        }
        composable("userProfile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val viewModel: UserProfileViewModel = hiltViewModel()
            UserProfileScreen(navController, userId = userId, viewModel = viewModel)
        }
        composable("editEvent/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val viewModel: EditEventViewModel = hiltViewModel()
            EditEventScreen(navController = navController, viewModel = viewModel, eventId = eventId)
        }

        // Add the CreateEventScreen route
        composable("createEvent") {
            val viewModel: CreateEventViewModel = hiltViewModel()
            CreateEventScreen(navController = navController, viewModel = viewModel)
        }
    }
}