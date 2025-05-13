package com.partympakache.littlegig.domain.usecase

import com.partympakache.littlegig.data.repository.RecapRepository
import javax.inject.Inject

class LikeRecapUseCase @Inject constructor(private val recapRepository: RecapRepository) {
     suspend operator fun invoke(recapId: String, userId: String){
         recapRepository.likeRecap(recapId, userId)
     }
}