package com.hamidi.forexrobot

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Complete Technical Analysis Library
 * EMA, SMA, MACD, RSI, Stochastic, Bollinger Bands, ATR, OBV
 */
object IndicatorEngine {

    // ────────────────────────────────────────────────────────────────────────
    // EMA — Exponential Moving Average
    // ────────────────────────────────────────────────────────────────────────
    fun ema(prices: DoubleArray, period: Int): DoubleArray {
        val result = DoubleArray(prices.size)
        if (prices.size < period) return result
        val k = 2.0 / (period + 1.0)
        var sum = 0.0
        for (i in 0 until period) sum += prices[i]
        result[period - 1] = sum / period
        for (i in period until prices.size)
            result[i] = prices[i] * k + result[i - 1] * (1.0 - k)
        return result
    }

    // ────────────────────────────────────────────────────────────────────────
    // SMA — Simple Moving Average
    // ────────────────────────────────────────────────────────────────────────
    fun sma(prices: DoubleArray, period: Int): DoubleArray {
        val result = DoubleArray(prices.size)
        for (i in period - 1 until prices.size) {
            var sum = 0.0
            for (j in 0 until period) sum += prices[i - j]
            result[i] = sum / period
        }
        return result
    }

    // ────────────────────────────────────────────────────────────────────────
    // MACD (12, 26, 9) — Moving Average Convergence Divergence
    // Returns: Triple(macdLine, signalLine, histogram)
    // ────────────────────────────────────────────────────────────────────────
    fun macd(closes: DoubleArray): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val fast = ema(closes, 12)
        val slow = ema(closes, 26)
        val macdLine = DoubleArray(closes.size) { i ->
            if (slow[i] == 0.0) 0.0 else fast[i] - slow[i]
        }
        val validStart = 25
        val macdValid = macdLine.copyOfRange(validStart, macdLine.size)
        val sigValid = ema(macdValid, 9)
        val signalLine = DoubleArray(closes.size) { i ->
            val idx = i - validStart
            if (idx < 0 || sigValid[idx] == 0.0) 0.0 else sigValid[idx]
        }
        val histogram = DoubleArray(closes.size) { i ->
            if (macdLine[i] == 0.0 || signalLine[i] == 0.0) 0.0
            else macdLine[i] - signalLine[i]
        }
        return Triple(macdLine, signalLine, histogram)
    }

    // ────────────────────────────────────────────────────────────────────────
    // RSI (14) — Relative Strength Index
    // ────────────────────────────────────────────────────────────────────────
    fun rsi(closes: DoubleArray, period: Int = 14): DoubleArray {
        val result = DoubleArray(closes.size)
        if (closes.size <= period) return result
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1..period) {
            val chg = closes[i] - closes[i - 1]
            if (chg > 0) avgGain += chg else avgLoss -= chg
        }
        avgGain /= period; avgLoss /= period
        result[period] = if (avgLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + avgGain / avgLoss)
        for (i in period + 1 until closes.size) {
            val chg = closes[i] - closes[i - 1]
            avgGain = (avgGain * (period - 1) + maxOf(0.0, chg)) / period
            avgLoss = (avgLoss * (period - 1) + maxOf(0.0, -chg)) / period
            result[i] = if (avgLoss == 0.0) 100.0 else 100.0 - 100.0 / (1.0 + avgGain / avgLoss)
        }
        return result
    }

    // ────────────────────────────────────────────────────────────────────────
    // Stochastic (14, 3, 3) — Returns Pair(%K, %D)
    // ────────────────────────────────────────────────────────────────────────
    fun stochastic(
        highs: DoubleArray, lows: DoubleArray, closes: DoubleArray,
        kPeriod: Int = 14, dPeriod: Int = 3
    ): Pair<DoubleArray, DoubleArray> {
        val rawK = DoubleArray(closes.size)
        val smoothK = DoubleArray(closes.size)
        val d = DoubleArray(closes.size)
        for (i in kPeriod - 1 until closes.size) {
            val hh = highs.copyOfRange(i - kPeriod + 1, i + 1).maxOrNull() ?: 0.0
            val ll = lows.copyOfRange(i - kPeriod + 1, i + 1).minOrNull() ?: 0.0
            rawK[i] = if (hh == ll) 50.0 else (closes[i] - ll) / (hh - ll) * 100.0
        }
        // Smooth %K with 3-period SMA
        val kStart = kPeriod + dPeriod - 2
        for (i in kStart until closes.size) {
            var sum = 0.0
            for (j in 0 until dPeriod) sum += rawK[i - j]
            smoothK[i] = sum / dPeriod
        }
        // %D = 3-period SMA of smooth %K
        val dStart = kStart + dPeriod - 1
        for (i in dStart until closes.size) {
            var sum = 0.0
            for (j in 0 until dPeriod) sum += smoothK[i - j]
            d[i] = sum / dPeriod
        }
        return Pair(smoothK, d)
    }

    // ────────────────────────────────────────────────────────────────────────
    // Bollinger Bands (20, 2) — Returns Triple(upper, middle, lower)
    // ────────────────────────────────────────────────────────────────────────
    fun bollingerBands(
        closes: DoubleArray, period: Int = 20, mult: Double = 2.0
    ): Triple<DoubleArray, DoubleArray, DoubleArray> {
        val upper = DoubleArray(closes.size)
        val mid = DoubleArray(closes.size)
        val lower = DoubleArray(closes.size)
        for (i in period - 1 until closes.size) {
            val slice = closes.copyOfRange(i - period + 1, i + 1)
            val mean = slice.average()
            val std = sqrt(slice.map { (it - mean).pow(2) }.average())
            mid[i] = mean; upper[i] = mean + mult * std; lower[i] = mean - mult * std
        }
        return Triple(upper, mid, lower)
    }

    // ────────────────────────────────────────────────────────────────────────
    // ATR (14) — Average True Range (for TP/SL calculation)
    // ────────────────────────────────────────────────────────────────────────
    fun atr(highs: DoubleArray, lows: DoubleArray, closes: DoubleArray, period: Int = 14): DoubleArray {
        val tr = DoubleArray(closes.size)
        val result = DoubleArray(closes.size)
        tr[0] = highs[0] - lows[0]
        for (i in 1 until closes.size)
            tr[i] = maxOf(highs[i] - lows[i], abs(highs[i] - closes[i - 1]), abs(lows[i] - closes[i - 1]))
        var sum = 0.0
        for (i in 0 until period) sum += tr[i]
        result[period - 1] = sum / period
        for (i in period until closes.size)
            result[i] = (result[i - 1] * (period - 1) + tr[i]) / period
        return result
    }

    // ────────────────────────────────────────────────────────────────────────
    // OBV — On-Balance Volume
    // ────────────────────────────────────────────────────────────────────────
    fun obv(closes: DoubleArray, volumes: DoubleArray): DoubleArray {
        val result = DoubleArray(closes.size)
        result[0] = volumes[0]
        for (i in 1 until closes.size)
            result[i] = when {
                closes[i] > closes[i - 1] -> result[i - 1] + volumes[i]
                closes[i] < closes[i - 1] -> result[i - 1] - volumes[i]
                else -> result[i - 1]
            }
        return result
    }

    // ────────────────────────────────────────────────────────────────────────
    // Volume MA (20)
    // ────────────────────────────────────────────────────────────────────────
    fun volumeMA(volumes: DoubleArray, period: Int = 20): DoubleArray = sma(volumes, period)
}
