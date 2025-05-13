package com.partympakache.littlegig.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.partympakache.littlegig.data.local.DataStoreManager
import com.partympakache.littlegig.data.repository.EventRepository
import com.partympakache.littlegig.data.repository.RecapRepository
import com.partympakache.littlegig.data.repository.UserRepository
import com.partympakache.littlegig.data.repository.impl.EventRepositoryImpl
import com.partympakache.littlegig.data.repository.impl.RecapRepositoryImpl
import com.partympakache.littlegig.data.repository.impl.UserRepositoryImpl
import com.partympakache.littlegig.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Use SingletonComponent for application-wide scope
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideEventRepository(firestore: FirebaseFirestore, storage: FirebaseStorage): EventRepository = EventRepositoryImpl(firestore, storage)

    @Provides
    @Singleton
    fun provideUserRepository(firestore: FirebaseFirestore, storage: FirebaseStorage, firebaseAuth: FirebaseAuth): UserRepository =
        UserRepositoryImpl(firestore, storage, firebaseAuth)

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager = DataStoreManager(context)

    @Provides
    @Singleton
    fun provideSaveLoginStateUseCase(dataStoreManager: DataStoreManager): SaveLoginStateUseCase {
        return SaveLoginStateUseCase(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideGetLoginStateUseCase(dataStoreManager: DataStoreManager): GetLoginStateUseCase {
        return GetLoginStateUseCase(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideCreateUserIfNotExistsUseCase(userRepository: UserRepository): CreateUserIfNotExistsUseCase {
        return CreateUserIfNotExistsUseCase(userRepository)
    }
    @Provides
    @Singleton
    fun providesRecapRepository(firestore: FirebaseFirestore, storage: FirebaseStorage, dataStoreManager: DataStoreManager) : RecapRepository = RecapRepositoryImpl(firestore, storage, dataStoreManager) //FIXED: Added dataStoreManager

    @Provides
    @Singleton
    fun provideGetRecapsForEventUseCase(recapRepository: RecapRepository): GetRecapsForEventUseCase {
        return GetRecapsForEventUseCase(recapRepository)
    }

    @Provides
    @Singleton
    fun providesLikeRecapUseCase(recapRepository: RecapRepository) : LikeRecapUseCase = LikeRecapUseCase(recapRepository)
    @Provides
    @Singleton
    fun providesUnLikeRecapUseCase(recapRepository: RecapRepository): UnLikeRecapUseCase = UnLikeRecapUseCase(recapRepository)
    @Provides
    @Singleton
    fun providesPostRecapUseCase(recapRepository: RecapRepository) : PostRecapUseCase = PostRecapUseCase(recapRepository)

    @Provides
    @Singleton
    fun providesViewRecapUseCase(recapRepository: RecapRepository) : ViewRecapUseCase = ViewRecapUseCase(recapRepository)
}