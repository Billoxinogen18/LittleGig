package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.repository.UserRepository
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(private val userRepository: UserRepository) {
    suspend operator fun invoke() = userRepository.getCurrentUser()
}