package com.adamfoerster.syncthing.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.adamfoerster.syncthing.core.designsystem.resources.Res
import com.adamfoerster.syncthing.core.designsystem.resources.chakrapetch_bold
import com.adamfoerster.syncthing.core.designsystem.resources.chakrapetch_medium
import com.adamfoerster.syncthing.core.designsystem.resources.chakrapetch_semibold
import com.adamfoerster.syncthing.core.designsystem.resources.ibmplexmono_medium
import com.adamfoerster.syncthing.core.designsystem.resources.ibmplexmono_regular
import com.adamfoerster.syncthing.core.designsystem.resources.ibmplexsans_medium
import com.adamfoerster.syncthing.core.designsystem.resources.ibmplexsans_regular
import com.adamfoerster.syncthing.core.designsystem.resources.ibmplexsans_semibold
import org.jetbrains.compose.resources.Font

@Immutable
data class NgTypography(
    val display: FontFamily = FontFamily.Default,
    val sans: FontFamily = FontFamily.Default,
    val mono: FontFamily = FontFamily.Monospace,
) {
    /** `.top h1` */
    val title = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)

    /** `.h2` */
    val h2 = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)

    /** `.h3` */
    val h3 = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)

    /** `.sub` */
    val sub = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)

    /** `.lbl` — uppercase section label. */
    val label = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em,
    )
    val tab = TextStyle(
        fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em,
    )
    val button = TextStyle(
        fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 14.sp, letterSpacing = 0.08.em,
    )

    /** `.badge` and `.st` */
    val tag = TextStyle(
        fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em,
    )
    val monoSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val mono14 = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)

    /** `.code` — diff and editor lines. */
    val code = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp)
    val lineNumber = TextStyle(fontFamily = mono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 20.sp)
    val chip = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val segment = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
    val node = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp)
}

val LocalNgTypography = staticCompositionLocalOf { NgTypography() }

@Composable
fun rememberNgTypography(): NgTypography {
    val display = FontFamily(
        Font(Res.font.chakrapetch_medium, FontWeight.Medium),
        Font(Res.font.chakrapetch_semibold, FontWeight.SemiBold),
        Font(Res.font.chakrapetch_bold, FontWeight.Bold),
    )
    val sans = FontFamily(
        Font(Res.font.ibmplexsans_regular, FontWeight.Normal),
        Font(Res.font.ibmplexsans_medium, FontWeight.Medium),
        Font(Res.font.ibmplexsans_semibold, FontWeight.SemiBold),
    )
    val mono = FontFamily(
        Font(Res.font.ibmplexmono_regular, FontWeight.Normal),
        Font(Res.font.ibmplexmono_medium, FontWeight.Medium),
    )
    return NgTypography(display = display, sans = sans, mono = mono)
}
