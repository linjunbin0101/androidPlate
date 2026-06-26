plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.zkc.plate"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("proguard-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // HyperLPR3 — Chinese license plate recognition SDK
    implementation("com.github.HyperInspire:hyperlpr3-android-sdk:1.0.3")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.zkc.plate"
                artifactId = "plate-sdk"
                version = "1.0.0"
            }
        }
    }
}
