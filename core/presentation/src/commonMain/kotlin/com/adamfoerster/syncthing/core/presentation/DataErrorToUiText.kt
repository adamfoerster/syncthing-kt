package com.adamfoerster.syncthing.core.presentation

import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.presentation.resources.Res
import com.adamfoerster.syncthing.core.presentation.resources.error_bad_request
import com.adamfoerster.syncthing.core.presentation.resources.error_core_unavailable
import com.adamfoerster.syncthing.core.presentation.resources.error_file_changed
import com.adamfoerster.syncthing.core.presentation.resources.error_io
import com.adamfoerster.syncthing.core.presentation.resources.error_not_found
import com.adamfoerster.syncthing.core.presentation.resources.error_permission
import com.adamfoerster.syncthing.core.presentation.resources.error_server
import com.adamfoerster.syncthing.core.presentation.resources.error_unauthorized
import com.adamfoerster.syncthing.core.presentation.resources.error_unknown

fun DataError.toUiText(): UiText = UiText.Resource(
    when (this) {
        DataError.Remote.CORE_UNAVAILABLE -> Res.string.error_core_unavailable
        DataError.Remote.UNAUTHORIZED -> Res.string.error_unauthorized
        DataError.Remote.BAD_REQUEST -> Res.string.error_bad_request
        DataError.Remote.SERVER_ERROR -> Res.string.error_server
        DataError.Remote.SERIALIZATION, DataError.Remote.UNKNOWN -> Res.string.error_unknown
        DataError.Local.NOT_FOUND -> Res.string.error_not_found
        DataError.Local.PERMISSION_DENIED -> Res.string.error_permission
        DataError.Local.FILE_CHANGED -> Res.string.error_file_changed
        DataError.Local.IO, DataError.Local.UNKNOWN -> Res.string.error_io
    },
)
