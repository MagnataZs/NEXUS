package com.nexusroot

import android.content.Context

object ResolutionManager {
    private const val PREFS = "nexus_resolution"
    private const val KEY_GLOBAL_SIZE_CMD = "global_size_restore"
    private const val KEY_GLOBAL_DENSITY_CMD = "global_density_restore"
    private const val KEY_TEMP_SIZE_CMD = "temp_size_restore"
    private const val KEY_TEMP_DENSITY_CMD = "temp_density_restore"
    private const val KEY_TEMP_ACTIVE = "temporary_active"

    private data class DisplaySnapshot(
        val sizeLabel: String,
        val densityLabel: String,
        val sizeRestoreCommand: String,
        val densityRestoreCommand: String
    )

    private fun snapshot(): DisplaySnapshot {
        val sizeOutput = RootAccess.run("wm size").output
        val densityOutput = RootAccess.run("wm density").output
        val sizeOverride = sizeOutput.lineSequence().firstOrNull { it.startsWith("Override size:") }
            ?.substringAfter(":")?.trim()
        val physicalSize = sizeOutput.lineSequence().firstOrNull { it.startsWith("Physical size:") }
            ?.substringAfter(":")?.trim()
        val densityOverride = densityOutput.lineSequence().firstOrNull { it.startsWith("Override density:") }
            ?.substringAfter(":")?.trim()
        val physicalDensity = densityOutput.lineSequence().firstOrNull { it.startsWith("Physical density:") }
            ?.substringAfter(":")?.trim()

        return DisplaySnapshot(
            sizeOverride ?: physicalSize ?: "indisponível",
            densityOverride ?: physicalDensity ?: "indisponível",
            if (sizeOverride != null) "wm size $sizeOverride" else "wm size reset",
            if (densityOverride != null) "wm density $densityOverride" else "wm density reset"
        )
    }

    fun readDisplayState(context: Context): Pair<String, String> {
        val state = snapshot()
        return state.sizeLabel to state.densityLabel
    }

    @Synchronized
    fun apply(context: Context, width: Int, height: Int, temporary: Boolean): Boolean {
        // Presets allowlisted: no arbitrary shell input is accepted.
        val allowed = setOf(720 to 1600, 900 to 2000, 1080 to 2400)
        if ((width to height) !in allowed) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = snapshot()

        if (temporary) {
            prefs.edit()
                .putString(KEY_TEMP_SIZE_CMD, state.sizeRestoreCommand)
                .putString(KEY_TEMP_DENSITY_CMD, state.densityRestoreCommand)
                .putBoolean(KEY_TEMP_ACTIVE, true)
                .apply()
        } else if (!prefs.contains(KEY_GLOBAL_SIZE_CMD)) {
            prefs.edit()
                .putString(KEY_GLOBAL_SIZE_CMD, state.sizeRestoreCommand)
                .putString(KEY_GLOBAL_DENSITY_CMD, state.densityRestoreCommand)
                .apply()
        }

        val result = RootAccess.run("wm size ${width}x${height}")
        if (!result.success && temporary) {
            prefs.edit().remove(KEY_TEMP_SIZE_CMD).remove(KEY_TEMP_DENSITY_CMD)
                .putBoolean(KEY_TEMP_ACTIVE, false).apply()
        }
        return result.success
    }

    @Synchronized
    fun restore(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sizeCmd = prefs.getString(KEY_GLOBAL_SIZE_CMD, null) ?: "wm size reset"
        val densityCmd = prefs.getString(KEY_GLOBAL_DENSITY_CMD, null) ?: "wm density reset"
        val a = RootAccess.run(sizeCmd)
        val b = RootAccess.run(densityCmd)
        if (a.success && b.success) {
            prefs.edit().remove(KEY_GLOBAL_SIZE_CMD).remove(KEY_GLOBAL_DENSITY_CMD).apply()
            return true
        }
        return false
    }

    @Synchronized
    fun restoreTemporaryIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_TEMP_ACTIVE, false)) return
        val sizeCmd = prefs.getString(KEY_TEMP_SIZE_CMD, "wm size reset") ?: "wm size reset"
        val densityCmd = prefs.getString(KEY_TEMP_DENSITY_CMD, "wm density reset") ?: "wm density reset"
        val a = RootAccess.run(sizeCmd)
        val b = RootAccess.run(densityCmd)
        if (a.success && b.success) {
            prefs.edit().remove(KEY_TEMP_SIZE_CMD).remove(KEY_TEMP_DENSITY_CMD)
                .putBoolean(KEY_TEMP_ACTIVE, false).apply()
        }
    }
}
