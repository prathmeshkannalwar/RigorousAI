// build.gradle.kts (Module :app) - VERIFIED VERSION

// Import the necessary Java classes at the top of the file
import java.util.Properties
import java.io.FileInputStream

// This logic for reading properties must be at the top level of the script.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.Rigorous_X"
    compileSdk = 35

    // This is the master switch that tells Gradle to generate BuildConfig.java.
    // Placing it here ensures it's always read.
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.Rigorous_X"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // This line adds your API key to the BuildConfig.java file.
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Gemini API
    implementation("com.google.ai.client.generativeai:generativeai:0.6.0") // <-- NEW, STABLE VERSION

    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")



    // Provides the main Markwon class and the MarkwonTheme class
    implementation("io.noties.markwon:core:4.6.2")

    // THIS IS THE LINE THAT FIXES YOUR ERROR
    implementation("io.noties.markwon:ext-tables:4.6.2")

    //Other useful plugins for a clean look
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")

}