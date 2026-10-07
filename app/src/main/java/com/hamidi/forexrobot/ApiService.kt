package com.hamidi.forexrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val decimalPlaces = mapOf(
        "XAUUSD.m"  to 2,
        "JP225.std" to 1,
        "BTCUSD.m"  to 2
    )

    // ── Symbol cascades — 60d range ensures 215+ bars for EMA200 ──────────
    // XAUUSD fix: use GC=F (Gold Futures) as primary — more reliable on Yahoo
    private val symbolCascades = mapOf(
        "XAUUSD.m"  to listOf(
            "GC=F|15m|60d",       // Gold Futures — most reliable
            "XAUUSD=X|15m|60d",   // Spot gold
            "GC=F|1h|60d",        // Gold Futures 1h fallback
            "XAUUSD=X|1h|60d"
        ),
        "JP225.std" to listOf(
            "^N225|15m|60d",      // Nikkei 225
            "^N225|1h|60d",       // Nikkei 1h fallback
            "EWJ|15m|60d"         // Japan ETF
        ),
        "BTCUSD.m"  to listOf(
            "BTC-USD|15m|60d",    // Bitcoin
            "BTC-USD|1h|60d"
        )
    )

    suspend fun getPriceData(symbol: String): PriceData? {
        return withContext(Dispatchers.IO) {
            val cascades = symbolCascades[symbol] ?: return@withContext null
            for (cascade in cascades) {
                val (ySym, interval, range) = cascade.split("|")
                // Try query1 first, then query2 mirror
                for (host in listOf("query1", "query2")) {
                    val data = fetchYahoo(host, ySym, interval, range)
                    if (data != null && data.closes.size >= 215) return@withContext data
                }
            }
            null
        }
    }

    private fun fetchYahoo(host: String, sym: String, interval: String, range: String): PriceData? {
        return try {
            val url = "https://$host.finance.yahoo.com/v8/finance/chart/$sym" +
                      "?interval=$interval&range=$range&includePrePost=false"
            val req = Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 12; Mobile)")
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            parseResponse(body)
        } catch (e: Exception) { null }
    }

    private fun parseResponse(json: String): PriceData? {
        return try {
            val root   = JSONObject(json)
            val chart  = root.getJSONObject("chart")
            if (!chart.isNull("error") && chart.get("error") != null &&
                chart.get("error").toString() != "null") return null
            val results = chart.optJSONArray("result") ?: return null
            if (results.length() == 0) return null

            val result     = results.getJSONObject(0)
            val indicators = result.getJSONObject("indicators")
            val quoteArr   = indicators.getJSONArray("quote")
            val q          = quoteArr.getJSONObject(0)

            val closeArr  = q.getJSONArray("close")
            val highArr   = q.getJSONArray("high")
            val lowArr    = q.getJSONArray("low")
            val openArr   = q.getJSONArray("open")
            val volumeArr = if (q.has("volume")) q.getJSONArray("volume") else null

            val closes  = mutableListOf<Double>()
            val highs   = mutableListOf<Double>()
            val lows    = mutableListOf<Double>()
            val opens   = mutableListOf<Double>()
            val volumes = mutableListOf<Double>()

            for (i in 0 until closeArr.length()) {
                if (closeArr.isNull(i) || highArr.isNull(i) ||
                    lowArr.isNull(i)   || openArr.isNull(i)) continue
                val c  = closeArr.getDouble(i)
                val h  = highArr.getDouble(i)
                val lo = lowArr.getDouble(i)
                val o  = openArr.getDouble(i)
                if (c <= 0 || h <= 0 || lo <= 0 || o <= 0) continue
                closes.add(c); highs.add(h); lows.add(lo); opens.add(o)
                val vol = if (volumeArr != null && !volumeArr.isNull(i))
                    volumeArr.getDouble(i) else 1000.0
                volumes.add(maxOf(vol, 1.0))
            }

            if (closes.size < 50) return null

            // Cap at 600 bars to limit memory — still enough for EMA200
            val cap = 600
            val from = maxOf(0, closes.size - cap)
            PriceData(
                closes  = closes.subList(from, closes.size).toDoubleArray(),
                highs   = highs.subList(from, highs.size).toDoubleArray(),
                lows    = lows.subList(from, lows.size).toDoubleArray(),
                opens   = opens.subList(from, opens.size).toDoubleArray(),
                volumes = volumes.subList(from, volumes.size).toDoubleArray()
            )
        } catch (e: Exception) { null }
    }
}
