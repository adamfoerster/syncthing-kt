package com.adamfoerster.syncthing.core.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.model.IgnorePreset
import com.adamfoerster.syncthing.core.domain.model.IgnoreRule
import com.adamfoerster.syncthing.core.domain.model.IgnoreRules
import kotlin.test.Test

class IgnoreRulesTest {

    @Test
    fun classifiesLinesForTheSimpleEditor() {
        assertThat(IgnoreRules.classify("*.tmp")).isEqualTo(IgnoreRule.FileType("tmp"))
        assertThat(IgnoreRules.classify("*.tar.gz")).isEqualTo(IgnoreRule.FileType("tar.gz"))
        assertThat(IgnoreRules.classify("node_modules")).isEqualTo(IgnoreRule.Name("node_modules"))
        assertThat(IgnoreRules.classify("/Camera/cache")).isEqualTo(IgnoreRule.Name("/Camera/cache"))
        assertThat(IgnoreRules.classify("My Videos")).isEqualTo(IgnoreRule.Name("My Videos"))
        assertThat(IgnoreRules.classify("// my notes")).isEqualTo(IgnoreRule.Comment("// my notes"))
    }

    @Test
    fun anythingBeyondPlainNamesAndFileTypesIsAdvanced() {
        listOf("!keep.txt", "(?i)photo*", "(?d).DS_Store", "**/cache", "#include more", "file?.txt", "a/*.log", "*.{jpg,png}")
            .forEach { assertThat(IgnoreRules.classify(it)).isEqualTo(IgnoreRule.Advanced(it)) }
    }

    @Test
    fun blankLinesAreDroppedWhenParsing() {
        assertThat(IgnoreRules.parse(listOf("*.tmp", "", "  ", "cache")).map { it.line }).containsExactly("*.tmp", "cache")
    }

    @Test
    fun normalizeTrimsAndRejectsEmptyOrMultilineInput() {
        assertThat(IgnoreRules.normalize("  *.log ")).isEqualTo("*.log")
        assertThat(IgnoreRules.normalize("   ")).isNull()
        assertThat(IgnoreRules.normalize("a\nb")).isNull()
    }

    @Test
    fun addSkipsDuplicatesAndRemoveDropsEveryCopy() {
        assertThat(IgnoreRules.add(listOf("a"), "a")).containsExactly("a")
        assertThat(IgnoreRules.add(listOf("a"), "b")).containsExactly("a", "b")
        assertThat(IgnoreRules.remove(listOf("a", "b", "a"), "a")).containsExactly("b")
    }

    @Test
    fun presetTogglesOnWithOnlyMissingLinesAndOffRemovingAllOfThem() {
        val partly = listOf("keep", "*.tmp")
        val on = IgnoreRules.toggle(partly, IgnorePreset.TEMPORARY_FILES)
        assertThat(on).containsExactly("keep", "*.tmp", "*.temp", "~$*")
        assertThat(IgnoreRules.isActive(on, IgnorePreset.TEMPORARY_FILES)).isTrue()

        val off = IgnoreRules.toggle(on, IgnorePreset.TEMPORARY_FILES)
        assertThat(off).containsExactly("keep")
        assertThat(IgnoreRules.isActive(off, IgnorePreset.TEMPORARY_FILES)).isFalse()
    }

    @Test
    fun textRoundTripKeepsCommentsAndOrderButNotTrailingBlankLines() {
        val lines = listOf("// photos", "*.tmp", "", "(?d)cache")
        assertThat(IgnoreRules.fromText(IgnoreRules.toText(lines))).isEqualTo(lines)
        assertThat(IgnoreRules.fromText("a\r\nb\n\n")).containsExactly("a", "b")
        assertThat(IgnoreRules.fromText("")).isEmpty()
    }
}
