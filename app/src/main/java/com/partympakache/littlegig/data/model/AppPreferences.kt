package com.partympakache.littlegig.data.model

data class AppPreferences(
    val viewedRecaps: Map<String, Boolean> = emptyMap()
)