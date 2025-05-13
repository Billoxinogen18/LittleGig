plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android") // Hilt
    id("kotlinx-serialization") //For Caching
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.partympakache.littlegig.com"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.partympakache.littlegig.com"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true // Enable Jetpack Compose
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Use a compatible Compose Compiler version
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.activity.compose.v182)
    implementation(libs.androidx.compose.bom.v20230800)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3) // Material 3
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.media3:media3-exoplayer:1.5.1") // Core ExoPlayer functionality
    implementation("androidx.media3:media3-ui:1.5.1")       // UI components (PlayerView)
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1") // For DASH support (if needed)
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1") // For HLS support (if needed)
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:1.5.1") // For SmoothStreaming (if needed)
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("com.google.android.libraries.places:places:3.3.0")
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // LiveData
    implementation(libs.androidx.runtime.livedata)

// Ensure these Supabase dependencies are present and correctly versioned
// Navigation and Room dependencies
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")

// Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:33.13.0")) // No change
    implementation("com.google.firebase:firebase-analytics") // No change

// Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0") // No change

// Supabase Integration - using the latest BOM version for Supabase libraries
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.3"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt") // No change
    implementation("io.github.jan-tennert.supabase:auth-kt") // No change

// Ktor Client - updated to the latest version compatible with Kotlin 2.0.0
    implementation("io.ktor:ktor-client-android:3.0.0")


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Serialization (required for Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
// Testing dependencies - no changes here.
    testImplementation(libs.junit) // No change
    androidTestImplementation(libs.androidx.junit) // No change

    // Firebase - We will remove these as we migrate features
    // For now, ensure they are not causing conflicts with Supabase Auth specifically.
    // //implementation(libs.firebase.bom) // Keep if other Firebase services are used
    // //implementation(libs.firebase.auth.ktx) // REMOVE THIS if LoginViewModel uses only Supabase
    // implementation("com.google.firebase:firebase-firestore-ktx") // Keep for now
    // //implementation(libs.firebase.storage.ktx) // Keep for now
    // implementation (libs.firebase.ui.auth) // REMOVE THIS (FirebaseUI for Auth)
    // //implementation(libs.firebase.auth) // REMOVE THIS (redundant with auth.ktx)





    // Firebase
    //implementation(libs.firebase.bom)
    //implementation(libs.firebase.auth.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx")  {
        exclude(group = "com.google.firebase", module = "firebase-common") // Add this line
    }
    //implementation(libs.firebase.storage.ktx)

    // Image Loading (Coil - Compose-friendly)
    implementation(libs.coil.compose)

    // Networking (Retrofit - Optional, but good practice)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Google Maps (Requires integration with AndroidView)
    implementation(libs.maps.compose) //maps
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // QR Code Generation (ZXing - May need a wrapper for Compose)
    implementation(libs.zxing.android.embedded)
    implementation (libs.core) //Core
    implementation (libs.calendar) //Calender
    implementation (libs.clock) //Clock
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    // Hilt

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose) // Hilt with Compose Navigation
    implementation(libs.accompanist.placeholder.material) //Add to dependencies
    // CameraX
    val cameraxVersion = "1.3.1"
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation (libs.firebase.ui.auth)
    implementation(libs.play.services.auth)
    //Shimmer
    implementation(libs.shimmer)

    //Room
    val room_version = "2.6.1"

    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)
    kapt("androidx.room:room-compiler:$room_version")
    // optional - Kotlin Extensions and Coroutines support for Room
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation ("com.googlecode.libphonenumber:libphonenumber:8.13.42")
    implementation(libs.guava)


    //Data Store
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    //implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.android.maps.utils)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}

kapt {
    correctErrorTypes = true // Important for Hilt with Compose
}