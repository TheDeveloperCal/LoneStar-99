// Copyright (C) 2026 DeveloperCal
// SPDX-License-Identifier: GPL-3.0-only

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.developercal.ti99pocket"
    compileSdk = 36
    ndkVersion = "27.1.12297006"

    defaultConfig {
        applicationId = "com.developercal.ti99pocket"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += "arm64-v8a" }
    }

    val releaseStore = providers.gradleProperty("LS99_RELEASE_STORE_FILE").orNull
    val releaseStorePassword = providers.gradleProperty("LS99_RELEASE_STORE_PASSWORD").orNull
    val releaseAlias = providers.gradleProperty("LS99_RELEASE_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.gradleProperty("LS99_RELEASE_KEY_PASSWORD").orNull
    val hasReleaseSigning = listOf(releaseStore, releaseStorePassword, releaseAlias, releaseKeyPassword).all { !it.isNullOrBlank() }
    if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = file(requireNotNull(releaseStore))
            storePassword = requireNotNull(releaseStorePassword)
            keyAlias = requireNotNull(releaseAlias)
            keyPassword = requireNotNull(releaseKeyPassword)
        }
    }
    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

val buildAndroidCore by tasks.registering(Exec::class) {
    val ndkHome = android.ndkDirectory.absolutePath
    workingDir = file("src/main/rust")
    environment("CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER", "$ndkHome/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android24-clang")
    commandLine("cargo", "build", "--release", "--locked", "--offline", "--target", "aarch64-linux-android")
    doLast {
        copy {
            from(file("src/main/rust/target/aarch64-linux-android/release/libti99pocket_core_android.so"))
            into(file("src/main/jniLibs/arm64-v8a"))
        }
    }
}

tasks.named("preBuild").configure { dependsOn(buildAndroidCore) }

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
