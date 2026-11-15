package com.example.aiassistant

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderService : AccessibilityService() {

    companion object {
        var instance: ScreenReaderService? = null
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    fun readScreen(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        collect(root, sb)
        return sb.toString().trim()
    }

    private fun collect(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        val t = node.text?.toString()
        if (!t.isNullOrBlank()) sb.append(t).append("\n")
        for (i in 0 until node.childCount) {
            collect(node.getChild(i), sb)
        }
    }
}
