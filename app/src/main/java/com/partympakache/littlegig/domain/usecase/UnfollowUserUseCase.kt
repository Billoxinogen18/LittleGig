package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.repository.UserRepository
import javax.inject.Inject
class UnfollowUserUseCase @Inject constructor(private val userRepository: UserRepository) {
    suspend operator fun invoke(currentUserId: String, userIdToUnfollow: String) {
        userRepository.unfollowUser(currentUserId, userIdToUnfollow)
    }
}