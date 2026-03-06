import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

group = findProperty("GROUP") as String
version = findProperty("VERSION_NAME") as String

android {
    namespace = "com.commit451.veyron.rx"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += "DuplicatePlatformClasses"
    }
}

dependencies {
    implementation(project(":veyron"))
    api("io.reactivex.rxjava3:rxjava:3.1.10")
}

mavenPublishing {
    configure(AndroidSingleVariantLibrary("release", true, true))
    coordinates("com.commit451.veyron", "rxjava", version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (System.getenv("RELEASE_SIGNING_ENABLED") == "true") {
        signAllPublications()
    }
}
