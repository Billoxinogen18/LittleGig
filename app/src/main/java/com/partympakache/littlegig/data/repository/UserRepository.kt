package com.partympakache.littlegig.data.repository

import com.partympakache.littlegig.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUser(userId: String): Flow<User?>
    suspend fun createUser(user: User)
    suspend fun updateUser(user: User)
    suspend fun followUser(currentUserId: String, userIdToFollow: String)
    suspend fun unfollowUser(currentUserId: String, userIdToUnfollow: String)
    suspend fun getCurrentUser(): User?
    suspend fun createUserIfNotExists(userId: String, phoneNumber: String) // This line was missing
    suspend fun createTicket(ticket: com.partympakache.littlegig.data.model.Ticket)
}