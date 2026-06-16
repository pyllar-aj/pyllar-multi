import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File as JFile

// Load shared keystore properties (same file used by the standalone Android project)
val keystorePropertiesFile = rootProject.file("../pyllar/android/keystore.properties")
val keystoreProperties = mutableMapOf<String, String>()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.readLines().forEach { line ->
        if (line.contains("=") && !line.trimStart().startsWith("#")) {
            val (k, v) = line.split("=", limit = 2)
            keystoreProperties[k.trim()] = v.trim()
        }
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    publicResClass = true
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    iosTargets.forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
           freeCompilerArgs += listOf("-Xbinary=bundleId=com.pyllar.consumer")
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.material.icons)
            implementation(libs.compottie)
            implementation(libs.qrose)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.play.services.location)
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.play.services.auth.api.phone)
            implementation(libs.androidx.core.ktx)
            implementation(libs.play.app.update)
            implementation(libs.play.review)
            implementation(libs.play.review.ktx)
            implementation(libs.installreferrer)
            implementation(libs.firebase.messaging)
            implementation("androidx.appcompat:appcompat:1.6.1")
            implementation("com.google.firebase:firebase-crashlytics")
//            implementation("com.appsflyer:af-android-sdk:6.16.2")
//            implementation("com.microsoft.clarity:clarity-compose:3.6.0")
//
            // Analytics, attribution and session recording
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:32.7.4"))
            implementation("com.google.firebase:firebase-analytics")
            implementation("com.microsoft.clarity:clarity-compose:3.6.0")
            implementation("com.facebook.android:facebook-android-sdk:17.0.2")
            implementation("com.appsflyer:af-android-sdk:6.16.2")
            implementation("com.singular.sdk:singular_sdk:12.7.1")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}

// ──────────────────────────────────────────────────
// iOS flavor – base URL injected via Gradle property
// Usage: -Ppyllar.flavor=debug  (default: release)
// ──────────────────────────────────────────────────
val pyllarFlavor: String = (project.findProperty("pyllar.flavor") as? String) ?: "release"
val iosBaseUrl: String = when (pyllarFlavor) {
    "debug" -> "http://localhost:8080"
    else    -> "https://api.pyllar.in"
}

val generateIosBuildConfig by tasks.registering {
    val flavor = pyllarFlavor
    val baseUrl = iosBaseUrl
    val outputDir = JFile(project.projectDir, "build/generated/ios-build-config")
    inputs.property("flavor", flavor)
    outputs.dir(outputDir)
    doLast {
        val pkg = "com.pyllar.consumer.config"
        val outFile = JFile(outputDir, "com/pyllar/consumer/config/IosBuildConfig.kt")
        outFile.parentFile.mkdirs()
        outFile.writeText("""
            package $pkg

            /** Auto-generated by Gradle. Do NOT edit manually. */
            internal const val IOS_BASE_URL: String = "$baseUrl"
            internal const val IS_DEBUG: Boolean = ${flavor == "debug"}
        """.trimIndent())
        println("[pyllar] iOS flavor=$flavor  baseUrl=$baseUrl")
    }
}

// Wire the generated directory safely to the iOS source set so Gradle knows about the task dependency
kotlin.sourceSets.getByName("iosMain") {
    kotlin.srcDir(generateIosBuildConfig.map { it.outputs.files.singleFile })
}

android {
    namespace = "com.pyllar.consumer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        val appsflyerKey = "\"${keystoreProperties["appsflyerDevKey"] ?: ""}\""
        val singularApiKey = "\"${keystoreProperties["singularApiKey"] ?: ""}\""
        val singularSecretKey = "\"${keystoreProperties["singularSecretKey"] ?: ""}\""
        debug {
            // Debug / local backend (matches pyllar.flavor=debug)
            buildConfigField("String", "BASE_URL", "\"http://10.222.186.212:8080\"")
            buildConfigField("String", "APPSFLYER_DEV_KEY", appsflyerKey)
            buildConfigField("String", "SINGULAR_API_KEY", singularApiKey)
            buildConfigField("String", "SINGULAR_SECRET_KEY", singularSecretKey)
        }
        getByName("release") {
            isMinifyEnabled = false
            // Production backend (matches pyllar.flavor=release)
            buildConfigField("String", "BASE_URL", "\"https://api.pyllar.in\"")
            buildConfigField("String", "APPSFLYER_DEV_KEY", appsflyerKey)
            buildConfigField("String", "SINGULAR_API_KEY", singularApiKey)
            buildConfigField("String", "SINGULAR_SECRET_KEY", singularSecretKey)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

