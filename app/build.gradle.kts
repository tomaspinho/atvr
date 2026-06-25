plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "com.tomaspinho.atvr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tomaspinho.atvr"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

chaquopy {
    defaultConfig {
        // pyatv 0.13.0 is the last release without a pydantic dependency.
        // pyatv 0.14.0+ pulls in pydantic -> pydantic-core, which has no
        // Android wheel on Chaquopy's repository and cannot be built from
        // source on Android (requires Rust/maturin). 0.13.0's
        // cryptography>=36.0.2 is satisfied by cryptography 42.0.8 (the
        // newest version Chaquopy ships Android wheels for).
        version = "3.12"
        buildPython("/home/ubuntu/.local/share/uv/python/cpython-3.12.13-linux-x86_64-gnu/bin/python3.12")
        pip {
            install("pyatv==0.13.0")
            install("cryptography==42.0.8")
        }
    }
    sourceSets {
        getByName("main") {
            srcDir("src/main/python")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}