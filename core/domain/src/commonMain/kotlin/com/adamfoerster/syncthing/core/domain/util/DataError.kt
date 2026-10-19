package com.adamfoerster.syncthing.core.domain.util

sealed interface DataError : Error {
    enum class Remote : DataError {
        /** The local Syncthing core is not running or not reachable yet. */
        CORE_UNAVAILABLE,
        UNAUTHORIZED,
        BAD_REQUEST,
        SERVER_ERROR,
        SERIALIZATION,
        UNKNOWN,
    }

    enum class Local : DataError {
        NOT_FOUND,
        PERMISSION_DENIED,

        /** A file changed on disk after it was read, so writing would lose that change. */
        FILE_CHANGED,
        IO,
        UNKNOWN,
    }
}
