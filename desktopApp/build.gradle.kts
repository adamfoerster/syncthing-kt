import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion: String = providers.gradleProperty("app.version").get()

// Windows (and, later, macOS) host for the shared UI. Plain JVM: the OS-specific pieces live here.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(projects.app)
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.presentation)
    implementation(libs.kotlinx.coroutines.core)
    // Dispatchers.Main for ViewModels on the desktop.
    implementation(libs.kotlinx.coroutines.swing)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.assertk)
    testImplementation(libs.junit)
}

compose.resources {
    packageOfResClass = "com.adamfoerster.meshsync.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "com.adamfoerster.meshsync.desktop.MainKt"
        // Skiko loads a native library; silences the JDK's restricted-method warning.
        jvmArgs += "--enable-native-access=ALL-UNNAMED"

        nativeDistributions {
            // resources/<os>-<arch>/ holds the Syncthing binary from :syncthing-native:buildWindows. The
            // plugin points `compose.application.resources.dir` at it, both for `run` and when packaged.
            appResourcesRootDir.set(layout.projectDirectory.dir("resources"))

            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "MeshSync"
            packageVersion = appVersion
            description = "Syncs folders between your devices with Syncthing"
            vendor = "MeshSync"

            // From :desktopApp:suggestRuntimeModules, plus what OkHttp reaches for at runtime.
            modules("java.instrument", "java.management", "jdk.unsupported", "java.naming", "jdk.crypto.ec")

            windows {
                iconFile.set(layout.projectDirectory.file("packaging/meshsync.ico"))
                menu = true
                menuGroup = "MeshSync"
                shortcut = true
                // Per user: no admin prompt, and the autostart entry lives in the user's registry hive.
                perUserInstall = true
                dirChooser = true
                // Fixed forever: it is how Windows recognises a newer MeshSync as an upgrade of the installed one.
                upgradeUuid = "6f3a1c52-8d47-4b1e-9a60-2c5e7b0d4f18"
            }
        }
    }
}

// Same rule as :androidApp: no build without a bumped version and a matching CHANGELOG entry.
tasks.named("compileKotlin") { dependsOn(":verifyVersion") }

val checkSyncthingBinary by tasks.registering {
    group = "verification"
    description = "Fails if the bundled syncthing.exe has not been built yet"
    val binary = layout.projectDirectory.file("resources/windows-x64/syncthing.exe")
    doLast {
        check(binary.asFile.exists()) { "Missing $binary. Run ./gradlew :syncthing-native:buildWindows first (needs Go)." }
    }
}
tasks.matching { it.name == "createDistributable" || it.name.startsWith("package") }
    .configureEach { dependsOn(checkSyncthingBinary) }

tasks.test {
    useJUnit()
}
