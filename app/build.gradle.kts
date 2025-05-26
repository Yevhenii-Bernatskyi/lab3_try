plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.lab3_try"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lab3_try"
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
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- ДОДАНІ ЗАЛЕЖНОСТІ ---
    // Location
    implementation(libs.play.services.location)

    // Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler) // Важливо: використовуй kapt для компілятора
    implementation(libs.room.ktx)

    // Lifecycle (додаткові компоненти)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.service)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // OSMDroid (якщо вибираєш OSM)
    implementation(libs.osmdroid.android)


    implementation(libs.icons.core)
    implementation(libs.icons.extended)

    implementation(libs.compose.runtime.livedata)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.X.X")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.X.X")
}