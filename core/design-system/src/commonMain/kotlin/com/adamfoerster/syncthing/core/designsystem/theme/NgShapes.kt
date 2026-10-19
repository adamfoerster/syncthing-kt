package com.adamfoerster.syncthing.core.designsystem.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.unit.dp

/** The chamfered corners that give the grid look its edge. */
object NgShapes {
    /** `.panel`: top-right and bottom-left cut. */
    val panel = CutCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 0.dp, bottomStart = 10.dp)

    /** `.btn`: bottom-right cut. */
    val button = CutCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 6.dp, bottomStart = 0.dp)

    /** `.av` and `.tile`: top-right cut. */
    val avatar = CutCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 0.dp)
}
