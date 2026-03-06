import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

group = findProperty("GROUP") as String
version = findProperty("VERSION_NAME") as String

android {
    namespace = "com.commit451.veyron"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-shrinker-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
            )
        }
    }

    lint {
        disable += "DuplicatePlatformClasses"
    }
}

dependencies {
    // Beware of updating the next 3 deps. Can cause weird
    // build issues due to the Apache HTTP client
    api("com.google.http-client:google-http-client-gson:1.47.0")
    api("com.google.api-client:google-api-client-android:2.8.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    api("com.google.apis:google-api-services-drive:v3-rev20250511-2.0.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    api("com.squareup.moshi:moshi:1.15.2")
    api("com.commit451:okyo:3.0.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("junit:junit:4.13.2")
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary("release", true, true))
    coordinates("com.commit451.veyron", "veyron", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (System.getenv("RELEASE_SIGNING_ENABLED") == "true") {
        signAllPublications()
    }
}
