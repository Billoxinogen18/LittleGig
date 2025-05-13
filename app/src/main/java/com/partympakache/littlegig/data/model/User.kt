package com.partympakache.littlegig.data.model

data class User(
    val userId: String = "",
    val phoneNumber: String = "",
    val displayName: String? = null,
    val profileImageUrl: String? = null, // Corrected: Use profileImageUrl
    val followers: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val rank: String = "Party Popper", // Default rank
    val isActiveNow: Boolean = false,
    val lastActiveTimestamp: String? = null,
    val visibilitySetting: String = "public", // or "private"
    val bio: String? = null
)