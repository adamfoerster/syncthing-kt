import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.util.Properties
import javax.inject.Inject

/*
 * Builds the Syncthing core (Go) for Android and drops it into the app's jniLibs as
 * libsyncthing.so — the same approach as syncthing-android's build-syncthing.py.
 *
 * Needs: git, Go (>= the version Syncthing's go.mod asks for) and the Android NDK.
 *   ./gradlew :syncthing-native:buildNative                 # all ABIs
 *   ./gradlew :syncthing-native:buildNative -Pabis=arm64-v8a,x86_64
 */

val syncthingVersion = "v2.1.6"

abstract class BuildSyncthingTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:Input abstract val minSdk: Property<String>
    @get:Input abstract val abis: ListProperty<String>
    @get:Input abstract val ndkDir: Property<String>
    @get:Internal abstract val sourceDir: DirectoryProperty
    @get:OutputDirectory abstract val jniLibsDir: DirectoryProperty

    @TaskAction
    fun build() {
        val src = sourceDir.get().asFile
        if (!File(src, "build.go").exists()) {
            src.parentFile.mkdirs()
            exec.exec {
                commandLine("git", "clone", "--depth", "1", "--branch", version.get(), "https://github.com/syncthing/syncthing.git", src.absolutePath)
            }
        }
        val ndk = File(ndkDir.get())
        require(ndk.isDirectory) { "Android NDK not found at $ndk. Install it with the SDK manager or set ANDROID_NDK_HOME." }
        val windows = System.getProperty("os.name").lowercase().contains("windows")
        val host = when {
            windows -> "windows-x86_64"
            System.getProperty("os.name").lowercase().contains("mac") -> "darwin-x86_64"
            else -> "linux-x86_64"
        }
        val goBinary = if (windows) "go.exe" else "go"

        for (abi in abis.get()) {
            val (goArch, triple) = TARGETS[abi] ?: error("Unknown ABI $abi; use one of ${TARGETS.keys}")
            val clang = File(ndk, "toolchains/llvm/prebuilt/$host/bin/$triple${minSdk.get()}-clang" + if (windows) ".cmd" else "")
            require(clang.exists()) { "NDK compiler not found: $clang" }
            logger.lifecycle("Building Syncthing ${version.get()} for $abi")
            exec.exec {
                workingDir(src)
                environment("CGO_ENABLED", "1")
                // Needed by the network-interface workaround Syncthing uses on Android (wlynxg/anet).
                environment("EXTRA_LDFLAGS", "-checklinkname=0")
                environment("BUILD_USER", "syncthing-kt")
                environment("SOURCE_DATE_EPOCH", "0")
                commandLine(
                    goBinary, "run", "build.go",
                    "-goos", "android", "-goarch", goArch, "-cc", clang.absolutePath,
                    "-version", version.get(), "-no-upgrade", "build",
                )
            }
            val built = File(src, if (windows) "syncthing.exe" else "syncthing").takeIf { it.exists() } ?: File(src, "syncthing")
            val target = File(jniLibsDir.get().asFile, "$abi/libsyncthing.so")
            target.parentFile.mkdirs()
            built.copyTo(target, overwrite = true)
            built.delete()
        }
    }

    companion object {
        /** ABI → (GOARCH, clang target triple) */
        val TARGETS = mapOf(
            "arm64-v8a" to ("arm64" to "aarch64-linux-android"),
            "armeabi-v7a" to ("arm" to "armv7a-linux-androideabi"),
            "x86_64" to ("amd64" to "x86_64-linux-android"),
            "x86" to ("386" to "i686-linux-android"),
        )
    }
}

fun findNdk(): String {
    System.getenv("ANDROID_NDK_HOME")?.let { return it }
    val props = Properties().apply {
        rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
    }
    val sdk = props.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME") ?: return ""
    // Newest installed NDK.
    return File(sdk, "ndk").listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }?.absolutePath ?: ""
}

tasks.register<BuildSyncthingTask>("buildNative") {
    group = "build"
    description = "Compiles the Syncthing core for Android into androidApp/src/main/jniLibs"
    version.set(syncthingVersion)
    minSdk.set(libs.versions.android.minSdk)
    abis.set(
        providers.gradleProperty("abis").map { it.split(',').map(String::trim) }
            .orElse(BuildSyncthingTask.TARGETS.keys.toList()),
    )
    ndkDir.set(findNdk())
    sourceDir.set(layout.projectDirectory.dir("go/syncthing"))
    jniLibsDir.set(rootProject.layout.projectDirectory.dir("androidApp/src/main/jniLibs"))
}
