import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Đọc local.properties
val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) localProps.load(localFile.inputStream())

android {
    namespace = "com.example.hisync"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hisync"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject SERVER_URL vào BuildConfig
        buildConfigField(
            "String",
            "SERVER_URL",
            "\"${localProps.getProperty("server.url", "http://10.0.2.2:8080/api/")}\""
        )
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

    // Bật BuildConfig
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Glide (load thumbnail YouTube)
    implementation("com.github.bumptech.glide:glide:4.16.0")
// Cloudinary Android SDK
    implementation("com.cloudinary:cloudinary-android:3.0.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}