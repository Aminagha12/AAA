package com.hamidi.forexrobot

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val SYMBOLS = listOf("XAUUSD.m", "JP225.std", "BTCUSD.m")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)

        // Load profile photo if set
        UserPreferences.getPhotoPath(this)?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val ivHeaderPhoto = findViewById<ImageView>(R.id.ivHeaderPhoto)
                ivHeaderPhoto.setImageBitmap(BitmapFactory.decodeFile(path))
                ivHeaderPhoto.visibility = View.VISIBLE
            }
        }

        // Profile button
        val name = UserPreferences.getName(this)
        if (name.isNotEmpty()) {
            val tvHeaderName = findViewById<TextView>(R.id.tvHeaderName)
            tvHeaderName.text = name
            tvHeaderName.visibility = View.VISIBLE
        }

        findViewById<View>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadAllSignals() }
        loadAllSignals()
    }

    override fun onResume() {
        super.onResume()
        // Refresh profile photo/name after returning from Profile
        UserPreferences.getPhotoPath(this)?.let { path ->
            val f = File(path)
            if (f.exists()) {
                val ivHeaderPhoto = findViewById<ImageView>(R.id.ivHeaderPhoto)
                ivHeaderPhoto.setImageBitmap(BitmapFactory.decodeFile(path))
                ivHeaderPhoto.visibility = View.VISIBLE
            }
        }
        val name = UserPreferences.getName(this)
        val tvHeaderName = findViewById<TextView>(R.id.tvHeaderName)
        tvHeaderName.text = name
        tvHeaderName.visibility = if (name.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadAllSignals() {
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val tvStatus   = findViewById<TextView>(R.id.tvStatus)

        btnRefresh.isEnabled = false
        btnRefresh.text = "⏳  Loading..."
        tvStatus.text = "🔄  Fetching live market data..."
        tvStatus.visibility = View.VISIBLE

        lifecycleScope.launch {
            var ok = 0
            for (sym in SYMBOLS) {
                try {
                    val data = ApiService.getPriceData(sym)
                    if (data != null) {
                        val sig = SignalEngine.generate(data, sym)
                        updateCard(sym, sig)
                        ok++
                    } else {
                        setCardMsg(sym, "⚠  No Data")
                    }
                } catch (e: Exception) {
                    setCardMsg(sym, "⚠  Error")
                }
            }
            btnRefresh.isEnabled = true
            btnRefresh.text = "🔄  Refresh Signals"
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            tvStatus.text = if (ok > 0) "✅  Updated at $time" else "❌  Check internet"
        }
    }

    private fun updateCard(symbol: String, sig: SignalInfo) {
        val ids = getIds(symbol) ?: return
        val dp  = ApiService.decimalPlaces[symbol] ?: 2
        val fmt = DecimalFormat("#,##0.${"0".repeat(dp)}")

        val tvSignal  = findViewById<TextView>(ids[0])
        val tvEntry   = findViewById<TextView>(ids[1])
        val tvSL      = findViewById<TextView>(ids[2])
        val tvTP      = findViewById<TextView>(ids[3])
        val tvMacd    = findViewById<TextView>(ids[4])
        val tvCross   = findViewById<TextView>(ids[5])
        val tvConf    = findViewById<TextView>(ids[6])
        val tvSummary = findViewById<TextView>(ids[7])

        when (sig.type) {
            "BUY" -> {
                tvSignal.text = "▲   B U Y"
                tvSignal.setBackgroundResource(R.drawable.bg_buy_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.buyGreen))
            }
            "SELL" -> {
                tvSignal.text = "▼   S E L L"
                tvSignal.setBackgroundResource(R.drawable.bg_sell_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.sellRed))
            }
            else -> {
                tvSignal.text = "—   W A I T"
                tvSignal.setBackgroundResource(R.drawable.bg_wait_signal)
                tvSignal.setTextColor(ContextCompat.getColor(this, R.color.textGray))
            }
        }

        // Confidence badge
        val confColor = when {
            sig.confidence >= 78 -> R.color.buyGreen
            sig.confidence >= 62 -> R.color.gold
            else -> R.color.textGray
        }
        tvConf.text = "🎯 ${sig.confidence}%  ${sig.signalStrength}"
        tvConf.setTextColor(ContextCompat.getColor(this, confColor))

        if (sig.entry > 0) {
            tvEntry.text   = "📍  Entry:                    ${fmt.format(sig.entry)}"
            tvSL.text      = "🔴  Stop Loss:            ${fmt.format(sig.stopLoss)}"
            tvTP.text      = "🟢  Take Profit (1:1.5): ${fmt.format(sig.takeProfit)}"
            tvMacd.text    = "RSI: ${"%.1f".format(sig.rsiValue)}  |  " +
                             "MACD: ${"%.3f".format(sig.macdValue)}  |  " +
                             "K/D: ${"%.1f".format(sig.stochK)}/${"%.1f".format(sig.stochD)}"
        } else {
            tvEntry.text = "📍  Entry: —"
            tvSL.text    = "🔴  Stop Loss: —"
            tvTP.text    = "🟢  Take Profit: —"
            tvMacd.text  = ""
        }

        tvSummary.text = sig.indicatorSummary
        tvCross.visibility = if (sig.isCrossover) View.VISIBLE else View.GONE
    }

    private fun setCardMsg(symbol: String, msg: String) {
        val ids = getIds(symbol) ?: return
        val tvSignal  = findViewById<TextView>(ids[0])
        val tvEntry   = findViewById<TextView>(ids[1])
        val tvSL      = findViewById<TextView>(ids[2])
        val tvTP      = findViewById<TextView>(ids[3])
        val tvMacd    = findViewById<TextView>(ids[4])
        val tvCross   = findViewById<TextView>(ids[5])
        val tvConf    = findViewById<TextView>(ids[6])
        val tvSummary = findViewById<TextView>(ids[7])

        tvSignal.text = msg
        tvSignal.setBackgroundResource(R.drawable.bg_wait_signal)
        tvSignal.setTextColor(ContextCompat.getColor(this, R.color.textGray))
        tvConf.text = ""; tvEntry.text = "📍  Entry: —"; tvSL.text = "🔴  Stop Loss: —"
        tvTP.text = "🟢  Take Profit: —"; tvMacd.text = ""; tvSummary.text = ""
        tvCross.visibility = View.GONE
    }

    // Returns [signalId, entryId, slId, tpId, macdId, crossId, confId, summaryId]
    private fun getIds(symbol: String): List<Int>? = when (symbol) {
        "XAUUSD.m" -> listOf(
            R.id.tvSignalGold, R.id.tvEntryGold, R.id.tvSLGold, R.id.tvTPGold,
            R.id.tvMacdGold, R.id.tvCrossGold, R.id.tvConfGold, R.id.tvSummaryGold
        )
        "JP225.std" -> listOf(
            R.id.tvSignalJP, R.id.tvEntryJP, R.id.tvSLJP, R.id.tvTPJP,
            R.id.tvMacdJP, R.id.tvCrossJP, R.id.tvConfJP, R.id.tvSummaryJP
        )
        "BTCUSD.m" -> listOf(
            R.id.tvSignalBTC, R.id.tvEntryBTC, R.id.tvSLBTC, R.id.tvTPBTC,
            R.id.tvMacdBTC, R.id.tvCrossBTC, R.id.tvConfBTC, R.id.tvSummaryBTC
        )
        else -> null
    }
}
