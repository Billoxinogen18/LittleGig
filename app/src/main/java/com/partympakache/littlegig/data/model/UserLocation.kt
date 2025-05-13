package com.partympakache.littlegig.data.model

import com.google.firebase.firestore.GeoPoint

//Create data class for the user location, since we store locations
data class UserLocation(
    val location: GeoPoint = GeoPoint(0.0, 0.0) // Provide a default value
)