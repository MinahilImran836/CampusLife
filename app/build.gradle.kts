plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.campuslife"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.campuslife"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // UI Components
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Room (The new additions)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.firebase.auth)
    ksp(libs.androidx.room.compiler)

    // Specialized Libraries for Campus Life
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.getkeepsafe.taptargetview:taptargetview:1.13.3")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:16.0.1")
    implementation("com.google.mediapipe:tasks-text:0.10.14")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.google.firebase:firebase-firestore:26.3.0")
    implementation("com.google.firebase:firebase-storage:22.0.1")

    // FirebaseUI architecture with explicit dependency exclusions
    implementation("com.firebaseui:firebase-ui-firestore:8.0.2") {
        exclude(group = "com.google.firebase", module = "firebase-common")
        exclude(group = "com.google.firebase", module = "firebase-firestore")
        exclude(group = "com.google.firebase", module = "firebase-auth")
        exclude(group = "com.google.firebase", module = "firebase-bom")
    }

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}