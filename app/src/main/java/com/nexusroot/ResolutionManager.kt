package com.nexusroot

import android.content.Context

object ResolutionManager {
    private const val P = "display_state"
    private const val SIZE = "restore_size"
    private const val DENSITY = "restore_density"
    private const val TEMP = "temporary"

    private fun capture(): Pair<String, String> {
        val s = RootAccess.run("wm size").output
        val d = RootAccess.run("wm density").output
        val so = s.lineSequence().firstOrNull { it.startsWith("Override size:") }?.substringAfter(":")?.trim()
        val dover = d.lineSequence().firstOrNull { it.startsWith("Override density:") }?.substringAfter(":")?.trim()
        return (if (so != null) "wm size $so" else "wm size reset") to
               (if (dover != null) "wm density $dover" else "wm density reset")
    }

    fun displayText(): String {
        val s = RootAccess.run("wm size").output.lineSequence().lastOrNull { it.contains("size:") }?.substringAfter(":")?.trim() ?: "?"
        val d = RootAccess.run("wm density").output.lineSequence().lastOrNull { it.contains("density:") }?.substringAfter(":")?.trim() ?: "?"
        return "$s  •  $d dpi"
    }

    @Synchronized fun apply(context: Context, level: Int, temporary: Boolean): Boolean {
        val presets = mapOf(1 to (1080 to 2400), 2 to (900 to 2000), 3 to (720 to 1600), 4 to (600 to 1333), 5 to (480 to 1067))
        val (w, h) = presets[level] ?: return false
        val prefs = context.getSharedPreferences(P, 0)
        val original = capture()
        if (temporary) {
            prefs.edit().putString("temp_size", original.first).putString("temp_density", original.second).putBoolean(TEMP, true).apply()
        } else if (!prefs.contains(SIZE)) {
            prefs.edit().putString(SIZE, original.first).putString(DENSITY, original.second).apply()
        }
        val result = RootAccess.run("wm size ${w}x${h}")
        if (!result.ok && temporary) prefs.edit().putBoolean(TEMP, false).apply()
        return result.ok
    }

    @Synchronized fun restoreTemporary(context: Context) {
        val p = context.getSharedPreferences(P, 0)
        if (!p.getBoolean(TEMP, false)) return
        val a = RootAccess.run(p.getString("temp_size", "wm size reset") ?: "wm size reset")
        val b = RootAccess.run(p.getString("temp_density", "wm density reset") ?: "wm density reset")
        if (a.ok && b.ok) p.edit().remove("temp_size").remove("temp_density").putBoolean(TEMP, false).apply()
    }

    @Synchronized fun restoreGlobal(context: Context): Boolean {
        val p = context.getSharedPreferences(P, 0)
        val a = RootAccess.run(p.getString(SIZE, "wm size reset") ?: "wm size reset")
        val b = RootAccess.run(p.getString(DENSITY, "wm density reset") ?: "wm density reset")
        if (a.ok && b.ok) {
            p.edit().remove(SIZE).remove(DENSITY).apply()
            return true
        }
        return false
    }
}
