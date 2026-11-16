package com.adamfoerster.meshsync.core.domain.preferences

/** Facts about the running app build, provided by each platform host (`app.version` in gradle.properties). */
interface AppInfo {
    /** SemVer of this MeshSync build, or empty when the host cannot tell. */
    val version: String
}
