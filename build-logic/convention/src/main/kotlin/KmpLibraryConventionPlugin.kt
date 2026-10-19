import com.adamfoerster.syncthing.buildlogic.derivedNamespace
import com.adamfoerster.syncthing.buildlogic.int
import com.adamfoerster.syncthing.buildlogic.lib
import com.adamfoerster.syncthing.buildlogic.libs
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Base for every shared module: Kotlin Multiplatform with Android as the only target for now.
 * Code goes to commonMain unless it needs an Android API, so new targets only need actuals.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            (this as ExtensionAware).extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = derivedNamespace
                compileSdk = libs.int("android-compileSdk")
                minSdk = libs.int("android-minSdk")
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
                // Without this, Compose Resources (strings, fonts) are left out of the APK.
                androidResources { enable = true }
                // Runs commonTest on the JVM, no device needed.
                withHostTest {}
            }

            applyDefaultHierarchyTemplate()

            sourceSets.commonMain.dependencies {
                implementation(libs.lib("kotlinx-coroutines-core"))
            }
            sourceSets.commonTest.dependencies {
                implementation(libs.lib("kotlin-test"))
                implementation(libs.lib("kotlinx-coroutines-test"))
                implementation(libs.lib("assertk"))
                implementation(libs.lib("turbine"))
            }

            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
                optIn.add("kotlin.time.ExperimentalTime")
            }
        }
    }
}
