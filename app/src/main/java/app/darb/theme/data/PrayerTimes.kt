package app.darb.theme.data

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * حساب مواقيت الصلاة بطريقة أم القرى (الفجر 18.5°، العشاء بعد المغرب بـ 90 دقيقة).
 * الحساب فلكي ويعمل دون إنترنت. الدقة في حدود دقيقة إلى دقيقتين.
 */
object PrayerTimes {

    data class Prayer(val name: String, val time: ZonedDateTime)

    data class State(val list: List<Prayer>, val next: Int, val tomorrow: Boolean) {
        val nextPrayer: Prayer get() = list[next]
    }

    private val NAMES = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    /** ساعات اليوم العشرية (بتوقيت المنطقة) للصلوات الخمس بالترتيب. */
    private fun hours(date: LocalDate, zone: ZoneId, lat: Double, lng: Double): DoubleArray {
        val tz = zone.rules.getOffset(date.atTime(12, 0).atZone(zone).toInstant()).totalSeconds / 3600.0
        var y = date.year
        var m = date.monthValue
        val dd = date.dayOfMonth
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + dd + b - 1524.5 - lng / 360.0
        val d = jd - 2451545.0

        fun fix(x: Double) = x - 360.0 * floor(x / 360.0)
        val g = fix(357.529 + 0.98560028 * d)
        val q = fix(280.459 + 0.98564736 * d)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)))
        val e = 23.439 - 0.00000036 * d

        var ra = deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0
        ra = ((ra % 24.0) + 24.0) % 24.0
        var eqt = q / 15.0 - ra
        if (eqt > 12) eqt -= 24
        if (eqt < -12) eqt += 24
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        val noon = 12.0 - eqt + (tz - lng / 15.0)

        fun t(angle: Double, before: Boolean): Double {
            val c = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat)))
            val v = deg(acos(c.coerceIn(-1.0, 1.0))) / 15.0
            return noon + if (before) -v else v
        }

        val asrAngle = -deg(atan(1.0 / (1.0 + tan(rad(abs(lat - decl))))))
        val maghrib = t(0.833, false)
        return doubleArrayOf(t(18.5, true), noon + 1.0 / 60.0, t(asrAngle, false), maghrib, maghrib + 1.5)
    }

    fun compute(date: LocalDate, zone: ZoneId, lat: Double, lng: Double): List<Prayer> {
        val h = hours(date, zone, lat, lng)
        val start = date.atStartOfDay(zone)
        return NAMES.mapIndexed { i, n -> Prayer(n, start.plusSeconds((h[i] * 3600).toLong())) }
    }

    /** الصلاة القادمة. بعد العشاء يصبح فجر الغد هو القادم. */
    fun state(now: ZonedDateTime, lat: Double, lng: Double): State {
        val today = compute(now.toLocalDate(), now.zone, lat, lng)
        val i = today.indexOfFirst { it.time.isAfter(now) }
        if (i >= 0) return State(today, i, false)
        val tomorrow = compute(now.toLocalDate().plusDays(1), now.zone, lat, lng)
        return State(listOf(tomorrow[0]) + today.drop(1), 0, true)
    }
}
