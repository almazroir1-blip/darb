package app.darb.theme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.darb.theme.data.PrayerTimes
import app.darb.theme.data.ThemeSettings
import app.darb.theme.data.VehicleState
import app.darb.theme.data.WIDGETS
import app.darb.theme.data.Weather
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZonedDateTime

/* ======================= مكونات عامة ======================= */

@Composable
fun DarbCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.card)
    Column(
        modifier
            .clip(shape)
            .background(t.p.card)
            .border(1.dp, t.p.line, shape)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun CardHeader(title: String, note: String? = null) {
    val t = LocalDarb.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = t.p.muted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font)
        if (note != null) Text(note, color = t.p.muted.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = t.p.font)
    }
}

@Composable
fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    val t = LocalDarb.current
    Column(
        modifier.clip(RoundedCornerShape(t.shape.inner)).background(t.p.card2).padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = t.p.muted, fontSize = 12.sp, fontFamily = t.p.font)
        Text(value, color = t.p.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmallButton(text: String, filled: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.inner)
    Box(
        modifier
            .clip(shape)
            .background(if (filled) t.accent else t.p.card)
            .border(1.dp, if (filled) t.accent else t.p.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font,
            color = if (filled) t.accent.onColor() else t.p.fg
        )
    }
}

/** بطاقة يمكن إخفاؤها وإظهارها في وضع التعديل. */
@Composable
fun WidgetSlot(
    id: String, s: ThemeSettings, editing: Boolean, onToggle: (String) -> Unit,
    modifier: Modifier, content: @Composable ColumnScope.() -> Unit
) {
    val t = LocalDarb.current
    val hidden = id in s.hidden
    if (hidden && !editing) return
    val name = WIDGETS.firstOrNull { it.first == id }?.second ?: id
    if (hidden) {
        Box(
            modifier.clip(RoundedCornerShape(t.shape.card)).border(2.dp, t.p.line, RoundedCornerShape(t.shape.card)),
            contentAlignment = Alignment.Center
        ) { SmallButton("إظهار $name", filled = false, onClick = { onToggle(id) }) }
        return
    }
    Box(modifier) {
        DarbCard(Modifier.fillMaxSize(), content)
        if (editing) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(t.shape.inner))
                    .background(t.p.card)
                    .border(1.dp, t.accent, RoundedCornerShape(t.shape.inner))
                    .clickable { onToggle(id) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text("إخفاء", color = t.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font) }
        }
    }
}

/* ======================= الشريط العلوي ======================= */

@Composable
fun TopBar(modeLabel: String, editing: Boolean, onEdit: () -> Unit, onCustomize: () -> Unit) {
    val t = LocalDarb.current
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            value = ZonedDateTime.now()
            delay(1000)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(hourMinute(now), color = t.p.fg, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
            Text(period(now), color = t.p.muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
            Text(hijriDate(now), color = t.p.muted, fontSize = 18.sp, fontFamily = t.p.font, modifier = Modifier.padding(start = 6.dp, bottom = 4.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .clip(RoundedCornerShape(t.shape.inner))
                .background(t.p.card)
                .border(1.dp, t.p.line, RoundedCornerShape(t.shape.inner))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(t.accent))
            Text(
                if (editing) "وضع التعديل: اختر ما يظهر على الشاشة" else "وضع القيادة · $modeLabel",
                color = t.p.fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font
            )
        }
        Spacer(Modifier.weight(1f))
        SmallButton("تخصيص", onClick = onCustomize)
        SmallButton(if (editing) "تم" else "تعديل", filled = editing, onClick = onEdit)
    }
}

/* ======================= الشاشة الرئيسية ======================= */

@Composable
fun HomeContent(
    s: ThemeSettings, v: VehicleState, weather: Weather?, loc: Pair<Double, Double>,
    editing: Boolean, onToggle: (String) -> Unit
) {
    val t = LocalDarb.current
    val style = s.gauge.resolve(s.mode)
    val weatherVisible = "weather" !in s.hidden
    val prayerVisible = "prayer" !in s.hidden
    val allHidden = WIDGETS.all { it.first in s.hidden }
    if (allHidden && !editing) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("كل البطاقات مخفية. اضغط «تعديل» لإظهارها.", color = t.p.muted, fontSize = 22.sp, fontFamily = t.p.font)
        }
        return
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        WidgetSlot("speed", s, editing, onToggle, Modifier.weight(1.2f).fillMaxHeight()) {
            SpeedCardContent(style, v, s.mode)
        }
        WidgetSlot("car", s, editing, onToggle, Modifier.weight(1f).fillMaxHeight()) {
            CarCardContent()
        }
        if (editing || weatherVisible || prayerVisible) {
            Column(Modifier.weight(0.95f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                WidgetSlot(
                    "weather", s, editing, onToggle,
                    if (prayerVisible || editing) Modifier.height(150.dp).fillMaxWidth() else Modifier.weight(1f).fillMaxWidth()
                ) { WeatherCardContent(weather) }
                WidgetSlot("prayer", s, editing, onToggle, Modifier.weight(1f).fillMaxWidth()) {
                    PrayerCardContent(loc.first, loc.second)
                }
            }
        }
    }
}

/* ---- العداد ---- */

@Composable
fun SpeedCard(style: GaugeStyle, v: VehicleState, mode: DriveMode, modifier: Modifier = Modifier) {
    DarbCard(modifier) { SpeedCardContent(style, v, mode) }
}

@Composable
fun ColumnScope.SpeedCardContent(style: GaugeStyle, v: VehicleState, mode: DriveMode) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        SpeedGauge(style, v.speedKmh, v.rpm, Modifier.fillMaxHeight())
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCell("الغيار", v.gear?.let { "D$it" } ?: "—", Modifier.weight(1f))
        if (mode == DriveMode.ECONOMY) {
            StatCell("الاستهلاك", "—", Modifier.weight(1f))
        } else {
            StatCell("الدورات", v.rpm?.let { oneDecimal(it / 1000f) + "k" } ?: "—", Modifier.weight(1f))
        }
        StatCell("حرارة المحرك", "—", Modifier.weight(1f))
    }
}

/* ---- السيارة ---- */

@Composable
fun ColumnScope.CarCardContent() {
    val t = LocalDarb.current
    CardHeader("سيارتي", "بيانات تجريبية")
    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 36.dp, bottom = 26.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Tyre("2.4"); Tyre("2.4")
        }
        Canvas(Modifier.width(170.dp).fillMaxHeight()) { drawCar(t) }
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = 36.dp, bottom = 26.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Tyre("2.5"); Tyre("2.5")
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("الوقود", color = t.p.muted, fontSize = 14.sp, fontFamily = t.p.font)
        Text("68%", color = t.p.fg, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
    }
    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(8.dp).clip(CircleShape).background(t.p.track)) {
        Box(Modifier.fillMaxWidth(0.68f).fillMaxHeight().clip(CircleShape).background(t.accent))
    }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCell("المدى", "512 كم", Modifier.weight(1f))
        StatCell("العداد", "128,450", Modifier.weight(1f))
    }
}

@Composable
private fun Tyre(value: String) {
    val t = LocalDarb.current
    Column(
        Modifier.width(62.dp).clip(RoundedCornerShape(t.shape.inner)).background(t.p.card2).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = t.p.fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
        Text("بار", color = t.p.muted, fontSize = 11.sp)
    }
}

private fun DrawScope.drawCar(t: DarbTheme) {
    val s = minOf(size.width / 200f, size.height / 400f)
    val ox = (size.width - 200f * s) / 2f
    val oy = (size.height - 400f * s) / 2f
    fun x(v: Float) = ox + v * s
    fun y(v: Float) = oy + v * s
    val p = t.p
    val edge = Color.White.copy(alpha = 0.16f)

    drawOval(t.accent.copy(alpha = 0.16f), Offset(x(4f), y(10f)), Size(192f * s, 380f * s), style = Stroke(1.5f))
    drawOval(t.accent.copy(alpha = 0.10f), Offset(x(22f), y(24f)), Size(156f * s, 352f * s), style = Stroke(1.5f))

    val wheel = Color(0xFF0B0B0C)
    drawRoundRect(wheel, Offset(x(28f), y(62f)), Size(16f * s, 50f * s), CornerRadius(5f * s))
    drawRoundRect(wheel, Offset(x(156f), y(62f)), Size(16f * s, 50f * s), CornerRadius(5f * s))
    drawRoundRect(wheel, Offset(x(26f), y(290f)), Size(18f * s, 54f * s), CornerRadius(5f * s))
    drawRoundRect(wheel, Offset(x(156f), y(290f)), Size(18f * s, 54f * s), CornerRadius(5f * s))

    val body = Path().apply {
        moveTo(x(100f), y(14f))
        cubicTo(x(138f), y(14f), x(150f), y(40f), x(152f), y(80f))
        lineTo(x(156f), y(150f)); lineTo(x(156f), y(290f)); lineTo(x(152f), y(345f))
        cubicTo(x(150f), y(380f), x(132f), y(392f), x(100f), y(392f))
        cubicTo(x(68f), y(392f), x(50f), y(380f), x(48f), y(345f))
        lineTo(x(44f), y(290f)); lineTo(x(44f), y(150f)); lineTo(x(48f), y(80f))
        cubicTo(x(50f), y(40f), x(62f), y(14f), x(100f), y(14f))
        close()
    }
    drawPath(body, p.carBody)
    drawPath(body, edge, style = Stroke(1.5f))

    drawRoundRect(p.carBody, Offset(x(38f), y(122f)), Size(12f * s, 9f * s), CornerRadius(3f * s))
    drawRoundRect(p.carBody, Offset(x(150f), y(122f)), Size(12f * s, 9f * s), CornerRadius(3f * s))

    val windshield = Path().apply {
        moveTo(x(66f), y(120f)); lineTo(x(134f), y(120f)); lineTo(x(142f), y(160f)); lineTo(x(58f), y(160f)); close()
    }
    drawPath(windshield, p.carGlass)
    drawRoundRect(p.carBody, Offset(x(58f), y(160f)), Size(84f * s, 92f * s), CornerRadius(10f * s))
    drawRoundRect(edge, Offset(x(58f), y(160f)), Size(84f * s, 92f * s), CornerRadius(10f * s), style = Stroke(1.5f))
    val rear = Path().apply {
        moveTo(x(58f), y(252f)); lineTo(x(142f), y(252f)); lineTo(x(134f), y(286f)); lineTo(x(66f), y(286f)); close()
    }
    drawPath(rear, p.carGlass)

    drawLine(t.accent, Offset(x(92f), y(28f)), Offset(x(92f), y(112f)), 2f * s, StrokeCap.Round)
    drawLine(t.accent, Offset(x(108f), y(28f)), Offset(x(108f), y(112f)), 2f * s, StrokeCap.Round)

    val head = Color(0xFFF4F6FF)
    drawLine(head, Offset(x(56f), y(46f)), Offset(x(82f), y(34f)), 4f * s, StrokeCap.Round)
    drawLine(head, Offset(x(144f), y(46f)), Offset(x(118f), y(34f)), 4f * s, StrokeCap.Round)
    val tail = Color(0xFFE23B3B)
    drawLine(tail, Offset(x(56f), y(372f)), Offset(x(84f), y(378f)), 4f * s, StrokeCap.Round)
    drawLine(tail, Offset(x(144f), y(372f)), Offset(x(116f), y(378f)), 4f * s, StrokeCap.Round)
}

/* ---- الطقس ---- */

@Composable
fun ColumnScope.WeatherCardContent(w: Weather?) {
    val t = LocalDarb.current
    CardHeader("الطقس", if (w == null) "تعذر التحديث" else null)
    Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SunIcon(t.p.acc2, Modifier.size(50.dp))
        Text(w?.let { "${it.temp}°" } ?: "—", color = t.p.fg, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
        Column {
            Text(w?.text ?: "—", color = t.p.fg, fontSize = 21.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
            if (w != null) Text("العليا ${w.hi}° · الدنيا ${w.lo}°", color = t.p.muted, fontSize = 13.sp, fontFamily = t.p.font)
        }
    }
    if (w != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip("رطوبة ${w.humidity}%")
            InfoChip("رياح ${w.windKmh} كم/س")
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    val t = LocalDarb.current
    Text(
        text, color = t.p.muted, fontSize = 13.sp, fontFamily = t.p.font,
        modifier = Modifier.clip(RoundedCornerShape(t.shape.inner)).background(t.p.card2).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
fun SunIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val c = Offset(12f * s, 12f * s)
        drawCircle(color, 4.5f * s, c, style = Stroke(1.8f * s))
        val rays = listOf(
            Offset(12f, 2f) to Offset(12f, 5f), Offset(12f, 19f) to Offset(12f, 22f),
            Offset(2f, 12f) to Offset(5f, 12f), Offset(19f, 12f) to Offset(22f, 12f),
            Offset(4.9f, 4.9f) to Offset(7f, 7f), Offset(17f, 17f) to Offset(19.1f, 19.1f),
            Offset(4.9f, 19.1f) to Offset(7f, 17f), Offset(17f, 7f) to Offset(19.1f, 4.9f)
        )
        for ((a, b) in rays) drawLine(color, Offset(a.x * s, a.y * s), Offset(b.x * s, b.y * s), 1.8f * s, StrokeCap.Round)
    }
}

/* ---- مواقيت الصلاة ---- */

@Composable
fun ColumnScope.PrayerCardContent(lat: Double, lng: Double) {
    val t = LocalDarb.current
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            value = ZonedDateTime.now()
            delay(1000)
        }
    }
    val st = PrayerTimes.state(now, lat, lng)
    val left = Duration.between(now, st.nextPrayer.time).seconds

    CardHeader("مواقيت الصلاة", "أم القرى")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(t.shape.inner))
            .background(t.accent.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("الصلاة القادمة", color = t.p.muted, fontSize = 12.sp, fontFamily = t.p.font)
            Text(st.nextPrayer.name, color = t.p.fg, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
        }
        Text(countdown(left), color = t.accent, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
    }
    Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.SpaceEvenly) {
        st.list.forEachIndexed { i, p ->
            val isNext = i == st.next
            val past = st.tomorrow || i < st.next
            val color = when {
                isNext -> t.accent
                past -> t.p.fg.copy(alpha = 0.42f)
                else -> t.p.fg
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(p.name, color = color, fontSize = 18.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal, fontFamily = t.p.font)
                Text(fmt12(p.time), color = color, fontSize = 18.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium, fontFamily = t.p.font)
            }
        }
    }
}
