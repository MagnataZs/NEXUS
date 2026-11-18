package com.nexusroot.premium

import android.app.*
import android.app.ActivityManager
import android.os.BatteryManager
import android.content.IntentFilter
import java.io.File
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
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
import android.widget.*
import androidx.core.app.NotificationCompat

/** Session-scoped overlay. It exists only after the user presses PLAY in NEXUS ROOT. */
class GameSessionService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var gamePackage = ""
    private var gameLabel = "Jogo"
    private var overlay: View? = null
    private var windowManager: WindowManager? = null
    private var lastSeen = System.currentTimeMillis()
    private var sessionStart = System.currentTimeMillis()
    private var collapsed = false
    private var metricsView: TextView? = null
    private var previousCpu: LongArray? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        gamePackage = intent?.getStringExtra(EXTRA_PACKAGE).orEmpty()
        gameLabel = intent?.getStringExtra(EXTRA_LABEL) ?: gamePackage
        if (gamePackage.isBlank()) { stopSelf(); return START_NOT_STICKY }
        createChannel()
        startForeground(NOTIFICATION_ID, notification("Sessão ativa: $gameLabel"))
        if (Settings.canDrawOverlays(this)) showOverlay() else updateNotification("Permita sobreposição para exibir o painel")
        lastSeen = System.currentTimeMillis()
        sessionStart = System.currentTimeMillis()
        handler.removeCallbacks(monitor)
        handler.postDelayed(monitor, 3000)
        handler.post(metricsTicker)
        return START_NOT_STICKY
    }

    private val metricsTicker = object : Runnable {
        override fun run() {
            try {
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val mem = ActivityManager.MemoryInfo(); am.getMemoryInfo(mem)
                val used = (mem.totalMem - mem.availMem) / (1024 * 1024)
                val total = mem.totalMem / (1024 * 1024)
                val now = readCpu()
                val cpu = if (now != null && previousCpu != null) {
                    val totalDelta = now[0] - previousCpu!![0]
                    val idleDelta = now[1] - previousCpu!![1]
                    if (totalDelta > 0) ((totalDelta - idleDelta) * 100 / totalDelta).coerceIn(0, 100) else null
                } else null
                previousCpu = now
                metricsView?.text = "CPU: ${cpu?.let { "$it%" } ?: "N/D"} • RAM: ${used}/${total} MB • FPS/GPU: N/D"
            } catch (_: Exception) { metricsView?.text = "CPU/RAM: N/D • FPS/GPU: N/D" }
            handler.postDelayed(this, 2000)
        }
    }

    private fun readCpu(): LongArray? = try {
        val p = File("/proc/stat").readLines().first().trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        longArrayOf(p.sum(), p[3] + (p.getOrNull(4) ?: 0L))
    } catch (_: Exception) { null }

    private val monitor = object : Runnable {
        override fun run() {
            if (!hasUsageAccess()) {
                updateNotification("Sessão ativa. Ative Acesso a dados de uso para detectar o fechamento do jogo.")
                handler.postDelayed(this, 8000)
                return
            }
            val foreground = foregroundPackageSince(sessionStart)
            if (foreground == gamePackage) lastSeen = System.currentTimeMillis()
            // Give Android transitions and short app switches a grace period.
            if (System.currentTimeMillis() - lastSeen > 25000) {
                finishSession("Jogo encerrado")
                return
            }
            handler.postDelayed(this, 3000)
        }
    }

    private fun foregroundPackageSince(begin: Long): String? {
        return try {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = usm.queryEvents(begin, System.currentTimeMillis())
            val event = UsageEvents.Event()
            var current: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> current = event.packageName
                    UsageEvents.Event.MOVE_TO_BACKGROUND -> if (current == event.packageName) current = null
                    else -> if (Build.VERSION.SDK_INT >= 29) {
                        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) current = event.packageName
                        if ((event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.ACTIVITY_STOPPED) && current == event.packageName) current = null
                    }
                }
            }
            current
        } catch (_: Exception) { null }
    }

    private fun hasUsageAccess(): Boolean = try {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), packageName) == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    private fun showOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(12), dp(13), dp(12))
            background = panelBg(0xF20A1729.toInt(), 0xFF10BFFF.toInt(), 18)
        }
        val header = TextView(this).apply {
            text = "NEXUS ROOT  •  TOQUE PARA ABRIR / ARRASTE"
            setTextColor(0xFF20D2FF.toInt()); textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(9))
        }
        root.addView(header)
        val metrics = TextView(this).apply { text = "CPU: lendo • RAM: lendo • FPS/GPU: N/D"; setTextColor(0xFFD7E6F7.toInt()); textSize = 10f }
        metricsView = metrics
        root.addView(metrics)
        val scroll = ScrollView(this)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        controls.addView(section("PAINEL RÁPIDO"))
        controls.addView(action("Resolução temporária 720p") { RootResolutionManager.apply(this, 720, 1600, temporary = true); toast("Resolução temporária solicitada") })
        controls.addView(action("Resolução temporária 540p") { RootResolutionManager.apply(this, 540, 1200, temporary = true); toast("Resolução temporária solicitada") })
        controls.addView(action("Restaurar resolução anterior") { toast(if (RootResolutionManager.restoreSession(this)) "Resolução restaurada" else "Falha ao restaurar") })
        val pixelProfile = PixelGraphicsProfile.read(this, gamePackage)
        controls.addView(section("PIXEL GRAPHICS • PERFIL DESTE JOGO"))
        controls.addView(action(if (pixelProfile.enabled) "Perfil salvo: ATIVO • Nível ${pixelProfile.level}" else "Pixel Graphics não adicionado a este jogo" ) {
            toast(if (pixelProfile.enabled) "Perfil por jogo salvo. Aplicação visual depende de suporte específico do jogo/driver." else "Abra a biblioteca, toque no ícone de ajustes do jogo e adicione Pixel Graphics.")
        })
        controls.addView(action("Configurar perfil Pixel Graphics") {
            val settings = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(settings)
            toast("Configure pelo botão de ajustes no card de ${gameLabel}")
        })
        controls.addView(action("Renderizador: controlado pelo jogo") { toast("OpenGL/Vulkan não pode ser forçado universalmente") })
        controls.addView(action("Encerrar sessão") { finishSession("Sessão encerrada pelo usuário") })
        scroll.addView(controls)
        root.addView(scroll, LinearLayout.LayoutParams(-1, dp(300)))
        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            dp(238), WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT; x = dp(8); y = 0 }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        var moved = false
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX; val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) > dp(5) || kotlin.math.abs(dy) > dp(5)) moved = true
                    params.x = startX + dx.toInt(); params.y = startY + dy.toInt()
                    try { windowManager?.updateViewLayout(root, params) } catch (_: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> { if (!moved) toggleCollapsed(root); true }
                else -> true
            }
        }
        try { windowManager?.addView(root, params); overlay = root } catch (_: Exception) { overlay = null }
    }

    private fun toggleCollapsed(root: LinearLayout) {
        collapsed = !collapsed
        for (i in 1 until root.childCount) root.getChildAt(i).visibility = if (collapsed) View.GONE else View.VISIBLE
    }

    private fun section(label: String) = TextView(this).apply {
        text = label; setTextColor(0xFF1CCBFF.toInt()); textSize = 10f; typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(11), 0, dp(4))
    }
    private fun action(label: String, click: () -> Unit) = TextView(this).apply {
        text = label; setTextColor(0xFFF1F6FC.toInt()); textSize = 12f; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(10), dp(8), dp(10)); background = panelBg(0xFF11263D.toInt(), 0x553B9DCE, 10)
        val lp = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) }; layoutParams = lp
        setOnClickListener { click() }
    }
    private fun panelBg(color: Int, stroke: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun finishSession(message: String) {
        handler.removeCallbacks(monitor)
        handler.removeCallbacks(metricsTicker)
        RootResolutionManager.restoreSession(this)
        removeOverlay()
        updateNotification(message)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    private fun removeOverlay() { try { overlay?.let { windowManager?.removeView(it) } } catch (_: Exception) {}; overlay = null }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "NEXUS ROOT Game Space", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("NEXUS ROOT").setContentText(text)
        .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()
    private fun updateNotification(text: String) { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text)) }

    override fun onDestroy() {
        handler.removeCallbacks(monitor)
        handler.removeCallbacks(metricsTicker)
        RootResolutionManager.restoreSession(this)
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PACKAGE = "game_package"
        const val EXTRA_LABEL = "game_label"
        private const val CHANNEL = "nexus_game_session"
        private const val NOTIFICATION_ID = 4102

        fun start(context: Context, packageName: String, label: String) {
            val intent = Intent(context, GameSessionService::class.java).putExtra(EXTRA_PACKAGE, packageName).putExtra(EXTRA_LABEL, label)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
