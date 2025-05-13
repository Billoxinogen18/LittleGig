package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke(userId: String): Flow<User?> { // Return a Flow<User?>
        return userRepository.getUser(userId)
    }
}