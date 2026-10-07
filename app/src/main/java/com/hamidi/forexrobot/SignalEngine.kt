package com.hamidi.forexrobot

/**
 * Multi-Indicator Scalping Signal Engine
 * Combines: EMA9/21/50/200 + MACD + RSI + Stochastic + Bollinger Bands + ATR + OBV
 * Optimized for scalping (tight TP/SL based on ATR)
 */
object SignalEngine {

    fun generate(data: PriceData, symbol: String): SignalInfo {
        val minBars = 215
        if (data.closes.size < minBars)
            return SignalInfo.empty(symbol, "⚠ Need more data (${data.closes.size}/$minBars bars)")

        val c = data.closes; val h = data.highs; val l = data.lows; val v = data.volumes
        val last = c.size - 1; val prev = last - 1

        // ── Calculate All Indicators ──────────────────────────────────────
        val e9   = IndicatorEngine.ema(c, 9)
        val e21  = IndicatorEngine.ema(c, 21)
        val e50  = IndicatorEngine.ema(c, 50)
        val e200 = IndicatorEngine.ema(c, 200)
        val (macdArr, sigArr, histArr) = IndicatorEngine.macd(c)
        val rsiArr = IndicatorEngine.rsi(c)
        val (stochKArr, stochDArr) = IndicatorEngine.stochastic(h, l, c)
        val (bbU, bbM, bbL) = IndicatorEngine.bollingerBands(c)
        val atrArr = IndicatorEngine.atr(h, l, c)
        val obvArr = IndicatorEngine.obv(c, v)

        // ── Current Values ────────────────────────────────────────────────
        val price = c[last]
        val e9v   = e9[last];   val e21v  = e21[last]
        val e50v  = e50[last];  val e200v = e200[last]
        val macd  = macdArr[last]; val sig = sigArr[last]
        val rsi   = rsiArr[last]
        val kv    = stochKArr[last]; val dv = stochDArr[last]
        val bbu   = bbU[last]; val bbm = bbM[last]; val bbl = bbL[last]
        val atrv  = atrArr[last]

        // Previous values
        val pe9 = e9[prev]; val pe21 = e21[prev]
        val pM  = macdArr[prev]; val pS = sigArr[prev]
        val pK  = stochKArr[prev]; val pD = stochDArr[prev]

        // Guard invalid values
        if (e9v == 0.0 || e21v == 0.0 || macd == 0.0 || rsi == 0.0 || atrv == 0.0)
            return SignalInfo.empty(symbol, "⏳ Calculating indicators...")

        // ── OBV Trend ─────────────────────────────────────────────────────
        val obv5 = if (last >= 5) obvArr[last - 5] else obvArr[0]
        val obvTrend = when {
            obvArr[last] > obv5 * 1.001 -> "↑"
            obvArr[last] < obv5 * 0.999 -> "↓"
            else -> "→"
        }

        // ── Crossover Flags ───────────────────────────────────────────────
        val emaBullX  = pe9 <= pe21 && e9v > e21v
        val emaBearX  = pe9 >= pe21 && e9v < e21v
        val macdBullX = pM <= pS && macd > sig
        val macdBearX = pM >= pS && macd < sig
        val stochBullX = pK <= pD && kv > dv
        val stochBearX = pK >= pD && kv < dv

        // BB position 0=lower band, 1=upper band
        val bbRange = bbu - bbl
        val bbPos   = if (bbRange > 0) (price - bbl) / bbRange else 0.5

        // ── BUY SCORE ─────────────────────────────────────────────────────
        var buyScore = 0.0
        val buyInfo = mutableListOf<String>()

        if (e9v > e21v)  { buyScore += 2.0; buyInfo.add("📈 EMA9>EMA21") }
        if (emaBullX)    { buyScore += 0.5; buyInfo.add("🔔 EMA Crossover") }
        if (e50v > e200v){ buyScore += 1.0; buyInfo.add("📊 Golden Cross (50>200)") }
        if (price > e50v){ buyScore += 0.5; buyInfo.add("📊 Price>EMA50") }

        if (macd > sig)  { buyScore += if (macdBullX) 2.5 else 2.0; buyInfo.add("✅ MACD Bullish") }

        when {
            rsi < 30       -> { buyScore += 2.0; buyInfo.add("💚 RSI Oversold(${rsi.toInt()})") }
            rsi in 30.0..45.0 -> { buyScore += 1.8; buyInfo.add("✅ RSI Strong(${rsi.toInt()})") }
            rsi in 45.0..65.0 -> { buyScore += 1.2; buyInfo.add("📊 RSI OK(${rsi.toInt()})") }
            rsi in 65.0..75.0 -> { buyScore += 0.3; buyInfo.add("⚠ RSI High(${rsi.toInt()})") }
            rsi > 75       -> { buyScore -= 1.5; buyInfo.add("⛔ Overbought(${rsi.toInt()})") }
        }

        if (kv > dv && kv < 80)   { buyScore += if (stochBullX) 1.5 else 1.0; buyInfo.add("✅ Stoch Bull") }
        if (bbPos < 0.35)          { buyScore += 0.7; buyInfo.add("📊 Near BB Lower") }
        else if (price > bbm)      { buyScore += 0.3; buyInfo.add("📊 Above BB Mid") }
        if (obvTrend == "↑")       { buyScore += 0.5; buyInfo.add("📈 OBV Rising") }

        // ── SELL SCORE ────────────────────────────────────────────────────
        var sellScore = 0.0
        val sellInfo = mutableListOf<String>()

        if (e9v < e21v)  { sellScore += 2.0; sellInfo.add("📉 EMA9<EMA21") }
        if (emaBearX)    { sellScore += 0.5; sellInfo.add("🔔 EMA Crossover") }
        if (e50v < e200v){ sellScore += 1.0; sellInfo.add("📊 Death Cross (50<200)") }
        if (price < e50v){ sellScore += 0.5; sellInfo.add("📊 Price<EMA50") }

        if (macd < sig)  { sellScore += if (macdBearX) 2.5 else 2.0; sellInfo.add("✅ MACD Bearish") }

        when {
            rsi > 70       -> { sellScore += 2.0; sellInfo.add("🔴 RSI Overbought(${rsi.toInt()})") }
            rsi in 55.0..70.0 -> { sellScore += 1.8; sellInfo.add("✅ RSI Strong(${rsi.toInt()})") }
            rsi in 40.0..55.0 -> { sellScore += 1.2; sellInfo.add("📊 RSI OK(${rsi.toInt()})") }
            rsi in 30.0..40.0 -> { sellScore += 0.3; sellInfo.add("⚠ RSI Low(${rsi.toInt()})") }
            rsi < 30       -> { sellScore -= 1.5; sellInfo.add("⛔ Oversold(${rsi.toInt()})") }
        }

        if (kv < dv && kv > 20)   { sellScore += if (stochBearX) 1.5 else 1.0; sellInfo.add("✅ Stoch Bear") }
        if (bbPos > 0.65)          { sellScore += 0.7; sellInfo.add("📊 Near BB Upper") }
        else if (price < bbm)      { sellScore += 0.3; sellInfo.add("📊 Below BB Mid") }
        if (obvTrend == "↓")       { sellScore += 0.5; sellInfo.add("📉 OBV Falling") }

        // ── Determine Final Signal ────────────────────────────────────────
        val maxScore = 10.0
        val type: String; val details: List<String>; val score: Double; val isCross: Boolean

        when {
            buyScore >= 5.0 && buyScore > sellScore -> {
                type = "BUY"; details = buyInfo; score = buyScore; isCross = emaBullX || macdBullX
            }
            sellScore >= 5.0 && sellScore > buyScore -> {
                type = "SELL"; details = sellInfo; score = sellScore; isCross = emaBearX || macdBearX
            }
            else -> {
                type = "WAIT"; details = listOf("⚖ Mixed signals — no clear direction")
                score = maxOf(buyScore, sellScore); isCross = false
            }
        }

        val confidence = ((score / maxScore) * 100).toInt().coerceIn(0, 97)

        // ── Scalping TP/SL (ATR-based — tight levels for scalping) ────────
        // SL = 1.0 × ATR  |  TP = 1.5 × ATR  |  RR = 1:1.5
        val slDist = atrv * 1.0
        val tpDist = atrv * 1.5
        val entry = price
        val sl = if (type == "SELL") entry + slDist else entry - slDist
        val tp = if (type == "SELL") entry - tpDist else entry + tpDist

        val strength = when {
            confidence >= 78 -> "STRONG"; confidence >= 62 -> "MODERATE"; else -> "WEAK"
        }

        return SignalInfo(
            type = type, confidence = confidence,
            entry = entry, stopLoss = sl, takeProfit = tp, atr = atrv,
            macdValue = macd, macdSignalVal = sig, macdHistogram = histArr[last],
            rsiValue = rsi, ema9 = e9v, ema21 = e21v, ema50 = e50v, ema200 = e200v,
            stochK = kv, stochD = dv,
            bbUpper = bbu, bbMiddle = bbm, bbLower = bbl,
            obvTrend = obvTrend, symbol = symbol, isCrossover = isCross,
            signalStrength = strength,
            indicatorSummary = details.take(3).joinToString(" | "),
            signalDetails = details
        )
    }
}
