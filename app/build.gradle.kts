import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

android {
    namespace = "neuracircuit.dev.game2048"
    compileSdk {
        version = release(36)
    }
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "neuracircuit.dev.game2048"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Define Build Config Fields from local.properties
        // If property is missing, fallback to Test IDs
        val admobAppId = localProperties.getProperty("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
        val admobBannerId = localProperties.getProperty("ADMOB_BANNER_ID") ?: "ca-app-pub-3940256099942544/6300978111"
        val admobInterstitialId = localProperties.getProperty("ADMOB_INTERSTITIAL_ID") ?: "ca-app-pub-3940256099942544/1033173712"
        val admobRewardedId = localProperties.getProperty("ADMOB_REWARDED_ID") ?: "ca-app-pub-3940256099942544/5224354917"
        val unityGameId = localProperties.getProperty("UNITY_GAME_ID") ?: "6028752"

        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
        buildConfigField("String", "UNITY_GAME_ID", "\"$unityGameId\"")

        // Inject App ID into Manifest
        manifestPlaceholders["ADMOB_APP_ID"] = admobAppId

        // Enable Native Debug Symbols for Play Store
        ndk {
            // "armeabi-v7a" = Old Phones (32-bit)
            // "arm64-v8a"   = New Phones (64-bit)
            // "x86_64"      = Chromebooks / PC Emulators
//            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
            debugSymbolLevel = "FULL"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.splashScreen)
    implementation(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    //noinspection UseTomlInstead
    // implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
