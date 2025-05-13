package com.partympakache.littlegig.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object EventList : Screen("eventList", "Events", Icons.Filled.List)
    object Map : Screen("map", "Map", Icons.Filled.Place)
    object Tickets : Screen("tickets", "Tickets", Icons.Filled.ConfirmationNumber)
    object Account : Screen("account", "Account", Icons.Filled.Person)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    //Don't add a route for create here.
}