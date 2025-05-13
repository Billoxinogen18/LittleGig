// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id ("androidx.navigation.safeargs") version "2.8.7" apply false
    alias(libs.plugins.google.gms.google.services) apply false

}