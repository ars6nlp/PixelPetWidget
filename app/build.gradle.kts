import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing. The values come from keystore.properties in the project root (never in git,
// see keystore.properties.example) or from the environment variables PIXELPET_STORE_FILE,
// PIXELPET_STORE_PASSWORD, PIXELPET_KEY_ALIAS and PIXELPET_KEY_PASSWORD. Without them the
// release build is simply left unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun signingValue(key: String, environment: String): String? =
    keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environment)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "PIXELPET_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "PIXELPET_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "PIXELPET_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "PIXELPET_KEY_PASSWORD")
val canSignRelease = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    .all { it != null } && rootProject.file(releaseStoreFile!!).isFile

android {
    namespace = "com.example.pixelpetwidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aixdev.pixelpet"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    bundle {
        language {
            // The app has its own RU/EN switch, so every install needs all languages
            enableSplit = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Brings the User Messaging Platform (consent to ads, GDPR) that Ads.java uses
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.play:review:2.0.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}