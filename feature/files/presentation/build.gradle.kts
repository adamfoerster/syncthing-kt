plugins {
    alias(libs.plugins.syncthing.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.files.domain)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.backhandler)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.meshsync.feature.files.presentation.resources"
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}
