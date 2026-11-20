plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.niz.tvlauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.niz.tvlauncher"
        minSdk = 22          // Fire OS 7 ~ Android 9, but 22 keeps older Fire sticks covered too
        targetSdk = 30       // staying <=30 avoids the strict package-visibility prompts on Fire OS
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key so the built APK sideloads straight away.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // QR code for the phone remote
    implementation("com.google.zxing:core:3.5.3")
}
