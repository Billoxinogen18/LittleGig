package com.partympakache.littlegig.data.model

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val eventId: String? = null,
    val messageText: String = "",
    val timestamp: String = "",
    val isActive: Boolean? = null // Use User's isActiveNow
)