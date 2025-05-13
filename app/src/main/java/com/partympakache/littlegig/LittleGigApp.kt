package com.partympakache.littlegig

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.partympakache.littlegig.data.repository.RecapRepository
import com.partympakache.littlegig.data.repository.impl.RecapRepositoryImpl
import dagger.Provides
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Singleton

@HiltAndroidApp
class LittleGigApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Places SDK *once*, here.
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDck_XPAGWCHfJBP8-_7s28yKNK70zrWW0") // Replace with your API Key
        }
    }
}
