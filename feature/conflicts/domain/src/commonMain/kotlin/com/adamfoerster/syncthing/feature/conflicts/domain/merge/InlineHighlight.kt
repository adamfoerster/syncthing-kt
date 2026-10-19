package com.adamfoerster.syncthing.feature.conflicts.domain.merge

/**
 * The changed span inside a line that has a counterpart on the other side, found by trimming the
 * common prefix and suffix (`- Leite integral` vs `- Leite desnatado` → `integral` / `desnatado`).
 */
data class InlineSpan(val start: Int, val end: Int) {
    val isEmpty: Boolean get() = start >= end
}

object InlineHighlight {
    /** Spans for [a] and [b]; null when the lines are too different for a highlight to help. */
    fun between(a: String, b: String): Pair<InlineSpan, InlineSpan>? {
        if (a == b) return null
        var prefix = 0
        val minLen = minOf(a.length, b.length)
        while (prefix < minLen && a[prefix] == b[prefix]) prefix++
        // Don't cut a word in half: widen the highlight to whole words.
        while (prefix > 0 && a[prefix - 1].isWordChar() && (a.isWordAt(prefix) || b.isWordAt(prefix))) prefix--
        var suffix = 0
        while (suffix < minLen - prefix && a[a.length - 1 - suffix] == b[b.length - 1 - suffix]) suffix++
        while (suffix > 0 && a[a.length - suffix].isWordChar() &&
            (a.isWordAt(a.length - suffix - 1) || b.isWordAt(b.length - suffix - 1))
        ) suffix--
        if (prefix + suffix == 0) return null
        return InlineSpan(prefix, a.length - suffix) to InlineSpan(prefix, b.length - suffix)
    }

    private fun Char.isWordChar() = isLetterOrDigit()
    private fun String.isWordAt(index: Int) = index in indices && this[index].isWordChar()
}
