package com.adamfoerster.syncthing.feature.home.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.components.NgIconText
import com.adamfoerster.syncthing.core.designsystem.components.NgStatusLabel
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.formatPercent
import com.adamfoerster.syncthing.core.presentation.label
import com.adamfoerster.syncthing.core.presentation.resources.Res as CoreRes
import com.adamfoerster.syncthing.core.presentation.resources.this_phone
import com.adamfoerster.syncthing.core.presentation.toSyncStatus
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val NodeWidth = 136.dp
private val NodeHeight = 72.dp
private val CenterWidth = 88.dp
private const val MAX_NODES = 6

/**
 * "This phone" in the middle, the other devices around it. Lines say how each is doing:
 * solid green = connected, moving cyan dashes = transferring (with %), grey dashes = offline.
 */
@Composable
fun DeviceMap(devices: List<MapDeviceUi>, modifier: Modifier = Modifier) {
    val c = NgTheme.colors
    val shown = devices.take(MAX_NODES)
    val transition = rememberInfiniteTransition()
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing)),
    )
    val parts = ArrayList<String>(shown.size)
    for (device in shown) parts += "${device.name}: ${device.status.label().asString()}"
    val description = parts.joinToString()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(300.dp)
            .semantics { contentDescription = description },
    ) {
        val width = maxWidth
        val height = maxHeight
        val positions = nodeCenters(shown.size, width, height)

        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            shown.forEachIndexed { index, device ->
                val (x, y) = positions[index]
                val end = Offset(x.toPx(), y.toPx())
                when (device.status) {
                    DeviceStatus.CONNECTED -> drawLine(c.ok, center, end, 2.dp.toPx())
                    DeviceStatus.SYNCING -> drawLine(
                        c.cyan, center, end, 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()), -phase.dp.toPx()),
                    )
                    DeviceStatus.DISCONNECTED, DeviceStatus.PAUSED -> drawLine(
                        c.lineStrong, center, end, 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                    )
                }
            }
        }

        // Progress tags sit halfway along the moving lines.
        shown.forEachIndexed { index, device ->
            val completion = device.completion
            if (device.status == DeviceStatus.SYNCING && completion != null) {
                val (x, y) = positions[index]
                Text(
                    formatPercent(completion),
                    style = NgTheme.type.monoSmall.copy(fontSize = NgTheme.type.tag.fontSize, lineHeight = NgTheme.type.tag.fontSize * 1.6f),
                    color = c.cyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset((width / 2 + x) / 2 - 20.dp, (height / 2 + y) / 2 - 9.dp)
                        .size(40.dp, 18.dp)
                        .background(c.panel),
                )
            }
        }

        shown.forEachIndexed { index, device ->
            val (x, y) = positions[index]
            Column(
                Modifier
                    .offset(x - NodeWidth / 2, y - NodeHeight / 2)
                    .size(NodeWidth, NodeHeight)
                    .background(c.raised)
                    .border(1.dp, c.lineStrong)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(device.name, style = NgTheme.type.node, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                NgStatusLabel(device.status.toSyncStatus(), device.status.label().asString())
            }
        }

        Column(
            Modifier
                .offset(width / 2 - CenterWidth / 2, height / 2 - NodeHeight / 2)
                .size(CenterWidth, NodeHeight)
                .background(c.cyanSub)
                .border(1.dp, c.yellow)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            Icon(NgIcons.Phone, null, tint = c.cyan, modifier = Modifier.size(20.dp))
            Text(stringResource(CoreRes.string.this_phone), style = NgTheme.type.chip, color = c.text, textAlign = TextAlign.Center)
        }
    }
}

/** Evenly around an ellipse, starting top-left so three devices land like the mockup. */
private fun nodeCenters(count: Int, width: Dp, height: Dp): List<Pair<Dp, Dp>> {
    if (count == 0) return emptyList()
    val rx = (width - NodeWidth) / 2
    val ry = (height - NodeHeight) / 2
    val start = -90.0 - 180.0 / count
    return List(count) { i ->
        val angle = (start + i * 360.0 / count) * PI / 180
        // A squarish curve (|t|^0.5) pushes nodes toward the corners, clear of the center box.
        (width / 2 + rx * cos(angle).squarish()) to (height / 2 + ry * sin(angle).squarish())
    }
}

private fun Double.squarish(): Float = (kotlin.math.sign(this) * kotlin.math.sqrt(kotlin.math.abs(this))).toFloat()

/** Shape-coded legend under the map. */
@Composable
fun MapLegend(modifier: Modifier = Modifier) {
    val c = NgTheme.colors
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(DeviceStatus.CONNECTED, DeviceStatus.SYNCING, DeviceStatus.DISCONNECTED).forEach { status ->
            NgIconText(
                icon = when (status) {
                    DeviceStatus.CONNECTED -> NgIcons.StatusSquare
                    DeviceStatus.SYNCING -> NgIcons.StatusDiamond
                    else -> NgIcons.StatusTriangle
                },
                text = status.label().asString(),
                tint = when (status) {
                    DeviceStatus.CONNECTED -> c.ok
                    DeviceStatus.SYNCING -> c.cyan
                    else -> c.warn
                },
            )
        }
    }
}
