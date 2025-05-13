package com.partympakache.littlegig

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.partympakache.littlegig.ui.navigation.Screen
import com.partympakache.littlegig.ui.screens.*
import com.partympakache.littlegig.ui.theme.LittleGigTheme
import com.partympakache.littlegig.ui.viewmodels.*
// import com.partympakache.littlegig.utils.FirebaseHelper // Remove if not used elsewhere
import com.partympakache.littlegig.utils.SupabaseManager // For checking current user session directly if needed
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O) // For CreateEventScreen and other date/time APIs
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LittleGigTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainAppNavigation() // Changed to a new composable for clarity
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // Determine the start destination based on login state
    // This LaunchedEffect will re-evaluate start destination if isLoggedIn changes
    // while the NavHost is not yet composed with the new start destination.
    val startDestination = remember(isLoggedIn) {
        if (isLoggedIn) Screen.EventList.route else "login"
    }
    // Keep track of whether the NavHost has been composed with the correct start destination
    var navHostReady by remember { mutableStateOf(false) }
    LaunchedEffect(startDestination) {
        navHostReady = true
    }


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Screens that should show the bottom navigation bar
    val bottomBarScreens = listOf(
        Screen.EventList.route,
        Screen.Chat.route,
        Screen.Map.route,
        Screen.Tickets.route,
        Screen.Account.route
    )
    val shouldShowBottomBar = currentDestination?.route in bottomBarScreens && isLoggedIn

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) { // Conditionally show BottomNavigationBar
                NavigationBar {
                    val items = listOf(
                        Screen.EventList,
                        Screen.Chat,
                        Screen.Map,
                        Screen.Tickets,
                        Screen.Account,
                    )
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (navHostReady) { // Only compose NavHost once startDestination is determined
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("login") {
                    LoginScreen(navController = navController, viewModel = loginViewModel)
                }
                composable(Screen.EventList.route) {
                    val viewModel: EventListViewModel = hiltViewModel()
                    EventListScreen(navController = navController, viewModel = viewModel)
                }
                composable("eventDetails/{eventId}") { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    val viewModel: EventDetailsViewModel = hiltViewModel()
                    EventDetailsScreen(navController = navController, viewModel = viewModel, eventId = eventId)
                }
                composable(Screen.Map.route) {
                    val viewModel: MapViewModel = hiltViewModel()
                    MapScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Tickets.route) {
                    val viewModel: TicketViewModel = hiltViewModel()
                    TicketScreen(viewModel = viewModel)
                }
                composable(Screen.Account.route) {
                    val viewModel: AccountViewModel = hiltViewModel()
                    AccountScreen(navController = navController, viewModel = viewModel)
                }
                composable(Screen.Chat.route) {
                    val viewModel: ChatViewModel = hiltViewModel()
                    ChatScreen(navController = navController, viewModel = viewModel)
                }
                composable("conversation/{receiverId}/{receiverName}") { backStackEntry ->
                    val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
                    val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""
                    val viewModel: ConversationViewModel = hiltViewModel()
                    ConversationScreen(navController = navController, viewModel = viewModel, receiverId = receiverId, receiverName = receiverName)
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
                composable("createEvent") {
                    val viewModel: CreateEventViewModel = hiltViewModel()
                    CreateEventScreen(navController = navController, viewModel = viewModel)
                }
                composable("recapScreen/{userId}/{eventId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
                    val viewModel: RecapViewModel = hiltViewModel()
                    RecapScreen(navController = navController, userId = userId, eventId = eventId, viewModel = viewModel)
                }
            }
        } else {
            // Optional: Show a global loading indicator while NavHost is not ready
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
