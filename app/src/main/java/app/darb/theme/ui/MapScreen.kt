package app.darb.theme.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.darb.theme.data.PrayerTimes
import app.darb.theme.data.ThemeSettings
import app.darb.theme.data.VehicleState
import app.darb.theme.data.hasLocationPermission
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime

/** شاشة الخريطة: ملء الشاشة أو مقسومة مع العداد حسب إعداد المستخدم. */
@Composable
fun MapScreen(s: ThemeSettings, v: VehicleState, loc: Pair<Double, Double>) {
    val t = LocalDarb.current
    val mapState = remember { mutableStateOf<MapView?>(null) }
    val dark = !t.p.light
    val style = s.gauge.resolve(s.mode)

    if (!s.mapSplit) {
        MapBox(loc, dark, mapState, v, showSpeed = true, Modifier.fillMaxSize())
    } else {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MapBox(loc, dark, mapState, v, showSpeed = false, Modifier.weight(1.55f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SpeedCard(style, v, s.mode, Modifier.weight(1f).fillMaxWidth())
                DarbCard(Modifier.fillMaxWidth()) { NextPrayerRow(loc) }
            }
        }
    }
}

@Composable
private fun NextPrayerRow(loc: Pair<Double, Double>) {
    val t = LocalDarb.current
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            value = ZonedDateTime.now()
            delay(1000)
        }
    }
    val st = PrayerTimes.state(now, loc.first, loc.second)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(st.nextPrayer.name, color = t.p.fg, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
        Text(countdown(Duration.between(now, st.nextPrayer.time).seconds), color = t.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
    }
}

@Composable
private fun MapBox(
    loc: Pair<Double, Double>, dark: Boolean, mapState: MutableState<MapView?>,
    v: VehicleState, showSpeed: Boolean, modifier: Modifier
) {
    val t = LocalDarb.current
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(t.shape.card)
    Box(modifier.clip(shape).border(1.dp, t.p.line, shape)) {
        OsmMap(loc, dark, mapState, Modifier.fillMaxSize())

        // الصلاة القادمة (أعلى اليمين في الواجهة العربية)
        if (showSpeed) {
            Box(Modifier.align(Alignment.TopStart).padding(14.dp)) {
                OverlayBox { NextPrayerRow(loc) }
            }
        }

        // التكبير والتصغير
        Column(Modifier.align(Alignment.CenterEnd).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ZoomButton("+") { mapState.value?.controller?.zoomIn() }
            ZoomButton("−") { mapState.value?.controller?.zoomOut() }
        }

        // فتح تطبيق الملاحة (Google Maps أو Waze أو غيرهما)
        Box(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(t.shape.inner))
                    .background(t.accent)
                    .clickable {
                        val uri = Uri.parse("geo:${loc.first},${loc.second}")
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp)
            ) {
                Text("فتح تطبيق الملاحة", color = t.accent.onColor(), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
            }
        }

        // السرعة الحالية
        if (showSpeed) {
            Box(Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                OverlayBox {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(v.speedKmh.toInt().toString(), color = t.p.fg, fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = t.p.font)
                        Text("كم/س", color = t.p.muted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayBox(content: @Composable () -> Unit) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.inner)
    Box(
        Modifier
            .clip(shape)
            .background(t.p.card.copy(alpha = 0.92f))
            .border(1.dp, t.p.line, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) { content() }
}

@Composable
private fun ZoomButton(label: String, onClick: () -> Unit) {
    val t = LocalDarb.current
    val shape = RoundedCornerShape(t.shape.inner)
    Box(
        Modifier.size(52.dp).clip(shape).background(t.p.card).border(1.dp, t.p.line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = t.p.fg, fontSize = 26.sp) }
}

/**
 * خريطة OpenStreetMap عبر osmdroid. في الأوضاع الداكنة تُعكس ألوان البلاطات.
 * ملاحظة: خوادم OSM العامة لها سياسة استخدام، فاستبدلها بمزوّد بلاطات مناسب عند النشر.
 */
@Composable
private fun OsmMap(loc: Pair<Double, Double>, dark: Boolean, mapState: MutableState<MapView?>, modifier: Modifier) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = ctx.packageName
            osmdroidBasePath = File(ctx.cacheDir, "osmdroid")
            osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(loc.first, loc.second))
            if (hasLocationPermission(ctx)) {
                val me = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                me.enableMyLocation()
                me.enableFollowLocation()
                overlays.add(me)
            }
        }
    }
    LaunchedEffect(map) { mapState.value = map }

    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            map.onDetach()
        }
    }

    AndroidView(
        factory = { map },
        modifier = modifier,
        update = { it.overlayManager.tilesOverlay.setColorFilter(if (dark) TilesOverlay.INVERT_COLORS else null) }
    )
}
