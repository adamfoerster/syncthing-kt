package com.adamfoerster.syncthing.core.presentation

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import java.io.File
import kotlin.test.Test

/** Every module's strings must exist in all supported languages with the same placeholders. */
class TranslationsTest {
    private val languages = listOf("pt", "es", "it", "de", "fr", "el")

    private data class Entry(val key: String, val placeholders: Set<String>)

    private fun projectRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return requireNotNull(dir) { "settings.gradle.kts not found" }
    }

    /** Directories holding `values/strings.xml` (Compose resources and Android res). */
    private fun resourceRoots(): List<File> = projectRoot().walkTopDown()
        .onEnter { it.name != "build" && it.name != ".git" && it.name != ".gradle" }
        .filter { it.isFile && it.name == "strings.xml" && it.parentFile.name == "values" }
        .map { it.parentFile.parentFile }
        .toList()

    private fun parse(file: File): List<Entry> {
        val text = file.readText()
        val placeholder = Regex("""%\d+\$[sd]""")
        val strings = Regex("""<string name="([^"]+)">(.*?)</string>""").findAll(text)
            .map { Entry(it.groupValues[1], placeholder.findAll(it.groupValues[2]).map { m -> m.value }.toSet()) }
        // Plural items: one Entry per quantity so "one" and "other" both need their placeholders.
        val plurals = Regex("""<plurals name="([^"]+)">(.*?)</plurals>""", RegexOption.DOT_MATCHES_ALL).findAll(text)
            .flatMap { block ->
                Regex("""<item quantity="(\w+)">(.*?)</item>""").findAll(block.groupValues[2]).map {
                    Entry(
                        "${block.groupValues[1]}#${it.groupValues[1]}",
                        placeholder.findAll(it.groupValues[2]).map { m -> m.value }.toSet(),
                    )
                }
            }
        return (strings + plurals).toList()
    }

    @Test
    fun `finds the resource folders`() {
        assertThat(resourceRoots()).isNotEmpty()
    }

    @Test
    fun `every language translates every string with the same placeholders`() {
        val problems = mutableListOf<String>()
        for (root in resourceRoots()) {
            val default = parse(File(root, "values/strings.xml")).associateBy { it.key }
            for (lang in languages) {
                val file = File(root, "values-$lang/strings.xml")
                if (!file.exists()) {
                    problems += "${root.name}: missing values-$lang"
                    continue
                }
                val translated = parse(file).associateBy { it.key }
                (default.keys - translated.keys).forEach { problems += "${root.path}: $lang lacks $it" }
                (translated.keys - default.keys).forEach { problems += "${root.path}: $lang has extra $it" }
                for ((key, entry) in default) {
                    val other = translated[key] ?: continue
                    // Plural "other" may drop the number when a language words it that way; "one" may too.
                    if (!key.contains('#') && other.placeholders != entry.placeholders) {
                        problems += "${root.path}: $lang/$key placeholders ${other.placeholders} != ${entry.placeholders}"
                    }
                }
            }
        }
        assertThat(problems).isEmpty()
    }

    @Test
    fun `no string needs apostrophe escaping`() {
        val offenders = resourceRoots().flatMap { root ->
            root.listFiles { f -> f.name.startsWith("values") }.orEmpty().map { File(it, "strings.xml") }
        }.filter { it.exists() && Regex("""<(string|item)[^>]*>[^<]*'[^<]*<""").containsMatchIn(it.readText()) }
        assertThat(offenders.map { it.path }).isEqualTo(emptyList())
    }
}
