# 📈 Forex Robot Signal Bot

**MACD-based Forex Signal App for Android**
Built by Amin Agha Hamidi | 24/09/2026

---

## ✨ Features

| Feature | Details |
|---------|---------|
| 📊 Indicator | MACD (12, 26, 9) — standard settings |
| 🔔 Signals | BUY / SELL based on MACD crossover |
| 🎯 Take Profit | 1:3 Risk-Reward ratio |
| 🛡️ Stop Loss | Based on recent swing high/low (10 bars) |
| 💹 Symbols | XAUUSD.m · JP225.std · BTCUSD.m |
| 🔄 Refresh | Manual refresh button |
| 🔐 Login | Password-protected access |
| 🌙 UI | Dark navy professional design |

---

## 📱 Build APK via GitHub Actions

### Step 1 — Upload to GitHub
1. Create new repo on github.com
2. Upload all these project files
3. Push to `main` branch

### Step 2 — Auto-build
GitHub Actions runs automatically and builds the APK.

### Step 3 — Download APK
1. Go to your repo → **Actions** tab
2. Click the latest workflow run
3. Download **ForexRobot-Debug-APK** artifact
4. Unzip → install `app-debug.apk` on your phone

---

## 📲 Install on Phone

1. On your Android phone: **Settings → Security → Unknown Sources → ON**
   (or "Install unknown apps" in newer Android)
2. Transfer the APK to your phone
3. Tap the APK file to install

---

## 📊 How Signals Work

```
BUY  Signal ▲ = MACD line crosses ABOVE Signal line (bullish crossover)
SELL Signal ▼ = MACD line crosses BELOW Signal line (bearish crossover)
🔔 = Fresh crossover — strongest entry signal
```

**Take Profit & Stop Loss:**
```
BUY  → SL = lowest low (10 bars) | TP = Entry + (Entry - SL) × 3
SELL → SL = highest high (10 bars) | TP = Entry - (SL - Entry) × 3
```

---

## ⚠️ Disclaimer

This app is for informational purposes only. Forex trading involves significant risk.
Always use proper risk management. Past signals do not guarantee future results.

---

## 🔑 Login Password
`Hamidi2026@#AK`
