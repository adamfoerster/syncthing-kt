plugins {
    alias(libs.plugins.syncthing.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
            implementation(libs.qrose)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.syncthing.feature.devices.presentation.resources"
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}
