package com.alialanbaki.wibrprored

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

data class ScannedNetworkItem(
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val signalLevel: Int
) {
    override fun toString(): String = "$ssid  ($signalLevel%)  [$capabilities]"
}

object SessionState {
    var selectedNetwork: ScannedNetworkItem? = null
    var attemptDelaySeconds: Int = 7
}

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var tvStatusBanner: TextView
    private lateinit var lvNetworks: ListView
    private val items = mutableListOf<ScannedNetworkItem>()
    private lateinit var adapter: ArrayAdapter<ScannedNetworkItem>

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val updated = intent?.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true) ?: true
            loadScanResults(updated)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        tvStatusBanner = findViewById(R.id.tvStatusBanner)
        lvNetworks = findViewById(R.id.lvNetworks)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        lvNetworks.adapter = adapter

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            requestPermissionsAndScan()
        }

        findViewById<Button>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        lvNetworks.setOnItemClickListener { _, _, position, _ ->
            SessionState.selectedNetwork = items[position]
            startActivity(Intent(this, MonitorActivity::class.java))
        }

        requestPermissionsAndScan()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(scanReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(scanReceiver)
        } catch (_: Exception) {
        }
    }

    private fun requestPermissionsAndScan() {
        val required = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
            return
        }

        startWifiScan()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            startWifiScan()
        }
    }

    private fun startWifiScan() {
        if (!wifiManager.isWifiEnabled) {
            tvStatusBanner.text = "الواي فاي مطفأ — فعّل الواي فاي والموقع (GPS) لعرض الشبكات"
            return
        }
        tvStatusBanner.text = "جاري فحص الشبكات المتاحة..."
        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (!started) {
            loadScanResults(false)
        }
    }

    private fun loadScanResults(fresh: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            tvStatusBanner.text = "يرجى منح صلاحية الموقع (GPS) لعرض الشبكات"
            return
        }

        @Suppress("DEPRECATION")
        val results = wifiManager.scanResults.orEmpty()
        items.clear()

        for (res in results) {
            @Suppress("DEPRECATION")
            val ssid = if (res.SSID.isNullOrBlank()) "<Hidden SSID>" else res.SSID
            val levelPercent = WifiManager.calculateSignalLevel(res.level, 100)
            items.add(
                ScannedNetworkItem(
                    ssid = ssid,
                    bssid = res.BSSID ?: "00:00:00:00:00:00",
                    capabilities = res.capabilities ?: "",
                    signalLevel = levelPercent
                )
            )
        }

        adapter.notifyDataSetChanged()
        tvStatusBanner.text = if (items.isEmpty()) {
            "لم يتم العثور على شبكات — تأكد من تفعيل الموقع (GPS) وفصل الشبكة الحالية"
        } else {
            "تم العثور على ${items.size} شبكة (${if (fresh) "تحديث جديد" else "نتائج مخزنة"}) — اختر شبكة لفتح المونتر"
        }
    }
}