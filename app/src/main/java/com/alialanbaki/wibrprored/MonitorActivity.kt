package com.alialanbaki.wibrprored

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.TimeUnit

class MonitorActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var tvMonitorState: TextView
    private lateinit var tvSsid: TextView
    private lateinit var tvBssid: TextView
    private lateinit var tvEncryption: TextView
    private lateinit var tvSignal: TextView
    private lateinit var tvCurrentPassword: TextView
    private lateinit var pbTestProgress: ProgressBar
    private lateinit var tvQueueProgress: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvEstimatedTime: TextView
    private lateinit var tvIntervalValue: TextView
    private lateinit var btnPauseResume: Button

    private val candidatePasswords = listOf(
        "12345678", "123456789", "1234567890", "87654321",
        "password", "password123", "admin123", "qwerty123",
        "11223344", "00000000", "11111111", "iraq1234",
        "baghdad123", "wifi1234", "internet123", "abc12345"
    )

    private var currentIndex = 0
    private var isPaused = false

    private val stepRunnable = object : Runnable {
        override fun run() {
            if (isPaused) return

            if (currentIndex >= candidatePasswords.size) {
                tvMonitorState.text = "Test FINISHED!"
                btnPauseResume.isEnabled = false
                updateStatsUi()
                return
            }

            val pwd = candidatePasswords[currentIndex]
            tvCurrentPassword.text = "Password: $pwd"
            pbTestProgress.progress = currentIndex + 1
            currentIndex++

            updateStatsUi()

            val delayMs = SessionState.attemptDelaySeconds.coerceAtLeast(1) * 1000L
            handler.postDelayed(this, delayMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monitor)

        tvMonitorState = findViewById(R.id.tvMonitorState)
        tvSsid = findViewById(R.id.tvSsid)
        tvBssid = findViewById(R.id.tvBssid)
        tvEncryption = findViewById(R.id.tvEncryption)
        tvSignal = findViewById(R.id.tvSignal)
        tvCurrentPassword = findViewById(R.id.tvCurrentPassword)
        pbTestProgress = findViewById(R.id.pbTestProgress)
        tvQueueProgress = findViewById(R.id.tvQueueProgress)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvEstimatedTime = findViewById(R.id.tvEstimatedTime)
        tvIntervalValue = findViewById(R.id.tvIntervalValue)
        btnPauseResume = findViewById(R.id.btnPauseResume)

        val net = SessionState.selectedNetwork
        tvSsid.text = "SSID: ${net?.ssid ?: "-"}"
        tvBssid.text = "BSSID: ${net?.bssid ?: "-"}"
        tvEncryption.text = "Encryption: ${net?.capabilities ?: "-"}"
        val sig = net?.signalLevel ?: 0
        tvSignal.text = if (sig < 40) "Signal level: $sig (weak)" else "Signal level: $sig"

        pbTestProgress.max = candidatePasswords.size
        pbTestProgress.progress = 0

        findViewById<Button>(R.id.btnSpeedMinus).setOnClickListener {
            if (SessionState.attemptDelaySeconds > 1) {
                SessionState.attemptDelaySeconds -= 1
                rescheduleNow()
            }
        }

        findViewById<Button>(R.id.btnSpeedPlus).setOnClickListener {
            if (SessionState.attemptDelaySeconds < 60) {
                SessionState.attemptDelaySeconds += 1
                rescheduleNow()
            }
        }

        btnPauseResume.setOnClickListener {
            if (!isPaused) {
                isPaused = true
                handler.removeCallbacks(stepRunnable)
                tvMonitorState.text = "Test PAUSED!"
                btnPauseResume.text = "Continue"
            } else {
                isPaused = false
                tvMonitorState.text = "Test RUNNING..."
                btnPauseResume.text = "Pause"
                handler.post(stepRunnable)
            }
        }

        updateIntervalLabel()
        updateStatsUi()
        handler.post(stepRunnable)
    }

    private fun rescheduleNow() {
        updateIntervalLabel()
        updateStatsUi()
        if (!isPaused) {
            handler.removeCallbacks(stepRunnable)
            val delayMs = SessionState.attemptDelaySeconds * 1000L
            handler.postDelayed(stepRunnable, delayMs)
        }
    }

    private fun updateIntervalLabel() {
        val sec = SessionState.attemptDelaySeconds
        tvIntervalValue.text = "سرعة المحاولة: $sec ${if (sec == 1) "ثانية" else "ثواني"}"
    }

    private fun updateStatsUi() {
        val total = candidatePasswords.size
        tvQueueProgress.text = "$currentIndex of $total"

        val sec = SessionState.attemptDelaySeconds.coerceAtLeast(1)
        val perMinute = 60 / sec
        tvSpeed.text = "Speed: $perMinute passwords/minute"

        val remaining = (total - currentIndex).coerceAtLeast(0)
        val remainingSeconds = remaining.toLong() * sec.toLong()
        val hours = TimeUnit.SECONDS.toHours(remainingSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds) % 60
        tvEstimatedTime.text = "Estimated time: $hours hr, $minutes min"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(stepRunnable)
    }
}
