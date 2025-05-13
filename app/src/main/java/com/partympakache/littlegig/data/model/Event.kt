package com.partympakache.littlegig.data.model

import com.google.firebase.firestore.GeoPoint
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val eventId: String = "",
    val title: String = "",
    val description: String = "",
    val date: String = "",
    val time: String = "",
    val locationName: String = "",
    @Contextual val locationCoordinates: GeoPoint = GeoPoint(0.0, 0.0),
    val price: Double = 0.0,
    val imageUrl: String = "",
    val organizerId: String = "",
    val availableTickets: Int = 0,
    val influencerIds: List<String> = emptyList(),
    val likedByUserIds: List<String> = emptyList(),
    val status: String = "upcoming", // "upcoming", "ongoing", "past"
    val managerUserIds: List<String> = emptyList(),
    val isPublicAttendance: Boolean = false, // Added for attendance list visibility
    val attendees: List<String> = emptyList()  // List of user IDs attending (if public)

)