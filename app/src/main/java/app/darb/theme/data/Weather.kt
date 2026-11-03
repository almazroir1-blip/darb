package app.darb.theme.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class Weather(
    val temp: Int,
    val hi: Int,
    val lo: Int,
    val humidity: Int,
    val windKmh: Int,
    val text: String
)

/** الطقس من Open-Meteo (مجاني وبلا مفتاح). يعيد null عند فشل الاتصال. */
object WeatherRepo {

    suspend fun fetch(lat: Double, lng: Double): Weather? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng" +
                    "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
            )
            val c = url.openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val body = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val j = JSONObject(body)
            val cur = j.getJSONObject("current")
            val daily = j.getJSONObject("daily")
            Weather(
                temp = cur.getDouble("temperature_2m").roundToInt(),
                hi = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                lo = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                humidity = cur.getInt("relative_humidity_2m"),
                windKmh = cur.getDouble("wind_speed_10m").roundToInt(),
                text = describe(cur.getInt("weather_code"))
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "صافٍ"
        1, 2 -> "غائم جزئيًا"
        3 -> "غائم"
        45, 48 -> "ضباب"
        in 51..57 -> "رذاذ"
        in 61..67 -> "مطر"
        in 71..77 -> "ثلج"
        in 80..82 -> "زخات مطر"
        in 95..99 -> "عاصفة رعدية"
        else -> "—"
    }
}
