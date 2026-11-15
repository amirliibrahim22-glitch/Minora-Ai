package com.example.aiassistant

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val label = TextView(this).apply {
            text = "Gemini API açarı (AIza ilə başlayır):"
        }
        val keyBox = EditText(this).apply {
            hint = "Açarı bura yapışdır"
            setSingleLine(true)
            setText(prefs.getString("gemini_key", ""))
        }
        val saveBtn = Button(this).apply {
            text = "Açarı yadda saxla"
            setOnClickListener {
                prefs.edit().putString("gemini_key", keyBox.text.toString().trim()).apply()
                Toast.makeText(this@MainActivity, "Saxlanıldı", Toast.LENGTH_SHORT).show()
            }
        }
        val startBtn = Button(this).apply {
            text = "Üzən düyməni başlat"
            setOnClickListener { start() }
        }

        layout.addView(label)
        layout.addView(keyBox)
        layout.addView(saveBtn)
        layout.addView(startBtn)
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
