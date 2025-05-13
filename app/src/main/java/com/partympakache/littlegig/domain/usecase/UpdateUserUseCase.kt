package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.model.User
import com.partympakache.littlegig.data.repository.UserRepository
import javax.inject.Inject

class UpdateUserUseCase @Inject constructor(
    private val userRepository: UserRepository
){
    suspend operator fun invoke(user: User){
        return userRepository.updateUser(user)
    }
}