package com.nexusroot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.TextView
import java.util.concurrent.atomic.AtomicBoolean

class GameSessionService : Service() {
    companion object {
        const val ACTION_START = "com.nexusroot.START_SESSION"
        const val EXTRA_PACKAGE = "game_package"
        const val EXTRA_LABEL = "game_label"
        private const val CHANNEL = "nexus_game_session"
        private const val NOTIFICATION_ID = 410
    }

    private var targetPackage = ""
    private var gameLabel = "Jogo"
    private var windowManager: WindowManager? = null
    private var handle: View? = null
    private var panel: View? = null
    private var lastForegroundAt = System.currentTimeMillis()
    private val running = AtomicBoolean(false)
    private var monitor: Thread? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
            gameLabel = intent.getStringExtra(EXTRA_LABEL) ?: "Jogo"
            startForeground(NOTIFICATION_ID, notification())
            addHandle()
            beginMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun beginMonitoring() {
        if (!running.compareAndSet(false, true)) return
        monitor = Thread {
            while (running.get()) {
                val foreground = UsageForeground.currentPackage(this)
                if (foreground == targetPackage) {
                    lastForegroundAt = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - lastForegroundAt > 30_000L) {
                    stopSession()
                    break
                }
                try { Thread.sleep(1500) } catch (_: InterruptedException) { break }
            }
        }.also { it.name = "NexusSessionMonitor"; it.start() }
    }

    private fun addHandle() {
        if (!Settings.canDrawOverlays(this) || handle != null) return
        val view = TextView(this).apply {
            text = "N"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(0, 135, 180))
            setOnTouchListener(DragTouchListener { togglePanel() })
        }
        val params = WindowManager.LayoutParams(
            48, 58,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT; x = 0 }
        try {
            windowManager?.addView(view, params)
            handle = view
        } catch (_: Exception) {}
    }

    private fun togglePanel() {
        if (panel != null) {
            try { windowManager?.removeView(panel) } catch (_: Exception) {}
            panel = null
            return
        }
        val box = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(Color.argb(225, 8, 13, 24))
        }
        val title = TextView(this).apply {
            text = "NEXUS ROOT  •  $gameLabel"
            textSize = 16f
            setTextColor(Color.rgb(0, 220, 255))
        }
        box.addView(title)
        listOf(
            "Resolução temporária" to { ResolutionManager.apply(this, 720, 1600, true); Unit },
            "Restaurar resolução" to { ResolutionManager.restore(this); Unit },
            "Pixel Graphics: experimental / indisponível" to { Unit },
            "Renderizador: controlado pelo jogo" to { Unit },
            "Encerrar sessão" to { stopSession(); Unit }
        ).forEach { (label, action) ->
            val button = android.widget.Button(this).apply {
                text = label
                textSize = 11f
                setOnClickListener { action() }
            }
            box.addView(button)
        }
        val params = WindowManager.LayoutParams(
            310, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT; x = 52 }
        try {
            windowManager?.addView(box, params)
            panel = box
        } catch (_: Exception) {}
    }

    private fun stopSession() {
        if (!running.getAndSet(false)) return
        try { panel?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        try { handle?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        panel = null
        handle = null
        ResolutionManager.restoreTemporaryIfNeeded(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSession()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        running.set(false)
        try { panel?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        try { handle?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        ResolutionManager.restoreTemporaryIfNeeded(this)
        super.onDestroy()
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setContentTitle("NEXUS ROOT")
            .setContentText("Sessão ativa: $gameLabel")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Sessão de jogo", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class DragTouchListener(val click: () -> Unit) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var moved = false
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; moved = false; return true }
                MotionEvent.ACTION_MOVE -> if (kotlin.math.abs(event.rawX-downX) > 12 || kotlin.math.abs(event.rawY-downY) > 12) moved = true
                MotionEvent.ACTION_UP -> { if (!moved) click(); return true }
            }
            return true
        }
    }
}
