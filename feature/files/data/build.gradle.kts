plugins {
    alias(libs.plugins.syncthing.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.files.domain)
            implementation(libs.okio)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
