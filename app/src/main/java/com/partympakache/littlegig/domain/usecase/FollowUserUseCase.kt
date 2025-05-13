package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.repository.UserRepository
import javax.inject.Inject

class FollowUserUseCase @Inject constructor(private val userRepository: UserRepository) {
    suspend operator fun invoke(currentUserId: String, userIdToFollow: String) {
        userRepository.followUser(currentUserId, userIdToFollow)
    }
}