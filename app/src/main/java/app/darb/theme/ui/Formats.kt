package app.darb.theme.ui

import java.time.ZonedDateTime
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.util.Locale

/** 4:52 ص */
fun fmt12(t: ZonedDateTime): String {
    val h = t.hour
    val hh = if (h % 12 == 0) 12 else h % 12
    return "$hh:${t.minute.toString().padStart(2, '0')} ${if (h < 12) "ص" else "م"}"
}

fun hourMinute(t: ZonedDateTime): String {
    val h = t.hour
    val hh = if (h % 12 == 0) 12 else h % 12
    return "$hh:${t.minute.toString().padStart(2, '0')}"
}

fun period(t: ZonedDateTime): String = if (t.hour < 12) "ص" else "م"

/** 01:12:30 */
fun countdown(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(Locale.US, s / 3600, (s % 3600) / 60, s % 60)
}

/** الاثنين 21 جمادى الأولى (التقويم الهجري، أرقام لاتينية) */
fun hijriDate(now: ZonedDateTime): String = try {
    val h = HijrahDate.from(now.toLocalDate())
    DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("ar"))
        .withChronology(HijrahChronology.INSTANCE)
        .withDecimalStyle(DecimalStyle.STANDARD)
        .format(h)
} catch (e: Exception) {
    ""
}

fun oneDecimal(x: Float): String = String.format(Locale.US, "%.1f", x)
