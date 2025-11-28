plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    /*id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.gms.google-services") version "4.4.4" apply false*/
}

android {
    namespace = "com.clublink.club_link"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.clublink.club_link"
        minSdk = 36
        targetSdk = 36
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Keep your existing non-Firebase dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)

    // Import the Firebase BoM
    // This will manage the versions of all Firebase libraries
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Add Firebase dependencies without specifying their versions
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    // Other libraries
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.airbnb.android:lottie:5.0.3")
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Test implementations
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
