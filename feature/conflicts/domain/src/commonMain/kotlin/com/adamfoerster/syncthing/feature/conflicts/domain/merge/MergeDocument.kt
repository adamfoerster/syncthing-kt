package com.adamfoerster.syncthing.feature.conflicts.domain.merge

/**
 * A two-way merge of the [current][com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide.CURRENT]
 * file and its conflict copy, split into shared passages and differing hunks — the same model as
 * GitHub's conflict editor: each hunk is resolved by taking one side, both, or by editing the result.
 *
 * Syncthing keeps no common ancestor, so every difference is a hunk the user decides on.
 */
data class MergeDocument(
    val blocks: List<MergeBlock>,
    val lineSeparator: String,
    val endsWithNewline: Boolean,
) {
    val hunks: List<MergeBlock.Hunk> get() = blocks.filterIsInstance<MergeBlock.Hunk>()

    /** The final text, or null while any hunk has no resolution. */
    fun render(resolutions: Map<Int, HunkResolution>): String? {
        val lines = ArrayList<String>()
        for (block in blocks) {
            when (block) {
                is MergeBlock.Common -> lines += block.lines
                is MergeBlock.Hunk -> lines += block.resolve(resolutions[block.index] ?: return null)
            }
        }
        return join(lines)
    }

    /**
     * The result with Git-style markers around undecided hunks, as the starting point for editing
     * by hand. [currentLabel]/[copyLabel] go after the markers (`<<<<<<< Este celular`).
     */
    fun renderWithMarkers(
        resolutions: Map<Int, HunkResolution>,
        currentLabel: String,
        copyLabel: String,
    ): String {
        val lines = ArrayList<String>()
        for (block in blocks) {
            when (block) {
                is MergeBlock.Common -> lines += block.lines
                is MergeBlock.Hunk -> {
                    val resolution = resolutions[block.index]
                    if (resolution != null) {
                        lines += block.resolve(resolution)
                    } else {
                        lines += "$MARKER_START $currentLabel"
                        lines += block.current
                        lines += MARKER_SEPARATOR
                        lines += block.copy
                        lines += "$MARKER_END $copyLabel"
                    }
                }
            }
        }
        return join(lines)
    }

    private fun join(lines: List<String>): String {
        val body = lines.joinToString(lineSeparator)
        return if (endsWithNewline && lines.isNotEmpty()) body + lineSeparator else body
    }

    companion object {
        const val MARKER_START = "<<<<<<<"
        const val MARKER_SEPARATOR = "======="
        const val MARKER_END = ">>>>>>>"

        fun of(current: String, copy: String): MergeDocument {
            val separator = if ("\r\n" in current || ("\n" !in current && "\r\n" in copy)) "\r\n" else "\n"
            val endsWithNewline = if (current.isNotEmpty()) current.endsWith('\n') else copy.endsWith('\n')
            val a = current.splitLines()
            val b = copy.splitLines()
            return MergeDocument(group(a, b, LineDiff.diff(a, b)), separator, endsWithNewline)
        }

        /** True while hand-edited text still has a conflict marker line in it. */
        fun hasConflictMarkers(text: String): Boolean = text.lineSequence().any { line ->
            line.startsWith(MARKER_START) || line == MARKER_SEPARATOR || line.startsWith(MARKER_END)
        }

        private fun String.splitLines(): List<String> {
            if (isEmpty()) return emptyList()
            val lines = split('\n').map { it.removeSuffix("\r") }
            return if (endsWith('\n')) lines.dropLast(1) else lines
        }

        private fun group(a: List<String>, b: List<String>, ops: List<DiffOp>): List<MergeBlock> {
            val blocks = ArrayList<MergeBlock>()
            val common = ArrayList<String>()
            val removed = ArrayList<String>()
            val added = ArrayList<String>()
            var hunkIndex = 0

            fun flushCommon() {
                if (common.isNotEmpty()) blocks += MergeBlock.Common(common.toList()).also { common.clear() }
            }

            fun flushHunk() {
                if (removed.isNotEmpty() || added.isNotEmpty()) {
                    blocks += MergeBlock.Hunk(hunkIndex++, removed.toList(), added.toList())
                    removed.clear()
                    added.clear()
                }
            }

            for (op in ops) {
                when (op) {
                    is DiffOp.Equal -> {
                        flushHunk()
                        common += a[op.aIndex]
                    }
                    is DiffOp.Delete -> {
                        flushCommon()
                        removed += a[op.aIndex]
                    }
                    is DiffOp.Insert -> {
                        flushCommon()
                        added += b[op.bIndex]
                    }
                }
            }
            flushHunk()
            flushCommon()
            return blocks
        }
    }
}

sealed interface MergeBlock {
    data class Common(val lines: List<String>) : MergeBlock

    /** Lines that differ. Either side may be empty (a passage that exists in only one version). */
    data class Hunk(val index: Int, val current: List<String>, val copy: List<String>) : MergeBlock {
        fun resolve(resolution: HunkResolution): List<String> = when (resolution) {
            HunkResolution.CURRENT -> current
            HunkResolution.COPY -> copy
            HunkResolution.BOTH_CURRENT_FIRST -> current + copy
            HunkResolution.BOTH_COPY_FIRST -> copy + current
        }
    }
}

enum class HunkResolution { CURRENT, COPY, BOTH_CURRENT_FIRST, BOTH_COPY_FIRST }
