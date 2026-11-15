package com.example.aiassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: View? = null
    private var panel: View? = null
    private var panelText: TextView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNow()
        if (bubble == null) showBubble()
        return START_STICKY
    }

    private fun startForegroundNow() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("float", "Köməkçi", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "float")
            .setContentTitle("AI Köməkçi işləyir")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun showBubble() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val tv = TextView(this).apply {
            text = "AI"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 18f
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#6750A4"))
            }
        }
        val size = (56 * resources.displayMetrics.density).toInt()
        val lp = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        tv.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = e.rawX; touchY = e.rawY
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - touchX).toInt()
                    val dy = (e.rawY - touchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) moved = true
                    lp.x = startX + dx
                    lp.y = startY + dy
                    wm.updateViewLayout(v, lp)
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) onBubbleTap()
                }
            }
            true
        }
        wm.addView(tv, lp)
        bubble = tv
    }

    private fun onBubbleTap() {
        if (panel != null) {
            closePanel()
            return
        }
        val svc = ScreenReaderService.instance
        if (svc == null) {
            Toast.makeText(
                this,
                "Accessibility ayarlarında 'AI Köməkçi'ni aktiv et",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val key = getSharedPreferences("prefs", MODE_PRIVATE)
            .getString("gemini_key", "") ?: ""
        if (key.isBlank()) {
            Toast.makeText(
                this,
                "Əvvəlcə app-ı açıb API açarını saxla",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val screen = svc.readScreen()
        if (screen.isBlank()) {
            showPanel("Ekranda yazı tapılmadı")
            return
        }
        showPanel("Düşünürəm... (1 dəqiqəyə qədər çəkə bilər)")
        Thread {
            val result = askGemini(key, screen)
            handler.post { panelText?.text = result }
        }.start()
    }

    private fun askGemini(key: String, screen: String): String {
        return try {
            val prompt = "Aşağıda telefon ekranından oxunmuş mətn var. " +
                "Orada görüş, iclas, müştəri, tapşırıq və ya vaxt/tarix varsa, " +
                "onları qısa siyahı ilə çıxar (kim, nə vaxt, harada). " +
                "Heç nə yoxdursa, bunu qısa yaz. Azərbaycan dilində cavab ver.\n\n" +
                "Mətn:\n" + screen.take(3000)
            val body = JSONObject().put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"
            )
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("x-goog-api-key", key)
            c.connectTimeout = 30000
            c.readTimeout = 90000
            c.doOutput = true
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val resp = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                "Xəta $code:\n" + resp.take(300)
            } else {
                JSONObject(resp)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            "Xəta: " + e.message
        }
    }

    private fun showPanel(text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(32, 32, 32, 32)
            setOnClickListener { closePanel() }
        }
        val sv = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#E6000000"))
            addView(tv)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.5).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM }
        wm.addView(sv, lp)
        panel = sv
        panelText = tv
    }

    private fun closePanel() {
        panel?.let { wm.removeView(it) }
        panel = null
        panelText = null
    }

    override fun onDestroy() {
        closePanel()
        bubble?.let { wm.removeView(it) }
        super.onDestroy()
    }
}
