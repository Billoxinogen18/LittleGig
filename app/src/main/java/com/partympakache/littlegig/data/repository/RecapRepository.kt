//data/repository/RecapRepository.kt
package com.partympakache.littlegig.data.repository

import android.net.Uri
import com.partympakache.littlegig.data.model.Recap
import kotlinx.coroutines.flow.Flow

interface RecapRepository {
    suspend fun postRecap(eventId: String, caption: String?, mediaUri: Uri)
    suspend fun getRecapsForEvent(eventId: String): Flow<List<Recap>>
    suspend fun likeRecap(recapId: String, userId: String)
    suspend fun unlikeRecap(recapId: String, userId: String)
    suspend fun incrementViewCount(recapId: String, userId: String)
    suspend fun hasUserViewedRecap(recapId: String, userId: String): Flow<Boolean>


}