package com.partympakache.littlegig.data.model

data class Recap(
    val recapId: String = "",
    val eventId: String = "",
    val userId: String = "",
    val mediaUrl: String = "",
    val caption: String? = null,
    val timestamp: String = "",
    val likes: List<String> = emptyList(),
    val views: Int = 0
)