package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.repository.UserRepository
import javax.inject.Inject

class CreateUserIfNotExistsUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, phoneNumber: String) {
        userRepository.createUserIfNotExists(userId, phoneNumber)
    }
}