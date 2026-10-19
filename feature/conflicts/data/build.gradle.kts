plugins {
    alias(libs.plugins.syncthing.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.conflicts.domain)
            implementation(projects.core.data)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}
