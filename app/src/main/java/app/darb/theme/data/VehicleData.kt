package app.darb.theme.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

/**
 * موقع احتياطي يُستخدم قبل الحصول على أول تحديد GPS (لحساب المواقيت والطقس).
 * غيّره إلى مدينتك.
 */
const val DEFAULT_LAT = 22.28
const val DEFAULT_LNG = 39.10

/**
 * حالة المركبة. السرعة تأتي من GPS. الدورات والغيار غير متاحة من GPS،
 * وتظهر فقط إذا وفّرها مصدر بيانات حقيقي (OBD-II أو ناقل CAN) أو وضع المحاكاة.
 */
data class VehicleState(
    val speedKmh: Float,
    val rpm: Float? = null,
    val gear: Int? = null,
    val isDemo: Boolean = false
)

fun hasLocationPermission(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** آخر موقع معروف، أو الموقع الاحتياطي. */
@SuppressLint("MissingPermission")
fun lastKnownLocation(ctx: Context): Pair<Double, Double> {
    if (hasLocationPermission(ctx)) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                lm.getLastKnownLocation(p)?.let { return it.latitude to it.longitude }
            } catch (_: Exception) {
            }
        }
    }
    return DEFAULT_LAT to DEFAULT_LNG
}

/** السرعة الحقيقية من GPS. */
@SuppressLint("MissingPermission")
fun gpsFlow(ctx: Context): Flow<VehicleState> = callbackFlow {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    // كائن صريح بدل lambda حتى لا يحدث AbstractMethodError على أندرويد 8 إلى 10
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val kmh = if (location.hasSpeed()) location.speed * 3.6f else 0f
            trySend(VehicleState(speedKmh = kmh))
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }
    try {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
    } catch (e: Exception) {
        close(e)
    }
    awaitClose { lm.removeUpdates(listener) }
}

/** محاكاة قيادة لمعاينة الثيم دون سيارة. */
fun demoFlow(): Flow<VehicleState> = flow {
    val th = floatArrayOf(0f, 22f, 45f, 75f, 110f, 150f, 400f)
    var v = 96f
    var target = 110f
    var t = 0f
    var rpm = 2400f
    while (true) {
        delay(50)
        t -= 0.05f
        if (t <= 0f) {
            target = 60f + Random.nextFloat() * 120f
            t = 2.5f + Random.nextFloat() * 3f
        }
        v += (target - v) * 0.035f
        var gear = 1
        for (i in 1..5) if (v >= th[i]) gear = i + 1
        val rpmTarget = 1000f + (v - th[gear - 1]) / (th[gear] - th[gear - 1]) * 3800f
        rpm += (rpmTarget - rpm) * 0.2f
        emit(VehicleState(speedKmh = v, rpm = rpm, gear = gear, isDemo = true))
    }
}

/*
 * لقراءة الدورات والغيار والوقود وضغط الإطارات من السيارة نفسها:
 *   - OBD-II عبر محوّل ELM327 (بلوتوث) يعطي الدورات والسرعة وحرارة المحرك.
 *   - بعض الشاشات الصينية تعرض ناقل CAN عبر خدمة نظام خاصة بالشركة المصنّعة.
 * يكفي بناء Flow<VehicleState> جديد وتمريره بدل gpsFlow في DarbApp.
 */
