package com.adamfoerster.syncthing.feature.conflicts.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.DiffOp
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.HunkResolution
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.InlineHighlight
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.InlineSpan
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.LineDiff
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeBlock
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeDocument
import kotlin.random.Random
import kotlin.test.Test

class MergeDocumentTest {

    private val mine = """
        Lista de compras — sábado

        Frutas
        - Maçã
        - Banana

        - Leite integral (2 caixas)
        - Pão de forma

        Limpeza
        - Detergente
        - Sabão em pó

        Lembrar: pagar a conta de luz
    """.trimIndent() + "\n"

    private val theirs = """
        Lista de compras — sábado

        Frutas
        - Maçã
        - Banana

        - Leite desnatado (3 caixas)
        - Pão integral

        Limpeza
        - Detergente
        - Sabão líquido
        - Amaciante

        Lembrar: pagar a conta de luz
    """.trimIndent() + "\n"

    @Test
    fun splitsIntoCommonPassagesAndHunks() {
        val doc = MergeDocument.of(mine, theirs)

        assertThat(doc.hunks).hasSize(2)
        assertThat(doc.hunks[0].current).containsExactly("- Leite integral (2 caixas)", "- Pão de forma")
        assertThat(doc.hunks[0].copy).containsExactly("- Leite desnatado (3 caixas)", "- Pão integral")
        assertThat(doc.hunks[1].current).containsExactly("- Sabão em pó")
        assertThat(doc.hunks[1].copy).containsExactly("- Sabão líquido", "- Amaciante")
    }

    @Test
    fun rendersOnlyWhenEveryHunkIsDecided() {
        val doc = MergeDocument.of(mine, theirs)
        assertThat(doc.render(mapOf(0 to HunkResolution.CURRENT))).isNull()

        val merged = doc.render(mapOf(0 to HunkResolution.COPY, 1 to HunkResolution.BOTH_CURRENT_FIRST))
        assertThat(merged).isNotNull()
        assertThat(merged!!.lines()).containsExactly(
            "Lista de compras — sábado", "", "Frutas", "- Maçã", "- Banana", "",
            "- Leite desnatado (3 caixas)", "- Pão integral",
            "", "Limpeza", "- Detergente",
            "- Sabão em pó", "- Sabão líquido", "- Amaciante",
            "", "Lembrar: pagar a conta de luz", "",
        )
    }

    @Test
    fun takingOneSideEverywhereReproducesThatFile() {
        val doc = MergeDocument.of(mine, theirs)
        val all = { r: HunkResolution -> doc.hunks.associate { it.index to r } }
        assertThat(doc.render(all(HunkResolution.CURRENT))).isEqualTo(mine)
        assertThat(doc.render(all(HunkResolution.COPY))).isEqualTo(theirs)
    }

    @Test
    fun keepsWindowsLineEndingsAndMissingFinalNewline() {
        val doc = MergeDocument.of("a\r\nb\r\nc", "a\r\nB\r\nc")
        assertThat(doc.render(mapOf(0 to HunkResolution.COPY))).isEqualTo("a\r\nB\r\nc")
    }

    @Test
    fun identicalContentHasNoHunks() {
        assertThat(MergeDocument.of("x\ny\n", "x\r\ny\r\n").hunks).hasSize(0)
    }

    @Test
    fun hunkMayBeEmptyOnOneSide() {
        val doc = MergeDocument.of("a\nc\n", "a\nb\nc\n")
        val hunk = doc.blocks.filterIsInstance<MergeBlock.Hunk>().single()
        assertThat(hunk.current).hasSize(0)
        assertThat(hunk.copy).containsExactly("b")
    }

    @Test
    fun markersSurroundUndecidedHunksOnly() {
        val doc = MergeDocument.of(mine, theirs)
        val text = doc.renderWithMarkers(mapOf(0 to HunkResolution.CURRENT), "Este celular", "Notebook")
        assertThat(text.lines().filter { it.startsWith("<<<<<<<") }).containsExactly("<<<<<<< Este celular")
        assertThat(MergeDocument.hasConflictMarkers(text)).isTrue()
        assertThat(MergeDocument.hasConflictMarkers(doc.render(doc.hunks.associate { it.index to HunkResolution.COPY })!!))
            .isFalse()
    }

    @Test
    fun diffIsAValidEditScript() {
        val random = Random(42)
        repeat(200) {
            val a = List(random.nextInt(0, 30)) { "l${random.nextInt(0, 6)}" }
            val b = List(random.nextInt(0, 30)) { "l${random.nextInt(0, 6)}" }
            val ops = LineDiff.diff(a, b)
            // Replaying the script must rebuild both sides in order.
            assertThat(ops.filter { it !is DiffOp.Insert }.map {
                when (it) {
                    is DiffOp.Equal -> a[it.aIndex]
                    is DiffOp.Delete -> a[it.aIndex]
                    else -> error("unreachable")
                }
            }).isEqualTo(a)
            assertThat(ops.filter { it !is DiffOp.Delete }.map {
                when (it) {
                    is DiffOp.Equal -> b[it.bIndex].also { line -> assertThat(a[it.aIndex]).isEqualTo(line) }
                    is DiffOp.Insert -> b[it.bIndex]
                    else -> error("unreachable")
                }
            }).isEqualTo(b)
        }
    }

    @Test
    fun fallsBackToOneHunkWhenFilesShareNothing() {
        val a = List(50) { "a$it" }
        val b = List(50) { "b$it" }
        val ops = LineDiff.diff(a, b, maxEdits = 10)
        assertThat(ops.count { it is DiffOp.Delete }).isEqualTo(50)
        assertThat(ops.count { it is DiffOp.Insert }).isEqualTo(50)
    }

    @Test
    fun inlineHighlightCoversWholeWords() {
        val (a, b) = InlineHighlight.between("- Pão de forma", "- Pão integral")!!
        assertThat("- Pão de forma".substring(a.start, a.end)).isEqualTo("de forma")
        assertThat("- Pão integral".substring(b.start, b.end)).isEqualTo("integral")
        assertThat(InlineHighlight.between("same", "same")).isNull()
        assertThat(InlineHighlight.between("- Sabão em pó", "- Sabão líquido")?.first)
            .isEqualTo(InlineSpan(8, 13))
    }
}
