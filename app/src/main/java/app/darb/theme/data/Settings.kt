package app.darb.theme.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.darb.theme.ui.CardShape
import app.darb.theme.ui.DriveMode
import app.darb.theme.ui.GaugeStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** معرّفات الشريط السفلي: "home" و"map" مدمجان، وما عداهما اسم حزمة تطبيق. */
const val DOCK_HOME = "home"
const val DOCK_MAP = "map"
const val MAX_DOCK = 7

/** أسماء البطاقات في الشاشة الرئيسية. */
val WIDGETS = listOf("speed" to "العداد", "car" to "السيارة", "weather" to "الطقس", "prayer" to "مواقيت الصلاة")

data class ThemeSettings(
    val mode: DriveMode = DriveMode.SPORT,
    val accent: Int? = null,            // null = لون الوضع الافتراضي
    val shape: CardShape? = null,       // null = شكل الوضع الافتراضي
    val gauge: GaugeStyle = GaugeStyle.AUTO,
    val mapSplit: Boolean = false,
    val dock: List<String> = listOf(DOCK_HOME, DOCK_MAP),
    val hidden: Set<String> = emptySet(),
    val demo: Boolean = false           // محاكاة السرعة بدل GPS
)

private val Context.dataStore by preferencesDataStore(name = "darb_theme")

class SettingsRepo(private val ctx: Context) {

    private object K {
        val mode = stringPreferencesKey("mode")
        val accent = intPreferencesKey("accent")
        val hasAccent = booleanPreferencesKey("has_accent")
        val shape = stringPreferencesKey("shape")
        val gauge = stringPreferencesKey("gauge")
        val mapSplit = booleanPreferencesKey("map_split")
        val dock = stringPreferencesKey("dock")
        val hidden = stringPreferencesKey("hidden")
        val demo = booleanPreferencesKey("demo")
    }

    val flow: Flow<ThemeSettings> = ctx.dataStore.data.map { p ->
        ThemeSettings(
            mode = p[K.mode]?.let { runCatching { DriveMode.valueOf(it) }.getOrNull() } ?: DriveMode.SPORT,
            accent = if (p[K.hasAccent] == true) p[K.accent] else null,
            shape = p[K.shape]?.let { runCatching { CardShape.valueOf(it) }.getOrNull() },
            gauge = p[K.gauge]?.let { runCatching { GaugeStyle.valueOf(it) }.getOrNull() } ?: GaugeStyle.AUTO,
            mapSplit = p[K.mapSplit] ?: false,
            dock = p[K.dock]?.split("|")?.filter { it.isNotBlank() } ?: listOf(DOCK_HOME, DOCK_MAP),
            hidden = p[K.hidden]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            demo = p[K.demo] ?: false
        )
    }

    suspend fun save(s: ThemeSettings) {
        ctx.dataStore.edit { p ->
            p[K.mode] = s.mode.name
            if (s.accent != null) {
                p[K.accent] = s.accent
                p[K.hasAccent] = true
            } else {
                p[K.hasAccent] = false
            }
            p[K.shape] = s.shape?.name ?: ""
            p[K.gauge] = s.gauge.name
            p[K.mapSplit] = s.mapSplit
            p[K.dock] = s.dock.joinToString("|")
            p[K.hidden] = s.hidden.joinToString(",")
            p[K.demo] = s.demo
        }
    }
}
