package com.nexusroot.premium

import android.content.Context
import java.io.File

/** Centralized display override controller. Global and session overrides share this class. */
object RootResolutionManager {
    data class DisplayState(val size: String?, val density: String?)
    private const val PREFS = "nexus_display"

    fun hasRoot(): Boolean = runRoot("id")?.contains("uid=0") == true

    fun readState(): DisplayState {
        val sizeOut = runRoot("wm size") .orEmpty()
        val densityOut = runRoot("wm density").orEmpty()
        fun override(output: String, kind: String): String? = output.lineSequence()
            .firstOrNull { it.trim().startsWith("Override $kind:", true) }
            ?.substringAfter(':')?.trim()
        return DisplayState(override(sizeOut, "size"), override(densityOut, "density"))
    }

    fun apply(context: Context, width: Int, height: Int, density: Int? = null, temporary: Boolean): Boolean {
        if (width !in setOf(480, 540, 640, 720, 900, 1080) || height !in setOf(1066, 1200, 1422, 1600, 2000, 2400)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (temporary && !prefs.getBoolean("session_saved", false)) {
            val original = readState()
            prefs.edit().putString("session_size", original.size).putString("session_density", original.density).putBoolean("session_saved", true).apply()
        }
        val commands = mutableListOf("wm size ${width}x${height}")
        if (density != null && density in 160..480) commands += "wm density $density"
        return runRoot(commands.joinToString(" && "))?.contains("ERROR", true) != true
    }

    fun restoreSession(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("session_saved", false)) return true
        val size = prefs.getString("session_size", null)
        val density = prefs.getString("session_density", null)
        val commands = mutableListOf(if (size.isNullOrBlank()) "wm size reset" else "wm size $size")
        commands += if (density.isNullOrBlank()) "wm density reset" else "wm density $density"
        val ok = runRoot(commands.joinToString(" && "))?.contains("ERROR", true) != true
        if (ok) prefs.edit().remove("session_size").remove("session_density").putBoolean("session_saved", false).apply()
        return ok
    }

    fun applyGlobal(context: Context, width: Int, height: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("global_saved", false)) {
            val original = readState()
            prefs.edit().putString("global_size", original.size).putString("global_density", original.density).putBoolean("global_saved", true).apply()
        }
        return apply(context, width, height, temporary = false)
    }

    fun restoreGlobal(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean("global_saved", false)) return true
        val size = prefs.getString("global_size", null)
        val density = prefs.getString("global_density", null)
        val commands = mutableListOf(if (size.isNullOrBlank()) "wm size reset" else "wm size $size")
        commands += if (density.isNullOrBlank()) "wm density reset" else "wm density $density"
        val ok = runRoot(commands.joinToString(" && "))?.contains("ERROR", true) != true
        if (ok) prefs.edit().remove("global_size").remove("global_density").putBoolean("global_saved", false).apply()
        return ok
    }

    private fun runRoot(command: String): String? = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() == 0) output else "ERROR: $output"
    } catch (e: Exception) { "ERROR: ${e.message}" }
}
