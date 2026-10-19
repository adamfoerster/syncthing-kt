plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val appVersionName: String = providers.gradleProperty("app.version").get()

/** 0.1.0 -> 100, 1.2.3 -> 10203: derived so it never has to be bumped by hand. */
val appVersionCode: Int = appVersionName.split(".").map(String::toInt).let { (major, minor, patch) ->
    major * 10_000 + minor * 100 + patch
}

android {
    namespace = "com.adamfoerster.syncthing.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.adamfoerster.syncthing"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // The Syncthing core ships as libsyncthing.so but is an executable: it must be extracted to
        // nativeLibraryDir (the only app location Android lets us exec from), not mapped from the APK.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// Every build checks that the version was bumped together with CHANGELOG.md.
tasks.named("preBuild") { dependsOn(":verifyVersion") }

dependencies {
    implementation(projects.app)
    implementation(projects.core.data)
    implementation(projects.core.presentation)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.zxing.android.embedded)

    testImplementation(libs.junit)
    testImplementation(libs.assertk)
}
