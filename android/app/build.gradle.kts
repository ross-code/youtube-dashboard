plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ytdash"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ytdash"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    sourceSets.getByName("main") {
        assets.srcDir(layout.buildDirectory.dir("generated/dashboardAssets"))
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
}

// The dashboard at the repo root is the single source of truth; copy it into
// assets at build time instead of committing a second copy.
val syncDashboardAssets by tasks.registering(Sync::class) {
    from(layout.projectDirectory.file("../../index.html"))
    into(layout.buildDirectory.dir("generated/dashboardAssets"))
}

tasks.named("preBuild") {
    dependsOn(syncDashboardAssets)
}
