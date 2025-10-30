import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.deflate"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.deflate"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                storeFile = file("keystore/release-key.jks")
                storePassword = "Str0ngD3fl4t3Passw0rd"
                keyAlias = "release"
                keyPassword = "Str0ngD3fl4t3Passw0rd"
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.biometric.ktx)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Facebook Sign-In
    implementation("com.facebook.android:facebook-android-sdk:16.2.0")

    // GitHub Sign-In (using OAuth)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Biometrics
    implementation("androidx.biometric:biometric:1.2.0")
    implementation(libs.androidx.biometric.ktx.v140alpha02)
    implementation("androidx.security:security-crypto:1.1.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    //  MaterialCalendarView (correct fork)
    implementation("com.github.mhiew:material-calendarview:2.0.1")

    // ThreeTenABP for LocalDate and time API
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.4")

    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")

    // AndroidX Core and AppCompat
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Material Components
    implementation("com.google.android.material:material:1.11.0")
}
