plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

/**
 * Fails unless `app.version` is a SemVer and the newest release in CHANGELOG.md is that version,
 * so a change can't ship without bumping the version and writing its changelog entry.
 */
abstract class VerifyVersionTask : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:InputFile abstract val changelog: RegularFileProperty

    @TaskAction
    fun verify() {
        val expected = version.get()
        require(Regex("""^\d+\.\d+\.\d+$""").matches(expected)) {
            "app.version '$expected' in gradle.properties is not MAJOR.MINOR.PATCH"
        }
        val latest = changelog.get().asFile.readLines()
            .firstNotNullOfOrNull { Regex("""^## \[(\d+\.\d+\.\d+)]""").find(it)?.groupValues?.get(1) }
        require(latest == expected) {
            "CHANGELOG.md's newest release is [$latest] but app.version is $expected. " +
                "Bump app.version and add a '## [$expected] - YYYY-MM-DD' entry describing the change."
        }
    }
}

tasks.register<VerifyVersionTask>("verifyVersion") {
    group = "verification"
    description = "Checks that app.version and the newest CHANGELOG.md entry match"
    version.set(providers.gradleProperty("app.version"))
    changelog.set(layout.projectDirectory.file("CHANGELOG.md"))
}
