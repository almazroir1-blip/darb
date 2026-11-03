package app.darb.theme.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.darb.theme.data.AppEntry
import app.darb.theme.data.DOCK_HOME
import app.darb.theme.data.DOCK_MAP
import app.darb.theme.data.MAX_DOCK
import app.darb.theme.data.SettingsRepo
import app.darb.theme.data.ThemeSettings
import app.darb.theme.data.VehicleState
import app.darb.theme.data.Weather
import app.darb.theme.data.WeatherRepo
import app.darb.theme.data.demoFlow
import app.darb.theme.data.gpsFlow
import app.darb.theme.data.hasLocationPermission
import app.darb.theme.data.lastKnownLocation
import app.darb.theme.data.launchApp
import app.darb.theme.data.loadLaunchableApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** الجذر: يجمع الإعدادات والبيانات الحية ويرسم الشاشة الرئيسية أو الخريطة مع الشريط السفلي. */
@Composable
fun DarbApp() {
    val ctx = LocalContext.current
    val repo = remember { SettingsRepo(ctx.applicationContext) }
    val s by repo.flow.collectAsState(initial = ThemeSettings())
    val scope = rememberCoroutineScope()
    val save: (ThemeSettings) -> Unit = { n -> scope.launch { repo.save(n) } }

    // الثيم الحالي
    val pal = paletteFor(s.mode)
    val accent = s.accent?.let { Color(it) } ?: pal.defaultAccent
    val theme = DarbTheme(pal, accent, s.shape ?: pal.defaultShape)

    // صلاحية الموقع
    var hasLoc by remember { mutableStateOf(hasLocationPermission(ctx)) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLoc = hasLocationPermission(ctx)
    }
    LaunchedEffect(Unit) {
        if (!hasLoc) {
            permLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // الموقع والطقس
    val loc by produceState(lastKnownLocation(ctx), hasLoc) {
        while (true) {
            value = lastKnownLocation(ctx)
            delay(300_000)
        }
    }
    val weather by produceState<Weather?>(null, loc) {
        while (true) {
            value = WeatherRepo.fetch(loc.first, loc.second) ?: value
            delay(20 * 60_000L)
        }
    }

    // السرعة: GPS الحقيقي، أو المحاكاة عند اختيارها أو عند عدم توفر الصلاحية
    val vehicleFlow = remember(hasLoc, s.demo) { if (hasLoc && !s.demo) gpsFlow(ctx) else demoFlow() }
    val vehicle by vehicleFlow.collectAsState(initial = VehicleState(0f))

    // التطبيقات المثبّتة
    val apps by produceState(emptyList<AppEntry>()) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(ctx) }
    }

    var view by remember { mutableStateOf("home") }
    var editing by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    // الشاشة الرئيسية للجهاز: زر الرجوع لا يغلقها
    BackHandler(enabled = true) {
        when {
            showPicker -> showPicker = false
            showCustomize -> showCustomize = false
            editing -> editing = false
            view != "home" -> view = "home"
        }
    }

    MaterialTheme(colorScheme = if (pal.light) lightColorScheme() else darkColorScheme()) {
        CompositionLocalProvider(
            LocalDarb provides theme,
            LocalLayoutDirection provides LayoutDirection.Rtl
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(pal.bgTop, pal.bgBottom)))) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TopBar(
                        modeLabel = s.mode.label,
                        editing = editing,
                        onEdit = { editing = !editing },
                        onCustomize = { showCustomize = true }
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (view == "map") {
                            MapScreen(s, vehicle, loc)
                        } else {
                            HomeContent(
                                s = s, v = vehicle, weather = weather, loc = loc, editing = editing,
                                onToggle = { id ->
                                    save(s.copy(hidden = if (id in s.hidden) s.hidden - id else s.hidden + id))
                                }
                            )
                        }
                    }
                    Dock(
                        s = s, apps = apps, view = view, editing = editing,
                        onOpen = { id ->
                            when (id) {
                                DOCK_HOME -> view = "home"
                                DOCK_MAP -> view = "map"
                                else -> launchApp(ctx, id)
                            }
                        },
                        onRemove = { id -> save(s.copy(dock = s.dock - id)) },
                        onAdd = { showPicker = true }
                    )
                }

                if (showCustomize) {
                    CustomizeDialog(s = s, onChange = save, onDismiss = { showCustomize = false })
                }
                if (showPicker) {
                    AppPickerDialog(
                        apps = apps, current = s.dock,
                        onPick = { pkg ->
                            if (s.dock.size < MAX_DOCK) save(s.copy(dock = s.dock + pkg))
                            showPicker = false
                        },
                        onDismiss = { showPicker = false }
                    )
                }
            }
        }
    }
}
