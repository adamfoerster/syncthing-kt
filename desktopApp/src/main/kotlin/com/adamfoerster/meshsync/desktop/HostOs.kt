package com.adamfoerster.meshsync.desktop

/** The desktop OS the app runs on. Passed around instead of reading `os.name` everywhere, so tests can pick one. */
enum class HostOs {
    Windows,
    MacOs,
    Other,
    ;

    companion object {
        fun of(osName: String): HostOs {
            val name = osName.lowercase()
            return when {
                name.contains("windows") -> Windows
                name.contains("mac") || name.contains("darwin") -> MacOs
                else -> Other
            }
        }

        fun current(): HostOs = of(System.getProperty("os.name").orEmpty())
    }
}
