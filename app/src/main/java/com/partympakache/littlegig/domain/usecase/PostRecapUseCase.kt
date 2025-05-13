package com.partympakache.littlegig.domain.usecase

import android.net.Uri
import com.partympakache.littlegig.data.repository.RecapRepository
import javax.inject.Inject

class PostRecapUseCase  @Inject constructor(private val recapRepository: RecapRepository){
    suspend operator fun invoke(eventId: String, caption: String?, mediaUri: Uri) {
        recapRepository.postRecap(eventId, caption, mediaUri)
    }
}