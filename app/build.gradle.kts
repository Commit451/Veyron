plugins {
    id("com.android.application")
}

android {
    namespace = "com.commit451.driveappfolderviewer.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.commit451.driveappfolderviewer.sample"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        create("release") {
            val keystorePassword = (project.findProperty("KEYSTORE_PASSWORD") as? String).orEmpty()
            storeFile = file("keystore.jks")
            storePassword = keystorePassword
            keyAlias = "commit451"
            keyPassword = keystorePassword
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "consumer-shrinker-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "consumer-shrinker-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // need to add this manually to avoid build issues, not actually using it
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("com.google.android.gms:play-services-base:18.7.2")
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation("com.google.android.material:material:1.13.0")

    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.squareup.moshi:moshi-adapters:1.15.2")

    implementation("com.github.Commit451.AdapterLayout:adapterlayout:1.2.0")
    implementation("com.commit451:aloy:1.2.1")
    implementation("com.github.Commit451:DriveAppFolderViewer:4.0.0")

    implementation(project(":veyron"))
    implementation(project(":rxjava"))
}
