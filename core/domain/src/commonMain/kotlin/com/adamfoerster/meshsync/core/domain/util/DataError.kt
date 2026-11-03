package com.adamfoerster.meshsync.core.domain.util

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

        /** A file or folder name that is empty or contains a path separator. */
        INVALID_NAME,

        /** Something with that name already exists, so it was not overwritten. */
        ALREADY_EXISTS,

        /** A folder cannot be copied or moved into itself. */
        INTO_ITSELF,
        UNKNOWN,
    }
}
