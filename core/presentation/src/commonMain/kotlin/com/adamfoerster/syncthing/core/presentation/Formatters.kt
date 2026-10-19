package com.adamfoerster.syncthing.core.presentation

import com.adamfoerster.syncthing.core.presentation.resources.Res
import com.adamfoerster.syncthing.core.presentation.resources.time_days_ago
import com.adamfoerster.syncthing.core.presentation.resources.time_hours_ago
import com.adamfoerster.syncthing.core.presentation.resources.time_just_now
import com.adamfoerster.syncthing.core.presentation.resources.time_minutes_ago
import com.adamfoerster.syncthing.core.presentation.resources.time_today_at
import com.adamfoerster.syncthing.core.presentation.resources.time_yesterday_at
import com.adamfoerster.syncthing.core.presentation.resources.weekday_friday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_monday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_saturday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_sunday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_thursday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_tuesday
import com.adamfoerster.syncthing.core.presentation.resources.weekday_wednesday
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** "4,2 GB", "312 MB" — decimal comma, as in the mockups. */
fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble().coerceAtLeast(0.0)
    var unit = 0
    while (value >= 1000 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val text = when {
        unit == 0 || value >= 100 -> value.roundToLong().toString()
        else -> {
            val tenths = (value * 10).roundToLong()
            "${tenths / 10},${tenths % 10}"
        }
    }
    return "$text ${units[unit]}"
}

/** "64%" */
fun formatPercent(value: Double): String = "${value.toInt().coerceIn(0, 100)}%"

fun Instant.toLocal(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime = toLocalDateTime(zone)

/** "14:32" */
fun Instant.formatClock(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val t = toLocal(zone)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

/** "há 2 min", "há 3 h", "agora" */
fun Instant.toRelativeUiText(now: Instant = Clock.System.now()): UiText {
    val elapsed = now - this
    val minutes = elapsed.inWholeMinutes
    return when {
        abs(minutes) < 1 -> UiText.Resource(Res.string.time_just_now)
        minutes < 60 -> UiText.Resource(Res.string.time_minutes_ago, minutes)
        minutes < 24 * 60 -> UiText.Resource(Res.string.time_hours_ago, elapsed.inWholeHours)
        else -> UiText.Resource(Res.string.time_days_ago, elapsed.inWholeDays)
    }
}

/** "Hoje, 14:32" / "Ontem, 09:10" / "sábado, 18:00" / "12/10, 18:00" */
fun Instant.toDayAndTimeUiText(now: Instant = Clock.System.now()): UiText {
    val zone = TimeZone.currentSystemDefault()
    val date = toLocal(zone).date
    val today = now.toLocal(zone).date
    val clock = formatClock(zone)
    return when {
        date == today -> UiText.Resource(Res.string.time_today_at, clock)
        date == (now - 1.days).toLocal(zone).date -> UiText.Resource(Res.string.time_yesterday_at, clock)
        now - this < 7.days -> UiText.Resource(weekdays[date.dayOfWeek.ordinal], clock)
        else -> UiText.DynamicString(
            "${date.day.toString().padStart(2, '0')}/${date.month.ordinal.plus(1).toString().padStart(2, '0')}, $clock",
        )
    }
}

private val weekdays = listOf(
    Res.string.weekday_monday,
    Res.string.weekday_tuesday,
    Res.string.weekday_wednesday,
    Res.string.weekday_thursday,
    Res.string.weekday_friday,
    Res.string.weekday_saturday,
    Res.string.weekday_sunday,
)
