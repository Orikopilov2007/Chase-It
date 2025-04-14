import java.util.Properties
import java.io.FileInputStream

val localProps = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        FileInputStream(localFile).use { load(it) }
    }
}

// ✅ Use a unique name here
val openAiKeyFromLocal = localProps["OPENAI_API_KEY"] as? String
    ?: throw GradleException("OPENAI_API_KEY not found in local.properties")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mypoject1"
    compileSdk = 34

    defaultConfig {
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAiKeyFromLocal\"")
        applicationId = "com.example.mypoject1"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
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
}

dependencies {
    // Core dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.tools.core)

    // Firebase dependencies
    implementation("com.google.firebase:firebase-auth:22.1.1")
    implementation("com.google.firebase:firebase-firestore:24.7.1")
    implementation("com.google.firebase:firebase-storage:20.2.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:18.0.0")
    implementation("com.google.android.gms:play-services-fitness:21.0.1")
    implementation("com.google.android.gms:play-services-auth:20.2.0")
    implementation ("androidx.activity:activity:1.6.0")
    implementation ("androidx.activity:activity-ktx:1.6.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation ("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.squareup.moshi:moshi:1.13.0")
    implementation ("com.squareup.moshi:moshi-kotlin:1.13.0")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation ("androidx.appcompat:appcompat:1.4.2")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation ("androidx.navigation:navigation-ui-ktx:2.6.0")
    implementation("org.json:json:20210307")
    implementation ("com.github.yalantis:ucrop:2.2.8")
    implementation ("com.github.bumptech.glide:glide:4.13.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.13.0")
    implementation ("com.airbnb.android:lottie:3.7.0")
    implementation ("com.tom-roush:pdfbox-android:2.0.27.0")


    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

apply(plugin = "com.google.gms.google-services")
