package com.adamfoerster.syncthing.core.domain.model

/**
 * One line of a folder's `.stignore`, classified so the simple editor can describe it in plain words.
 * Anything the simple editor does not understand is [Advanced] and is always kept verbatim.
 */
sealed interface IgnoreRule {
    val line: String

    /** `*.ext`: every file with this extension, in any subfolder. */
    data class FileType(val extension: String) : IgnoreRule {
        override val line: String get() = "*.$extension"
    }

    /** A plain file or folder name (or `/path` from the folder root), with no wildcards. */
    data class Name(override val line: String) : IgnoreRule

    /** `// note`. Hidden in the simple editor, kept when saving. */
    data class Comment(override val line: String) : IgnoreRule

    /** Wildcards, `!` exceptions, `(?i)`/`(?d)` prefixes, `#include`… */
    data class Advanced(override val line: String) : IgnoreRule
}

/** Ready-made groups of patterns the simple editor offers as one-tap switches. */
enum class IgnorePreset(val lines: List<String>) {
    TEMPORARY_FILES(listOf("*.tmp", "*.temp", "~$*")),
    SYSTEM_FILES(listOf(".DS_Store", "Thumbs.db", "desktop.ini")),
    ANDROID_LEFTOVERS(listOf(".thumbnails", ".trashed-*")),
    PROGRAMMING_FOLDERS(listOf("node_modules", ".gradle", "__pycache__")),
}

/**
 * Pure helpers over a folder's ignore lines. Syncthing matches these as glob patterns (not regular
 * expressions): see https://docs.syncthing.net/users/ignoring.html.
 */
object IgnoreRules {
    private val wildcards = charArrayOf('*', '?', '[', ']', '{', '}', '\\')
    private val extensionForbidden = charArrayOf('*', '?', '[', ']', '{', '}', '\\', '/', '!', ' ')

    fun classify(line: String): IgnoreRule = when {
        line.startsWith("//") -> IgnoreRule.Comment(line)
        line.startsWith("!") || line.startsWith("#") || line.startsWith("(?") -> IgnoreRule.Advanced(line)
        isFileType(line) -> IgnoreRule.FileType(line.removePrefix("*."))
        line.none { it in wildcards } -> IgnoreRule.Name(line)
        else -> IgnoreRule.Advanced(line)
    }

    private fun isFileType(line: String): Boolean =
        line.startsWith("*.") && line.length > 2 && line.drop(2).none { it in extensionForbidden }

    /** The rules to show: blank lines are dropped, comments are hidden by callers that want to. */
    fun parse(lines: List<String>): List<IgnoreRule> =
        lines.filter { it.isNotBlank() }.map(::classify)

    /** Trims user input into one rule line; null when there is nothing usable to add. */
    fun normalize(input: String): String? =
        input.trim().takeIf { it.isNotEmpty() && '\n' !in it && '\r' !in it }

    /** Adds [line] unless an identical line is already there. */
    fun add(lines: List<String>, line: String): List<String> = if (line in lines) lines else lines + line

    fun remove(lines: List<String>, line: String): List<String> = lines.filterNot { it == line }

    fun isActive(lines: List<String>, preset: IgnorePreset): Boolean = preset.lines.all { it in lines }

    /** Turns the whole [preset] off if it is fully on, otherwise on (adding only the missing lines). */
    fun toggle(lines: List<String>, preset: IgnorePreset): List<String> =
        if (isActive(lines, preset)) lines.filterNot { it in preset.lines } else lines + preset.lines.filterNot { it in lines }

    /** The text of the advanced editor: one rule per line, like `.stignore` itself. */
    fun toText(lines: List<String>): String = lines.joinToString("\n")

    fun fromText(text: String): List<String> =
        text.split('\n').map { it.trimEnd('\r') }.dropLastWhile { it.isEmpty() }
}
