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
        // Python 3.12 (broadest Android wheel coverage on Chaquopy).
        //
        // pyatv 0.16.1 is the latest release that still allows pydantic 1.x
        // (>=1.10.10), which is pure Python with no pydantic-core dependency.
        // pyatv 0.17.0+ requires pydantic>=2.0.0 → pydantic-core (Rust native
        // extension, no Android wheel on Chaquopy).
        //
        // pyatv 0.16.1 declares cryptography>=44.0.1 and chacha20poly1305-
        // reuseable>=0.13.2 (→ cryptography>=43.0.0), but the actual APIs
        // used (ed25519, x25519, HKDF, ChaCha20Poly1305, serialization) are
        // stable since cryptography 2.x, so 42.0.8 (the newest Chaquopy
        // ships) works at runtime. We use --no-deps globally and list every
        // package in the full dependency tree in requirements.txt so pip's
        // resolver doesn't reject the "incompatible" version pins.
        //
        // No local/cross-compiled wheels needed — everything resolves from
        // Chaquopy's Android wheel repo (native) or PyPI (pure-Python).
        version = "3.12"
        buildPython("/home/tomas/.local/share/mise/installs/python/3.12.13/bin/python")
        pip {
            options("--no-deps")
            install("pyatv==0.16.1")
            install("aiohttp")
            install("chaquopy-libffi")
            install("cffi")
            install("cryptography")
            install("miniaudio")
            install("-r", "requirements.txt")
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
