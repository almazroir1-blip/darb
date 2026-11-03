package app.darb.theme.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.darb.theme.data.AppEntry
import app.darb.theme.data.DOCK_HOME
import app.darb.theme.data.DOCK_MAP
import app.darb.theme.data.MAX_DOCK
import app.darb.theme.data.ThemeSettings

/** الشريط السفلي: الرئيسية والخريطة مدمجان، وبقية العناصر تطبيقات حقيقية مثبّتة على الشاشة. */
@Composable
fun Dock(
    s: ThemeSettings, apps: List<AppEntry>, view: String, editing: Boolean,
    onOpen: (String) -> Unit, onRemove: (String) -> Unit, onAdd: () -> Unit
) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.card)
    Row(
        Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(shape)
            .background(t.p.card)
            .border(1.dp, t.p.line, shape)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (id in s.dock) {
            when (id) {
                DOCK_HOME -> DockItem("الرئيسية", view == "home", editing, Modifier.weight(1f), { onOpen(id) }, { onRemove(id) }) {
                    BuiltinIcon(DOCK_HOME, if (view == "home") t.accent else t.p.fg)
                }
                DOCK_MAP -> DockItem("الخريطة", view == "map", editing, Modifier.weight(1f), { onOpen(id) }, { onRemove(id) }) {
                    BuiltinIcon(DOCK_MAP, if (view == "map") t.accent else t.p.fg)
                }
                else -> {
                    val app = apps.firstOrNull { it.pkg == id } ?: continue
                    DockItem(app.label, false, editing, Modifier.weight(1f), { onOpen(id) }, { onRemove(id) }) {
                        Image(app.icon, app.label, Modifier.size(36.dp))
                    }
                }
            }
        }
        if (editing && s.dock.size < MAX_DOCK) {
            DockItem("إضافة", false, false, Modifier.weight(1f), onAdd, null) {
                BuiltinIcon("plus", t.accent)
            }
        }
    }
}

@Composable
private fun DockItem(
    label: String, active: Boolean, editing: Boolean, modifier: Modifier,
    onClick: () -> Unit, onRemove: (() -> Unit)?, icon: @Composable () -> Unit
) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.inner)
    Box(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(if (active) t.accent.copy(alpha = 0.16f) else Color.Transparent)
            .then(if (editing) Modifier.border(1.dp, t.p.muted, shape) else Modifier)
            .clickable(onClick = if (editing) ({}) else onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            icon()
            Text(
                label, color = if (active) t.accent else t.p.fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                fontFamily = t.p.font, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        if (editing && onRemove != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(t.p.bad)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) { Text("×", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun BuiltinIcon(id: String, color: Color, modifier: Modifier = Modifier.size(32.dp)) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val st = Stroke(1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (id) {
            DOCK_HOME -> {
                val p = Path().apply {
                    moveTo(3f * s, 11f * s); lineTo(12f * s, 3f * s); lineTo(21f * s, 11f * s)
                    lineTo(21f * s, 21f * s); lineTo(15f * s, 21f * s); lineTo(15f * s, 14f * s)
                    lineTo(9f * s, 14f * s); lineTo(9f * s, 21f * s); lineTo(3f * s, 21f * s); close()
                }
                drawPath(p, color, style = st)
            }
            DOCK_MAP -> {
                drawCircle(color, 7f * s, Offset(12f * s, 10f * s), style = st)
                drawCircle(color, 2.5f * s, Offset(12f * s, 10f * s), style = st)
                drawLine(color, Offset(12f * s, 17f * s), Offset(12f * s, 22f * s), 1.8f * s, StrokeCap.Round)
            }
            else -> { // plus
                drawLine(color, Offset(12f * s, 5f * s), Offset(12f * s, 19f * s), 1.8f * s, StrokeCap.Round)
                drawLine(color, Offset(5f * s, 12f * s), Offset(19f * s, 12f * s), 1.8f * s, StrokeCap.Round)
            }
        }
    }
}

/** اختيار تطبيق من المثبّتة لإضافته إلى الشريط. */
@Composable
fun AppPickerDialog(apps: List<AppEntry>, current: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.card)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .width(720.dp)
                .height(420.dp)
                .clip(shape)
                .background(t.p.card)
                .border(1.dp, t.p.line, shape)
                .padding(16.dp)
        ) {
            Text("اختر تطبيقًا لإضافته", color = t.p.fg, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
            val choices = apps.filter { it.pkg !in current }
            if (choices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد تطبيقات أخرى", color = t.p.muted, fontFamily = t.p.font)
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Adaptive(96.dp),
                    Modifier.padding(top = 12.dp).fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(choices, key = { it.pkg }) { a ->
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(t.shape.inner))
                                .background(t.p.card2)
                                .clickable { onPick(a.pkg) }
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(a.icon, a.label, Modifier.size(44.dp))
                            Text(a.label, color = t.p.fg, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = t.p.font)
                        }
                    }
                }
            }
        }
    }
}
