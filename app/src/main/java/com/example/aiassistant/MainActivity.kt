package com.example.aiassistant

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }
        val btn = Button(this).apply {
            text = "Üzən düyməni başlat"
            setOnClickListener { start() }
        }
        layout.addView(btn)
        setContentView(layout)
    }

    private fun start() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            Toast.makeText(this, "İcazə ver, geri qayıt və yenidən bas", Toast.LENGTH_LONG).show()
            return
        }
        startForegroundService(Intent(this, FloatingService::class.java))
    }
}
