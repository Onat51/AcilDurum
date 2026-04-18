plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.emergencyapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.emergencyapp"
        minSdk = 24
        targetSdk = 34
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core:1.9.0")
    implementation("androidx.activity:activity:1.7.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // WorkManager — servis watchdog + boot recovery için
    implementation("androidx.work:work-runtime:2.9.0")
    // Startup (WorkManager initialization için)
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Firebase (BOM ile sürüm yönetimi)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth")          // Email + Google Auth

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Konum
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.17")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
