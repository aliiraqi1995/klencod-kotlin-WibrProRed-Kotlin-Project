package com.alialanbaki.wibrprored

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val tvHelpContent: TextView = findViewById(R.id.tvHelpContent)
        val rawHtml = getString(R.string.help_html)
        tvHelpContent.text = HtmlCompat.fromHtml(rawHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
}
