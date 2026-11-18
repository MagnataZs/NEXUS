package com.nexusroot

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

object UsageForeground {
    fun currentPackage(context: Context): String? {
        return try {
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val events = manager.queryEvents(end - 15_000L, end)
            val event = UsageEvents.Event()
            var foreground: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (android.os.Build.VERSION.SDK_INT >= 29 &&
                     event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)) {
                    foreground = event.packageName
                }
            }
            foreground
        } catch (_: Exception) { null }
    }
}
