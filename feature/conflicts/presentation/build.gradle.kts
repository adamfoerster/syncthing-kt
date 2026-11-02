plugins {
    alias(libs.plugins.syncthing.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.conflicts.domain)
            implementation(libs.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.meshsync.feature.conflicts.presentation.resources"
}

dependencies {
    "androidRuntimeClasspath"(libs.compose.ui.tooling)
}
