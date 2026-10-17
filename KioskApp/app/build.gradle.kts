plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android — AGP 9 bundles Kotlin.
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.jiitkiosk.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jiitkiosk.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "0.2.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // shortcut: the debug key signs release builds so they stay installable over
            // the dev build; swap for a real keystore before any Play distribution
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += listOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json", "/META-INF/*.version")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")

    // Google sign-in runs in a WebView: needs the modern webkit (WebMessageListener,
    // document-start scripts) and GSI's own page.
    implementation("androidx.webkit:webkit:1.17.0")

    // Classroom connect rides Google Identity; the reminder sync rides WorkManager
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("androidx.work:work-runtime-ktx:2.10.2")

    // the vendored portal client (ktjiit) talks kotlinx-serialization + coroutines only
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    testImplementation("junit:junit:4.13.2")

    // the persistent-login tests need a real Keystore: they run on a device
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
