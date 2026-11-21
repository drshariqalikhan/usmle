package com.step2ck.scanquiz.debug

import androidx.compose.runtime.mutableStateListOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    val entries = mutableStateListOf<String>()

    init {
        log("SYS", "App started. Logger ready.")
    }

    fun log(tag: String, message: String) {
        val time = timeFormat.format(Date())
        val formatted = "[$time][$tag] $message"
        entries.add(formatted)
        if (entries.size > 300) {
            entries.removeAt(0)
        }
        android.util.Log.d(tag, message)
    }

    fun d(tag: String, message: String) = log(tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val errStr = if (throwable != null) "$message (${throwable.javaClass.simpleName}: ${throwable.message})" else message
        log("ERROR/$tag", errStr)
        if (throwable != null) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    fun getAllLogs(): String = entries.joinToString("\n")

    fun clear() {
        entries.clear()
        log("SYS", "Logs cleared by user")
    }
}

