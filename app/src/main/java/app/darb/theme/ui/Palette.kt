package app.darb.theme.ui

import android.graphics.Typeface
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** أوضاع القيادة الأربعة. */
enum class DriveMode(val label: String, val sub: String) {
    SPORT("رياضي", "حاد وسريع الاستجابة"),
    LUXURY("فاخر", "هادئ وراقٍ"),
    CLASSIC("كلاسيكي", "عدادات تناظرية"),
    ECONOMY("اقتصادي", "توفير الوقود")
}

/** شكل البطاقات. */
enum class CardShape(val label: String, val card: Dp, val inner: Dp) {
    SHARP("حاد", 4.dp, 3.dp),
    SOFT("ناعم", 14.dp, 10.dp),
    ROUND("مستدير", 26.dp, 16.dp),
    PILL("كبسولة", 46.dp, 22.dp)
}

/** شكل العداد. AUTO يعني نفس شكل الوضع الحالي. */
enum class GaugeStyle(val label: String) {
    AUTO("حسب الوضع"),
    SPORT("رياضي"),
    LUXURY("فاخر"),
    CLASSIC("كلاسيكي"),
    ECONOMY("اقتصادي")
}

fun GaugeStyle.resolve(mode: DriveMode): GaugeStyle = when (this) {
    GaugeStyle.AUTO -> when (mode) {
        DriveMode.SPORT -> GaugeStyle.SPORT
        DriveMode.LUXURY -> GaugeStyle.LUXURY
        DriveMode.CLASSIC -> GaugeStyle.CLASSIC
        DriveMode.ECONOMY -> GaugeStyle.ECONOMY
    }
    else -> this
}

data class Palette(
    val bgTop: Color,
    val bgBottom: Color,
    val card: Color,
    val card2: Color,
    val line: Color,
    val fg: Color,
    val muted: Color,
    val track: Color,
    val acc2: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
    val carBody: Color,
    val carGlass: Color,
    val defaultAccent: Color,
    val defaultShape: CardShape,
    val font: FontFamily,
    val light: Boolean
)

private val Condensed = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))

fun paletteFor(mode: DriveMode): Palette = when (mode) {
    DriveMode.SPORT -> Palette(
        bgTop = Color(0xFF0A0B0F), bgBottom = Color(0xFF141720),
        card = Color(0xFF14161C), card2 = Color(0x0FFFFFFF), line = Color(0xFF272C37),
        fg = Color(0xFFF3F5F9), muted = Color(0xFF8D96A8), track = Color(0xFF2A303C),
        acc2 = Color(0xFFFFB020), good = Color(0xFF35D07F), warn = Color(0xFFFFB020), bad = Color(0xFFFF4D4D),
        carBody = Color(0xFF23262E), carGlass = Color(0xFF080A0E),
        defaultAccent = Color(0xFFFF4D2E), defaultShape = CardShape.SHARP,
        font = Condensed, light = false
    )
    DriveMode.LUXURY -> Palette(
        bgTop = Color(0xFF0A0F0D), bgBottom = Color(0xFF101713),
        card = Color(0xFF0F1512), card2 = Color(0x14C8A45C), line = Color(0x55C8A45C),
        fg = Color(0xFFF1EBDD), muted = Color(0xFFA19D8C), track = Color(0x3DC8A45C),
        acc2 = Color(0xFFEAD6A2), good = Color(0xFF7FC8A0), warn = Color(0xFFE3B45C), bad = Color(0xFFD9694F),
        carBody = Color(0xFF17201C), carGlass = Color(0xFF070B09),
        defaultAccent = Color(0xFFC8A45C), defaultShape = CardShape.SOFT,
        font = FontFamily.Serif, light = false
    )
    DriveMode.CLASSIC -> Palette(
        bgTop = Color(0xFF221A11), bgBottom = Color(0xFF150F09),
        card = Color(0xFF281F15), card2 = Color(0x47000000), line = Color(0xFF3D2F20),
        fg = Color(0xFFF2E6CC), muted = Color(0xFFB5A688), track = Color(0xFF3B2F22),
        acc2 = Color(0xFFF3C261), good = Color(0xFF8BBF5A), warn = Color(0xFFE0A030), bad = Color(0xFFC9382C),
        carBody = Color(0xFF5A1F1F), carGlass = Color(0xFF150D0A),
        defaultAccent = Color(0xFFE0A030), defaultShape = CardShape.SOFT,
        font = FontFamily.Serif, light = false
    )
    DriveMode.ECONOMY -> Palette(
        bgTop = Color(0xFFEEF5F0), bgBottom = Color(0xFFDDE8E1),
        card = Color(0xFFFFFFFF), card2 = Color(0xFFEEF5F0), line = Color(0xFFCFDFD6),
        fg = Color(0xFF12241C), muted = Color(0xFF58705F), track = Color(0xFFDBE8E0),
        acc2 = Color(0xFF0E8F9A), good = Color(0xFF1F9D63), warn = Color(0xFFD99A1C), bad = Color(0xFFD63C3C),
        carBody = Color(0xFFE9EEEB), carGlass = Color(0xFF6D8A86),
        defaultAccent = Color(0xFF1F9D63), defaultShape = CardShape.ROUND,
        font = FontFamily.SansSerif, light = true
    )
}

/** ما تراه كل المكونات: اللوحة + اللون المختار + شكل البطاقات. */
data class DarbTheme(val p: Palette, val accent: Color, val shape: CardShape)

val LocalDarb = staticCompositionLocalOf<DarbTheme> { error("DarbTheme غير متوفر") }

/** لون نص مقروء فوق لون معيّن. */
fun Color.onColor(): Color = if (luminance() > 0.55f) Color(0xFF101010) else Color.White
