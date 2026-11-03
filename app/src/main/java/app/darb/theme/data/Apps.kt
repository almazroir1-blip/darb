package app.darb.theme.data

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

/** التطبيقات المثبّتة القابلة للتشغيل (عدا هذا التطبيق نفسه). */
fun loadLaunchableApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map {
            AppEntry(
                pkg = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
            )
        }
        .distinctBy { it.pkg }
        .sortedBy { it.label }
}

fun launchApp(ctx: Context, pkg: String) {
    val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i)
}
