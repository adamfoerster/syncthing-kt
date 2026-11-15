import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val appVersion: String = providers.gradleProperty("app.version").get()

// Windows and macOS (Apple Silicon) host for the shared UI. Plain JVM: the OS-specific pieces live here.
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
        // jpackage and the bundled runtime come from this JDK; see packagingJdk() below.
        packagingJdk()?.let { javaHome = it }

        nativeDistributions {
            // resources/<os>-<arch>/ holds the Syncthing binary from :syncthing-native:buildWindows/buildMacos. The
            // plugin points `compose.application.resources.dir` at it, both for `run` and when packaged.
            appResourcesRootDir.set(layout.projectDirectory.dir("resources"))

            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
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

            // Apple Silicon only (jpackage builds for the JDK's own architecture). Not Developer ID signed:
            // the first launch needs right-click › Open.
            macOS {
                iconFile.set(layout.projectDirectory.file("packaging/meshsync.icns"))
                bundleID = "com.adamfoerster.meshsync"
                dockName = "MeshSync"
                packageName = "MeshSync"
                appCategory = "public.app-category.utilities"
            }
        }
    }
}

/**
 * The JDK whose jpackage builds the installers and whose runtime is bundled. Gradle's own JVM often can't:
 * Android Studio's JBR has no jpackage, and the Compose plugin refuses Homebrew builds. So: an explicit
 * -Pmeshsync.packagingJdk wins; otherwise the first of JAVA_HOME, Gradle's JVM and the installed macOS JDKs
 * (newest first) that is 17+, has jpackage, matches this machine's architecture and is not Homebrew's.
 * Null leaves the plugin's default (Gradle's JVM), which then reports what is wrong.
 */
fun packagingJdk(): String? {
    providers.gradleProperty("meshsync.packagingJdk").orNull?.let { return it }
    fun release(home: File): Map<String, String> = File(home, "release").takeIf { it.isFile }?.readLines().orEmpty()
        .mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1].trim('"') } }
        .toMap()
    fun major(home: File): Int = release(home)["JAVA_VERSION"]?.substringBefore('.')?.toIntOrNull() ?: 0
    val hostArch = System.getProperty("os.arch").let { if (it == "arm64") "aarch64" else it }
    fun usable(home: File): Boolean {
        val info = release(home)
        val hasJpackage = File(home, "bin/jpackage").exists() || File(home, "bin/jpackage.exe").exists()
        val arch = info["OS_ARCH"]?.let { if (it == "arm64") "aarch64" else it }
        return hasJpackage && major(home) >= 17 && info["IMPLEMENTOR"] != "Homebrew" && (arch == null || arch == hostArch)
    }
    val installed = listOf(File("/Library/Java/JavaVirtualMachines"), File(System.getProperty("user.home"), "Library/Java/JavaVirtualMachines"))
        .flatMap { it.listFiles()?.toList().orEmpty() }
        .map { File(it, "Contents/Home") }
        .sortedByDescending(::major)
    val candidates = listOfNotNull(System.getenv("JAVA_HOME")?.let(::File), File(System.getProperty("java.home"))) + installed
    return candidates.firstOrNull { it.isDirectory && usable(it) }?.absolutePath
}

// Same rule as :androidApp: no build without a bumped version and a matching CHANGELOG entry.
tasks.named("compileKotlin") { dependsOn(":verifyVersion") }

val hostIsMac = System.getProperty("os.name").lowercase().contains("mac")
val checkSyncthingBinary by tasks.registering {
    group = "verification"
    description = "Fails if the Syncthing binary for this OS has not been built yet"
    val binary = layout.projectDirectory.file(if (hostIsMac) "resources/macos-arm64/syncthing" else "resources/windows-x64/syncthing.exe")
    val buildTask = if (hostIsMac) "buildMacos" else "buildWindows"
    doLast {
        check(binary.asFile.exists()) { "Missing $binary. Run ./gradlew :syncthing-native:$buildTask first (needs Go)." }
    }
}
tasks.matching { it.name == "createDistributable" || it.name.startsWith("package") }
    .configureEach { dependsOn(checkSyncthingBinary) }

tasks.test {
    useJUnit()
}
