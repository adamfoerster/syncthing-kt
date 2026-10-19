plugins {
    alias(libs.plugins.syncthing.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.domain)
            implementation(projects.core.designSystem)
            api(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.runtime.compose)
            api(libs.kotlinx.datetime)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.adamfoerster.syncthing.core.presentation.resources"
}
