plugins {
    alias(libs.plugins.syncthing.kmp.feature)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.feature.conflicts.data)
            implementation(projects.feature.conflicts.presentation)
            implementation(projects.feature.home.presentation)
            implementation(projects.feature.sharing.presentation)
            implementation(projects.feature.folders.presentation)
            implementation(projects.feature.devices.presentation)
            implementation(projects.feature.settings.presentation)
            implementation(libs.compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.adamfoerster.syncthing.app.resources"
}
