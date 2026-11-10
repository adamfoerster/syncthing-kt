package com.adamfoerster.meshsync.desktop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter

/**
 * The tray/window icon, drawn in code so it needs no image assets yet. Each status has its own glyph
 * (check, ring, pause bars, cross, exclamation) on top of its colour. These colours belong to the OS
 * tray, outside the app theme, so they are fixed on purpose.
 */
class StatusIcon(private val status: TrayStatus) : Painter() {
    override val intrinsicSize: Size = Size(SIZE, SIZE)

    override fun DrawScope.onDraw() {
        val s = size.minDimension
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        val stroke = s * 0.11f

        drawCircle(color(status), radius = s / 2f)
        when (status) {
            TrayStatus.Running -> {
                drawLine(Color.White, p(0.27f, 0.53f), p(0.43f, 0.69f), stroke, StrokeCap.Round)
                drawLine(Color.White, p(0.43f, 0.69f), p(0.75f, 0.33f), stroke, StrokeCap.Round)
            }
            TrayStatus.Starting -> drawArc(
                Color.White, startAngle = -90f, sweepAngle = 270f, useCenter = false,
                topLeft = p(0.27f, 0.27f), size = Size(s * 0.46f, s * 0.46f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            TrayStatus.Paused -> {
                drawLine(Color.White, p(0.38f, 0.28f), p(0.38f, 0.72f), stroke * 1.2f, StrokeCap.Round)
                drawLine(Color.White, p(0.62f, 0.28f), p(0.62f, 0.72f), stroke * 1.2f, StrokeCap.Round)
            }
            TrayStatus.Failed -> {
                drawLine(Color.White, p(0.32f, 0.32f), p(0.68f, 0.68f), stroke, StrokeCap.Round)
                drawLine(Color.White, p(0.68f, 0.32f), p(0.32f, 0.68f), stroke, StrokeCap.Round)
            }
            TrayStatus.Missing -> {
                drawLine(Color.White, p(0.5f, 0.26f), p(0.5f, 0.55f), stroke * 1.2f, StrokeCap.Round)
                drawCircle(Color.White, radius = stroke * 0.7f, center = p(0.5f, 0.73f))
            }
        }
    }

    private fun color(status: TrayStatus) = when (status) {
        TrayStatus.Running -> Color(0xFF2E9E5B)
        TrayStatus.Starting -> Color(0xFF2F7DD1)
        TrayStatus.Paused -> Color(0xFF6F757D)
        TrayStatus.Failed -> Color(0xFFD1453B)
        TrayStatus.Missing -> Color(0xFFC77E12)
    }

    private companion object {
        const val SIZE = 64f
    }
}
