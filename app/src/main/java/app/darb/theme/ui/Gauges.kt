package app.darb.theme.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * عدادات السرعة الأربعة مرسومة على Canvas.
 * كل الإحداثيات مأخوذة من تصميم 400×400 ثم تُكبَّر بحسب حجم العنصر.
 * الزاوية 0 تعني أعلى الدائرة، والموجب باتجاه عقارب الساعة، والمدى -135 إلى +135.
 */
@Composable
fun SpeedGauge(style: GaugeStyle, speedKmh: Float, rpm: Float?, modifier: Modifier = Modifier) {
    val t = LocalDarb.current
    val v by animateFloatAsState(speedKmh, tween(300, easing = LinearEasing), label = "speed")
    val m = rememberTextMeasurer()
    Canvas(modifier.aspectRatio(1f)) {
        val g = Geo(size)
        when (style) {
            GaugeStyle.LUXURY -> drawLuxury(g, t, m, v)
            GaugeStyle.CLASSIC -> drawClassic(g, t, m, v)
            GaugeStyle.ECONOMY -> drawEconomy(g, t, m, v)
            else -> drawSport(g, t, m, v, rpm)
        }
    }
}

private class Geo(size: Size) {
    val k = size.minDimension / 400f
    val c = Offset(size.width / 2f, size.height / 2f)

    /** نقطة من إحداثيات التصميم (400×400). */
    fun o(x: Float, y: Float) = Offset(c.x + (x - 200f) * k, c.y + (y - 200f) * k)

    /** نقطة على دائرة نصف قطرها r عند الزاوية a. */
    fun pt(r: Float, a: Float): Offset {
        val t = Math.toRadians(a.toDouble())
        return Offset(c.x + (r * k * sin(t)).toFloat(), c.y - (r * k * cos(t)).toFloat())
    }

    fun tl(r: Float) = Offset(c.x - r * k, c.y - r * k)
    fun sz(r: Float) = Size(2f * r * k, 2f * r * k)
}

private fun angleOf(v: Float, max: Float) = -135f + 270f * (v.coerceIn(0f, max) / max)

private fun DrawScope.ctext(
    m: TextMeasurer, text: String, at: Offset, sizePx: Float,
    color: Color, font: FontFamily, weight: FontWeight = FontWeight.Normal
) {
    val r = m.measure(
        AnnotatedString(text),
        TextStyle(color = color, fontSize = sizePx.toSp(), fontFamily = font, fontWeight = weight)
    )
    drawText(r, topLeft = Offset(at.x - r.size.width / 2f, at.y - r.size.height / 2f))
}

/* ---------------- رياضي ---------------- */
private fun DrawScope.drawSport(g: Geo, t: DarbTheme, m: TextMeasurer, v: Float, rpm: Float?) {
    val p = t.p
    val k = g.k
    val max = 280f
    val frac = (v / max).coerceIn(0f, 1f)
    val w = 20f * k

    drawArc(p.track, 135f, 270f, false, g.tl(170f), g.sz(170f), style = Stroke(w))
    rotate(135f, g.c) {
        drawArc(
            brush = Brush.sweepGradient(0f to t.accent, 0.75f to p.acc2, 1f to p.acc2, center = g.c),
            startAngle = 0f, sweepAngle = 270f * frac, useCenter = false,
            topLeft = g.tl(170f), size = g.sz(170f), style = Stroke(w)
        )
    }
    var s = 0
    while (s <= 280) {
        val major = s % 40 == 0
        val a = angleOf(s.toFloat(), max)
        drawLine(
            if (s >= 240) p.acc2 else p.muted,
            g.pt(147f, a), g.pt(if (major) 131f else 139f, a),
            strokeWidth = (if (major) 2.4f else 1.4f) * k
        )
        if (major) ctext(m, "$s", g.pt(112f, a), 17f * k, if (s >= 240) p.acc2 else p.muted, p.font, FontWeight.SemiBold)
        s += 20
    }
    val tip = g.pt(170f, angleOf(v, max))
    drawCircle(Color.White, 9f * k, tip)
    drawCircle(t.accent, 9f * k, tip, style = Stroke(4f * k))

    ctext(m, v.roundToInt().toString(), g.o(200f, 212f), 116f * k, p.fg, p.font, FontWeight.Bold)
    ctext(m, "كم/س", g.o(200f, 270f), 20f * k, p.muted, p.font)

    if (rpm != null) {
        val on = (rpm.coerceIn(0f, 8000f) / 8000f * 28f).roundToInt()
        for (i in 0 until 28) {
            val color = when {
                i >= on -> p.track
                i >= 22 -> p.bad
                else -> t.accent
            }
            drawRoundRect(color, g.o(116f + i * 6f, 294f), Size(4.4f * k, 16f * k), CornerRadius(1f * k))
        }
        ctext(m, "دورات المحرك ×1000", g.o(200f, 334f), 13f * k, p.muted, p.font)
    }
}

/* ---------------- فاخر ---------------- */
private fun DrawScope.drawLuxury(g: Geo, t: DarbTheme, m: TextMeasurer, v: Float) {
    val p = t.p
    val k = g.k
    val max = 240f
    val frac = (v / max).coerceIn(0f, 1f)

    drawCircle(t.accent, 186f * k, g.c, style = Stroke(1.4f * k))
    drawCircle(t.accent, 178f * k, g.c, style = Stroke(0.6f * k))
    drawArc(p.track, 135f, 270f, false, g.tl(168f), g.sz(168f), style = Stroke(2f * k))
    drawArc(t.accent, 135f, 270f * frac, false, g.tl(168f), g.sz(168f), style = Stroke(4f * k, cap = StrokeCap.Round))

    var s = 0
    while (s <= 240) {
        val a = angleOf(s.toFloat(), max)
        val major = s % 20 == 0
        drawLine(
            t.accent.copy(alpha = 0.85f), g.pt(160f, a), g.pt(if (major) 146f else 153f, a),
            strokeWidth = (if (major) 1.6f else 0.8f) * k
        )
        if (s % 40 == 0) ctext(m, "$s", g.pt(122f, a), 27f * k, p.fg, p.font, FontWeight.Medium)
        s += 10
    }
    ctext(m, "DARB", g.o(200f, 104f), 13f * k, t.accent, FontFamily.Serif, FontWeight.SemiBold)

    rotate(angleOf(v, max), g.c) {
        val needle = Path().apply {
            moveTo(g.c.x, g.c.y - 162f * k)
            lineTo(g.c.x + 5f * k, g.c.y)
            lineTo(g.c.x, g.c.y + 24f * k)
            lineTo(g.c.x - 5f * k, g.c.y)
            close()
        }
        drawPath(needle, p.acc2)
    }
    drawCircle(p.card, 15f * k, g.c)
    drawCircle(t.accent, 15f * k, g.c, style = Stroke(2f * k))
    drawCircle(t.accent, 4f * k, g.c)

    ctext(m, v.roundToInt().toString(), g.o(200f, 292f), 60f * k, p.fg, p.font, FontWeight.SemiBold)
    ctext(m, "كم/س", g.o(200f, 326f), 19f * k, p.muted, p.font)
}

/* ---------------- كلاسيكي ---------------- */
private fun DrawScope.drawClassic(g: Geo, t: DarbTheme, m: TextMeasurer, v: Float) {
    val k = g.k
    val max = 200f
    val dark = Color(0xFF2B2014)
    val red = Color(0xFFB3261E)
    val serif = FontFamily.Serif

    drawCircle(
        brush = Brush.sweepGradient(
            listOf(Color(0xFFF4F4F1), Color(0xFF9C9A93), Color(0xFFE9E8E2), Color(0xFF6C6A64), Color(0xFFF4F4F1)),
            center = g.c
        ),
        radius = 192f * k, center = g.c
    )
    drawCircle(Color(0xFF1A1510), 180f * k, g.c)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFF6EDD8), Color(0xFFDCCBA3)),
            center = Offset(g.c.x, g.c.y - 16f * k), radius = 172f * k
        ),
        radius = 172f * k, center = g.c
    )

    val redStart = angleOf(160f, max)
    drawArc(red, redStart - 90f, 135f - redStart, false, g.tl(160f), g.sz(160f), style = Stroke(8f * k))

    var s = 0
    while (s <= 200) {
        val a = angleOf(s.toFloat(), max)
        val major = s % 20 == 0
        drawLine(dark, g.pt(166f, a), g.pt(if (major) 144f else 154f, a), strokeWidth = (if (major) 3f else 1.6f) * k)
        if (major) ctext(m, "$s", g.pt(122f, a), 26f * k, dark, serif, FontWeight.Bold)
        s += 10
    }
    ctext(m, "KM/H", g.o(200f, 150f), 17f * k, Color(0xFF5A4A33), serif)

    drawRoundRect(Color(0xFF15110C), g.o(146f, 268f), Size(108f * k, 32f * k), CornerRadius(4f * k))
    drawRoundRect(Color(0xFF8A8478), g.o(146f, 268f), Size(108f * k, 32f * k), CornerRadius(4f * k), style = Stroke(1f * k))
    ctext(m, "128450", g.o(200f, 284f), 22f * k, Color(0xFFEFE4C8), serif)
    ctext(m, "المسافة الكلية بالكيلومتر", g.o(200f, 322f), 15f * k, Color(0xFF5A4A33), serif)

    rotate(angleOf(v, max), g.c) {
        val needle = Path().apply {
            moveTo(g.c.x, g.c.y - 154f * k)
            lineTo(g.c.x + 6f * k, g.c.y)
            lineTo(g.c.x, g.c.y + 28f * k)
            lineTo(g.c.x - 6f * k, g.c.y)
            close()
        }
        drawPath(needle, red)
    }
    drawCircle(Color(0xFFC9C7C0), 17f * k, g.c)
    drawCircle(Color(0xFF5C5A54), 17f * k, g.c, style = Stroke(1f * k))
    drawCircle(Color(0xFF1A1510), 5f * k, g.c)
}

/* ---------------- اقتصادي ---------------- */
private fun DrawScope.drawEconomy(g: Geo, t: DarbTheme, m: TextMeasurer, v: Float) {
    val p = t.p
    val k = g.k
    val max = 200f
    val frac = (v / max).coerceIn(0f, 1f)
    val zone = when {
        v < 110f -> p.good
        v < 130f -> p.warn
        else -> p.bad
    }
    val w = 24f * k

    drawArc(p.track, 135f, 270f, false, g.tl(160f), g.sz(160f), style = Stroke(w, cap = StrokeCap.Round))
    if (frac > 0.003f) {
        drawArc(zone, 135f, 270f * frac, false, g.tl(160f), g.sz(160f), style = Stroke(w, cap = StrokeCap.Round))
    }
    for (n in listOf(0, 50, 100, 150, 200)) {
        ctext(m, "$n", g.pt(128f, angleOf(n.toFloat(), max)), 16f * k, p.muted, p.font, FontWeight.Medium)
    }
    val tip = g.pt(160f, angleOf(v, max))
    drawCircle(p.card, 7f * k, tip)
    drawCircle(zone, 7f * k, tip, style = Stroke(4f * k))

    ctext(m, v.roundToInt().toString(), g.o(200f, 192f), 104f * k, p.fg, p.font, FontWeight.SemiBold)
    ctext(m, "كم/س", g.o(200f, 252f), 20f * k, p.muted, p.font)

    // درجة الكفاءة تقديرية من السرعة فقط (الأفضل قرب 80 كم/س)
    val eco = (100f - abs(v - 80f) * 0.38f).coerceIn(40f, 99f).roundToInt()
    drawRoundRect(t.accent.copy(alpha = 0.16f), g.o(124f, 286f), Size(152f * k, 36f * k), CornerRadius(18f * k))
    drawCircle(t.accent, 6f * k, g.o(148f, 304f))
    ctext(m, "كفاءة $eco%", g.o(214f, 304f), 17f * k, p.fg, p.font, FontWeight.SemiBold)
}
