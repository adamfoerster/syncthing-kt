package com.adamfoerster.syncthing.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** "Neon Grid" tokens from the redesign. The app is dark-only, like the mockups. */
@Immutable
data class NgColors(
    val bg: Color = Color(0xFF080A0F),
    val panel: Color = Color(0xFF10151D),
    val raised: Color = Color(0xFF171F2A),
    val hover: Color = Color(0xFF1D2835),
    val active: Color = Color(0xFF243242),
    val line: Color = Color(0xFF293746),
    val lineStrong: Color = Color(0xFF5E7388),
    val text: Color = Color(0xFFE8F1F8),
    val text2: Color = Color(0xFF91A2B4),
    val cyan: Color = Color(0xFF00F0FF),
    val cyanHover: Color = Color(0xFF5CF6FF),
    val cyanEdge: Color = Color(0xFF0B5C66),
    val cyanSub: Color = Color(0xFF082A31),
    val magenta: Color = Color(0xFFFF2A6D),
    val magentaSub: Color = Color(0xFF35101F),
    val yellow: Color = Color(0xFFFCEE0A),
    val ok: Color = Color(0xFF39FF88),
    val okSub: Color = Color(0xFF0C2B1B),
    val warn: Color = Color(0xFFFFB000),
    val warnSub: Color = Color(0xFF2F2303),
    val err: Color = Color(0xFFFF4560),
    val errSub: Color = Color(0xFF35121B),
    val info: Color = Color(0xFF5CB8FF),
    val infoSub: Color = Color(0xFF0D2437),
    val disabledBg: Color = Color(0xFF141A22),
    val disabledText: Color = Color(0xFF7D8A99),
) {
    /** Diff rows: "ours" in green, "theirs" in blue, as in the text resolver mockup. */
    val oursLine: Color get() = ok.copy(alpha = 0.10f)
    val oursHighlight: Color get() = ok.copy(alpha = 0.30f)
    val theirsLine: Color get() = info.copy(alpha = 0.10f)
    val theirsHighlight: Color get() = info.copy(alpha = 0.30f)
}

val LocalNgColors = staticCompositionLocalOf { NgColors() }
