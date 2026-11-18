package com.nexusroot.premium

import android.content.Context

/** Per-game preference only. This stores configuration; it does not claim to alter a game's renderer. */
object PixelGraphicsProfile {
    private const val PREFS = "nexus_pixel_graphics_profiles"
    data class Profile(val enabled: Boolean = false, val level: Int = 3)

    fun read(context: Context, packageName: String): Profile {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = packageName.replace('.', '_')
        return Profile(p.getBoolean("${key}_enabled", false), p.getInt("${key}_level", 3).coerceIn(1, 5))
    }

    fun save(context: Context, packageName: String, profile: Profile) {
        val key = packageName.replace('.', '_')
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("${key}_enabled", profile.enabled)
            .putInt("${key}_level", profile.level.coerceIn(1, 5))
            .apply()
    }
}
