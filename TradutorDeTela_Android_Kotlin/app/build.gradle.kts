plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.exemplo.tradutortela"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.exemplo.tradutortela"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 1. Google ML Kit - Reconhecimento Óptico de Caracteres (OCR Local e Gratuito)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // 2. Google ML Kit - Tradução Offline de Idiomas (Totalmente Gratuito sem chaves de API)
    implementation("com.google.mlkit:translate:17.0.3")

    // 3. AndroidX e Material Design UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // 4. Coroutines para processamento suave em segundo plano
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
