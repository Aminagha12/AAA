package com.hamidi.forexrobot

data class SignalInfo(
    val type: String,              // BUY / SELL / WAIT
    val confidence: Int,           // 0-100
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val atr: Double,
    val macdValue: Double,
    val macdSignalVal: Double,
    val macdHistogram: Double,
    val rsiValue: Double,
    val ema9: Double,
    val ema21: Double,
    val ema50: Double,
    val ema200: Double,
    val stochK: Double,
    val stochD: Double,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val obvTrend: String,           // ↑ ↓ →
    val symbol: String,
    val isCrossover: Boolean,
    val signalStrength: String,     // STRONG / MODERATE / WEAK
    val indicatorSummary: String,
    val signalDetails: List<String>
) {
    companion object {
        fun empty(symbol: String, msg: String = "Loading...") = SignalInfo(
            type = "WAIT", confidence = 0,
            entry = 0.0, stopLoss = 0.0, takeProfit = 0.0, atr = 0.0,
            macdValue = 0.0, macdSignalVal = 0.0, macdHistogram = 0.0,
            rsiValue = 0.0, ema9 = 0.0, ema21 = 0.0, ema50 = 0.0, ema200 = 0.0,
            stochK = 0.0, stochD = 0.0, bbUpper = 0.0, bbMiddle = 0.0, bbLower = 0.0,
            obvTrend = "→", symbol = symbol,
            isCrossover = false, signalStrength = "WEAK",
            indicatorSummary = msg, signalDetails = listOf(msg)
        )
    }
}

data class PriceData(
    val closes: DoubleArray,
    val highs: DoubleArray,
    val lows: DoubleArray,
    val opens: DoubleArray,
    val volumes: DoubleArray
)
