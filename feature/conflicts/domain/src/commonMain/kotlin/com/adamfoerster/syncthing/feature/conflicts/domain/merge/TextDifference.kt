package com.adamfoerster.syncthing.feature.conflicts.domain.merge

/**
 * Differences between two texts that the line diff ([MergeDocument]) does not show as changed lines.
 * They explain a conflict that lists no passages even though the files are not byte-for-byte equal.
 */
enum class TextDifference {
    /** One version uses Windows line endings (CRLF) and the other Unix ones (LF). */
    LINE_ENDINGS,

    /** Only one version ends with a line break. */
    FINAL_NEWLINE,
    ;

    companion object {
        /** Empty when the texts are equal or when they differ in something no case here explains. */
        fun between(current: String, copy: String): Set<TextDifference> {
            if (current == copy) return emptySet()
            val found = linkedSetOf<TextDifference>()
            if (("\r\n" in current) != ("\r\n" in copy)) found += LINE_ENDINGS
            val na = current.replace("\r\n", "\n")
            val nb = copy.replace("\r\n", "\n")
            if (na.endsWith('\n') != nb.endsWith('\n')) found += FINAL_NEWLINE
            return found
        }
    }
}
