plugins {
    alias(libs.plugins.syncthing.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.syncthing.feature.folders.presentation.resources"
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}
