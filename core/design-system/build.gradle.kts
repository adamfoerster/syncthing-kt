plugins {
    alias(libs.plugins.syncthing.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.syncthing.core.designsystem.resources"
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}
