package app.darb.theme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.darb.theme.data.ThemeSettings
import app.darb.theme.data.WIDGETS

private val SWATCHES = listOf(
    0xFFFF4D2E, 0xFFFF9F1C, 0xFFC8A45C, 0xFF1F9D63,
    0xFF17B8B0, 0xFF3A86FF, 0xFF8B5CF6, 0xFFFF5D9E
).map { it.toInt() }

/** لوحة التخصيص: الوضع واللون وشكل البطاقات والعداد والخريطة والبطاقات والمحاكاة. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizeDialog(s: ThemeSettings, onChange: (ThemeSettings) -> Unit, onDismiss: () -> Unit) {
    val t = LocalDarb.current
    var hue by remember { mutableFloatStateOf(14f) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onDismiss),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(460.dp)
                    .background(t.p.card)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("تخصيص الثيم", color = t.p.fg, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)

                Section("وضع القيادة") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DriveMode.entries.forEach { m ->
                            Chip(m.label, s.mode == m) { onChange(s.copy(mode = m)) }
                        }
                    }
                }

                Section("اللون") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Chip("حسب الوضع", s.accent == null) { onChange(s.copy(accent = null)) }
                        SWATCHES.forEach { c ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(if (s.accent == c) 3.dp else 1.dp, if (s.accent == c) t.p.fg else t.p.line, CircleShape)
                                    .clickable { onChange(s.copy(accent = c)) }
                            )
                        }
                    }
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            onChange(s.copy(accent = Color.hsv(it, 0.75f, 1f).toArgb()))
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(thumbColor = t.accent, activeTrackColor = t.accent)
                    )
                }

                Section("شكل البطاقات") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("حسب الوضع", s.shape == null) { onChange(s.copy(shape = null)) }
                        CardShape.entries.forEach { sh ->
                            Chip(sh.label, s.shape == sh) { onChange(s.copy(shape = sh)) }
                        }
                    }
                }

                Section("شكل العداد") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GaugeStyle.entries.forEach { g ->
                            Chip(g.label, s.gauge == g) { onChange(s.copy(gauge = g)) }
                        }
                    }
                }

                Section("وضع الخريطة") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("ملء الشاشة", !s.mapSplit) { onChange(s.copy(mapSplit = false)) }
                        Chip("شاشة مقسومة", s.mapSplit) { onChange(s.copy(mapSplit = true)) }
                    }
                }

                Section("البطاقات في الرئيسية") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WIDGETS.forEach { (id, name) ->
                            val visible = id !in s.hidden
                            Chip(name, visible) {
                                onChange(s.copy(hidden = if (visible) s.hidden + id else s.hidden - id))
                            }
                        }
                    }
                }

                Section("مصدر السرعة") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("GPS", !s.demo) { onChange(s.copy(demo = false)) }
                        Chip("محاكاة", s.demo) { onChange(s.copy(demo = true)) }
                    }
                }

                Chip("تم", true, Modifier.align(Alignment.End)) { onDismiss() }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val t = LocalDarb.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = t.p.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font)
        content()
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = LocalDarb.current
    Box(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) t.accent else t.p.card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text, color = if (selected) t.accent.onColor() else t.p.fg,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFamily = t.p.font
        )
    }
}
