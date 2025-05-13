package com.partympakache.littlegig.data.repository.impl

import android.util.Log
import com.partympakache.littlegig.data.model.Ticket
import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.data.repository.UserRepository
import io.github.jan_tennert.supabase.gotrue.GoTrue
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.postgrest.query.PostgrestBuilder
import io.github.jan_tennert.supabase.postgrest.result.PostgrestResult
import io.github.jan_tennert.supabase.storage.Storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val db: Postgrest, // Injected Supabase Postgrest
    private val storage: Storage, // Injected Supabase Storage for profile images etc.
    private val auth: GoTrue // Injected Supabase GoTrue for current user info
) : UserRepository {

    // Configure Kotlinx Serialization
    private val json = Json {
        ignoreUnknownKeys = true // Important for flexibility if Supabase returns extra fields
        isLenient = true
        encodeDefaults = true // Ensure default values are encoded if not present
    }

    override suspend fun getUser(userId: String): Flow<User?> = flow {
        try {
            Log.d("UserRepositoryImpl", "Fetching user from Supabase: $userId")
            val response: String = db.from("users") // "users" is your table name in Supabase
                .select {
                    filter {
                        eq("userId", userId) // Assuming your primary key or unique ID column is 'userId'
                    }
                }
                .single() // Expects a single row or throws an error if not found/multiple
                .data // Get the JSON string data

            val user = json.decodeFromString<User>(response)
            Log.d("UserRepositoryImpl", "User fetched successfully: ${user.displayName}")
            emit(user)
        } catch (e: Exception) {
            // Supabase single() throws an exception if no row is found or more than one.
            // We should treat "no row found" as emitting null.
            // Other exceptions should be logged.
            if (e.message?.contains("Received 0 rows") == true || e.message?.contains("PGRST116") == true) { // PGRST116: Row not found
                Log.w("UserRepositoryImpl", "User not found in Supabase: $userId")
                emit(null)
            } else {
                Log.e("UserRepositoryImpl", "Error fetching user $userId from Supabase: ${e.message}", e)
                emit(null) // Or throw a custom domain exception
            }
        }
    }

    override suspend fun getCurrentUser(): User? {
        val currentSupabaseUser = auth.currentUserOrNull() ?: return null
        // Fetch the user profile from your 'users' table using the Supabase auth ID
        var userProfile: User? = null
        getUser(currentSupabaseUser.id).collect { userProfile = it } // Collect the single emission
        return userProfile
    }


    override suspend fun createUser(user: User) {
        try {
            Log.d("UserRepositoryImpl", "Creating user in Supabase: ${user.userId}")
            db.from("users").insert(user) // Supabase handles serialization
            Log.d("UserRepositoryImpl", "User ${user.userId} created successfully in Supabase.")
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to create user ${user.userId} in Supabase: ${e.message}", e)
            // Handle or throw error
        }
    }

    override suspend fun updateUser(user: User) {
        try {
            Log.d("UserRepositoryImpl", "Updating user in Supabase: ${user.userId}")
            db.from("users")
                .update(
                    value = user, // Send the whole user object, Supabase will match on primary key
                    // Or specify fields: mapOf("displayName" to user.displayName, "bio" to user.bio)
                ) {
                    filter {
                        eq("userId", user.userId)
                    }
                }
            Log.d("UserRepositoryImpl", "User ${user.userId} updated successfully in Supabase.")
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to update user ${user.userId} in Supabase: ${e.message}", e)
            // Handle or throw error
        }
    }

    override suspend fun followUser(currentUserId: String, userIdToFollow: String) {
        try {
            // This logic needs to be adapted. Supabase doesn't have FieldValue.arrayUnion directly.
            // You'd typically fetch the current arrays, modify them, and then update.
            // Or, use a database function (RPC) for atomicity.

            // Simplified (less atomic, consider RPC for production):
            // 1. Get current user's following list
            val currentUserDataJson = db.from("users").select("following") { filter { eq("userId", currentUserId) } }.single().data
            val currentUserData = json.decodeFromString<Map<String, List<String>>>(currentUserDataJson)
            val currentFollowing = currentUserData["following"]?.toMutableList() ?: mutableListOf()

            // 2. Get target user's followers list
            val targetUserDataJson = db.from("users").select("followers") { filter { eq("userId", userIdToFollow) } }.single().data
            val targetUserData = json.decodeFromString<Map<String, List<String>>>(targetUserDataJson)
            val targetFollowers = targetUserData["followers"]?.toMutableList() ?: mutableListOf()

            if (!currentFollowing.contains(userIdToFollow)) {
                currentFollowing.add(userIdToFollow)
                db.from("users").update(mapOf("following" to currentFollowing)) { filter { eq("userId", currentUserId) } }
            }

            if (!targetFollowers.contains(currentUserId)) {
                targetFollowers.add(currentUserId)
                db.from("users").update(mapOf("followers" to targetFollowers)) { filter { eq("userId", userIdToFollow) } }
            }
            Log.d("UserRepositoryImpl", "$currentUserId followed $userIdToFollow")

        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to follow user: ${e.message}", e)
        }
    }

    override suspend fun unfollowUser(currentUserId: String, userIdToUnfollow: String) {
        try {
            // Similar to followUser, fetch, modify, update. Consider RPC.
            val currentUserDataJson = db.from("users").select("following") { filter { eq("userId", currentUserId) } }.single().data
            val currentUserData = json.decodeFromString<Map<String, List<String>>>(currentUserDataJson)
            val currentFollowing = currentUserData["following"]?.toMutableList() ?: mutableListOf()

            val targetUserDataJson = db.from("users").select("followers") { filter { eq("userId", userIdToUnfollow) } }.single().data
            val targetUserData = json.decodeFromString<Map<String, List<String>>>(targetUserDataJson)
            val targetFollowers = targetUserData["followers"]?.toMutableList() ?: mutableListOf()

            if (currentFollowing.contains(userIdToUnfollow)) {
                currentFollowing.remove(userIdToUnfollow)
                db.from("users").update(mapOf("following" to currentFollowing)) { filter { eq("userId", currentUserId) } }
            }

            if (targetFollowers.contains(currentUserId)) {
                targetFollowers.remove(currentUserId)
                db.from("users").update(mapOf("followers" to targetFollowers)) { filter { eq("userId", userIdToUnfollow) } }
            }
            Log.d("UserRepositoryImpl", "$currentUserId unfollowed $userIdToUnfollow")
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to unfollow user: ${e.message}", e)
        }
    }

    override suspend fun createUserIfNotExists(userId: String, phoneNumber: String) {
        try {
            Log.d("UserRepositoryImpl", "Checking if user exists in Supabase: $userId")
            // Attempt to fetch the user. If it fails because the user doesn't exist, then create.
            var userExists = false
            try {
                val existingUserJson = db.from("users")
                    .select("userId") // Select a minimal field to check existence
                    { filter { eq("userId", userId) } }
                    .singleOrNull() // Use singleOrNull to avoid exception if not found
                    ?.data // Get JSON string
                userExists = (existingUserJson != null && existingUserJson != "null")

            } catch(e: Exception) {
                // This catch might be hit if singleOrNull is not available or if there's another issue.
                // The primary check is if existingUserJson is null or "null".
                if (e.message?.contains("Received 0 rows", ignoreCase = true) == true || e.message?.contains("PGRST116") == true) {
                    userExists = false
                } else {
                    Log.w("UserRepositoryImpl", "Unexpected error checking user existence for $userId: ${e.message}", e)
                    // Decide if you want to proceed with creation or rethrow
                }
            }


            if (!userExists) {
                Log.d("UserRepositoryImpl", "User $userId does not exist. Creating new user in Supabase.")
                val newUser = User(
                    userId = userId, // This should be the Supabase Auth User ID
                    phoneNumber = phoneNumber,
                    displayName = "User ${userId.take(6)}", // Generate a default display name
                    rank = "Party Popper", // Default rank
                    visibilitySetting = "public", // Default visibility
                    isActiveNow = false,
                    // Initialize other default fields for your User model
                    followers = emptyList(),
                    following = emptyList(),
                    profileImageUrl = null, // Or a default placeholder image URL
                    bio = null,
                    lastActiveTimestamp = null
                )
                db.from("users").insert(newUser)
                Log.i("UserRepositoryImpl", "New user $userId created in Supabase with phone $phoneNumber.")
            } else {
                Log.d("UserRepositoryImpl", "User $userId already exists in Supabase.")
                // Optionally, update phone number if it's different and user exists
                // val existingUser = json.decodeFromString<User>(existingUserJson!!)
                // if (existingUser.phoneNumber != phoneNumber) {
                //     db.from("users").update(mapOf("phoneNumber" to phoneNumber)) { filter { eq("userId", userId) } }
                //     Log.i("UserRepositoryImpl", "Updated phone number for existing user $userId.")
                // }
            }
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Error in createUserIfNotExists (Supabase) for $userId: ${e.message}", e)
        }
    }

    override suspend fun createTicket(ticket: Ticket) {
        try {
            Log.d("UserRepositoryImpl", "Creating ticket in Supabase: ${ticket.ticketId}")
            db.from("tickets").insert(ticket) // "tickets" is your table name
            Log.d("UserRepositoryImpl", "Ticket ${ticket.ticketId} created successfully in Supabase.")
        } catch (e: Exception) {
            Log.e("UserRepositoryImpl", "Failed to create ticket ${ticket.ticketId} in Supabase: ${e.message}", e)
            throw e // Re-throw to be handled by the use case or ViewModel
        }
    }
}
