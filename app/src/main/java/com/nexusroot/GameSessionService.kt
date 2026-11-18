package com.nexusroot

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import java.util.concurrent.atomic.AtomicBoolean

class GameSessionService : Service() {
    companion object {
        const val START = "nexus.start"
        const val PKG = "target_package"
        const val LABEL = "target_label"
        private const val CHANNEL = "nexus_session"
    }
    private var target = ""
    private var label = "Game"
    private var wm: WindowManager? = null
    private var leftMenu: View? = null
    private var quickPanel: View? = null
    private var topHud: View? = null
    private var bottomHud: View? = null
    private val active = AtomicBoolean(false)
    private var lastSeen = System.currentTimeMillis()
    private var pixelLevel = 1

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "NEXUS ROOT sessão", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == START) {
            target = intent.getStringExtra(PKG) ?: ""
            label = intent.getStringExtra(LABEL) ?: "Game"
            val n = Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("NEXUS ROOT").setContentText("Overlay ativo: $label").setOngoing(true).build()
            startForeground(81, n)
            createHud()
            monitor()
        }
        return START_NOT_STICKY
    }

    private fun lp(w: Int, h: Int, gravity: Int, x: Int = 0, y: Int = 0) =
        WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { this.gravity = gravity; this.x = x; this.y = y }

    private fun panelBg() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.argb(224, 3, 12, 29)); cornerRadius = 22f; setStroke(2, Color.rgb(0,170,255))
    }

    private fun label(text: String, size: Float = 13f, color: Int = Color.WHITE) =
        TextView(this).apply { this.text = text; textSize = size; setTextColor(color); setPadding(10,9,10,9) }

    private fun action(text: String, click: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; setTextColor(Color.WHITE); setPadding(12,13,12,13)
        setOnClickListener { click() }
    }

    private fun createHud() {
        if (!Settings.canDrawOverlays(this)) return
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = panelBg(); setPadding(5,8,5,8)
        }
        left.addView(label("NEXUS ROOT", 18f, Color.rgb(0,210,255)))
        left.addView(label("GAME BOOST", 9f, Color.LTGRAY))
        val menuItems = listOf("⚡  Painel Rápido", "▣  Gráficos", "▥  Desempenho", "⬡  Renderizador", "▦  Pixel Graphics", "🎮  Perfil do Jogo", "⚙  Mais Opções")
        menuItems.forEachIndexed { index, item ->
            val v = action(item) {
                when (index) {
                    0 -> toggleQuick()
                    1 -> showGraphics()
                    2 -> showPerformance()
                    3 -> Toast.makeText(this, "Renderizador é definido pelo próprio jogo/driver.", Toast.LENGTH_LONG).show()
                    4 -> showPixel()
                    5 -> showProfiles()
                    6 -> showMore()
                }
            }
            left.addView(v)
        }
        leftMenu = left
        add(left, lp(dp(190), WindowManager.LayoutParams.WRAP_CONTENT, Gravity.LEFT or Gravity.CENTER_VERTICAL, dp(6)))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; background = panelBg(); gravity = Gravity.CENTER
        }
        top.addView(label("FPS  --", 13f, Color.GREEN))
        top.addView(label("CPU  --", 13f, Color.CYAN))
        top.addView(label("GPU  --", 13f, Color.MAGENTA))
        top.addView(label("TEMP  --", 13f, Color.YELLOW))
        topHud = top
        add(top, lp(dp(380), dp(54), Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, dp(5)))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; background = panelBg()
        }
        listOf("🌡 --°C", "▣ Bateria --", "▦ RAM --", "🚀 Boost", "🎮 Modo Gamer", "⌄ Minimizar").forEach { s ->
            bottom.addView(action(s) { if (s.contains("Minimizar")) hideHud() else Toast.makeText(this, "Métrica depende de suporte do aparelho.", Toast.LENGTH_SHORT).show() })
        }
        bottomHud = bottom
        add(bottom, lp(WindowManager.LayoutParams.MATCH_PARENT, dp(56), Gravity.BOTTOM, 0, dp(3)))
        toggleQuick()
    }

    private fun add(v: View, p: WindowManager.LayoutParams) {
        try { wm?.addView(v,p) } catch (_: Exception) {}
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toggleQuick() {
        if (quickPanel != null) {
            try { wm?.removeView(quickPanel) } catch (_: Exception) {}
            quickPanel = null; return
        }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = panelBg(); setPadding(8,8,8,8) }
        box.addView(label("◎  Modo de Jogo", 15f, Color.CYAN))
        box.addView(action("Desempenho Máximo  ›") { showProfiles() })
        box.addView(label("▣  Gráficos", 14f))
        box.addView(action("Pixelização: Nível $pixelLevel  ▰▰▰▰▰") { showPixel() })
        box.addView(action("⬡  Renderizador  •  Automático  ›") { Toast.makeText(this,"O jogo controla OpenGL/Vulkan.",Toast.LENGTH_LONG).show() })
        box.addView(action("⬢  FPS Alvo  •  padrão do jogo") { Toast.makeText(this,"Não é possível forçar FPS universalmente.",Toast.LENGTH_LONG).show() })
        quickPanel = box
        add(box, lp(dp(235), WindowManager.LayoutParams.WRAP_CONTENT, Gravity.RIGHT or Gravity.TOP, dp(7), dp(68)))
    }

    private fun showPixel() {
        val options = arrayOf("Nível 1 • leve", "Nível 2 • moderado", "Nível 3 • forte", "Nível 4 • extremo", "Nível 5 • blocos grandes")
        AlertDialog.Builder(this).setTitle("PIXEL GRAPHICS")
            .setSingleChoiceItems(options, pixelLevel - 1) { dialog, which ->
                pixelLevel = which + 1
                // Resolution scaling is a display-wide approximation, not a per-game texture filter.
                Thread {
                    ResolutionManager.apply(this, pixelLevel, true)
                    Handler(Looper.getMainLooper()).post {
                        dialog.dismiss()
                        toggleQuickIfOpen()
                        Toast.makeText(this, "Nível $pixelLevel selecionado. O efeito usa escala de resolução global.", Toast.LENGTH_LONG).show()
                    }
                }.start()
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun toggleQuickIfOpen() {
        if (quickPanel != null) { try { wm?.removeView(quickPanel) } catch (_: Exception) {}; quickPanel = null; toggleQuick() }
    }
    private fun showGraphics() = showPixel()
    private fun showPerformance() = AlertDialog.Builder(this).setTitle("Desempenho")
        .setItems(arrayOf("Equilibrado", "Desempenho", "Economia")) { _, which ->
            Toast.makeText(this, "Perfil ${arrayOf("Equilibrado","Desempenho","Economia")[which]} selecionado. Ajustes dependem do kernel.", Toast.LENGTH_LONG).show()
        }.show()
    private fun showProfiles() = AlertDialog.Builder(this).setTitle("Perfil do jogo")
        .setItems(arrayOf("Desempenho máximo", "Equilibrado", "Economia de bateria")) { _, which ->
            Toast.makeText(this, "Perfil selecionado. Nem todo kernel permite aplicar ajustes de CPU/GPU.", Toast.LENGTH_LONG).show()
        }.show()
    private fun showMore() = AlertDialog.Builder(this).setTitle("Mais opções")
        .setItems(arrayOf("Restaurar resolução", "Encerrar sessão")) { _, which ->
            if (which == 0) Thread { ResolutionManager.restoreTemporary(this) }.start() else stopSession()
        }.show()

    private fun hideHud() {
        listOf(leftMenu, quickPanel, topHud, bottomHud).forEach { v -> try { if (v != null) wm?.removeView(v) } catch (_: Exception) {} }
        leftMenu = null; quickPanel = null; topHud = null; bottomHud = null
    }

    private fun monitor() {
        if (!active.compareAndSet(false,true)) return
        Thread {
            while (active.get()) {
                val current = currentForeground()
                if (current == target) lastSeen = System.currentTimeMillis()
                else if (System.currentTimeMillis() - lastSeen > 35_000) { stopSession(); break }
                try { Thread.sleep(1800) } catch (_: InterruptedException) { break }
            }
        }.start()
    }

    private fun currentForeground(): String? = try {
        val usm = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val events = usm.queryEvents(end - 12_000, end)
        val e = UsageEvents.Event()
        var pkg: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                (Build.VERSION.SDK_INT >= 29 && e.eventType == UsageEvents.Event.ACTIVITY_RESUMED)) pkg = e.packageName
        }
        pkg
    } catch (_: Exception) { null }

    private fun stopSession() {
        if (!active.getAndSet(false)) return
        hideHud()
        Thread { ResolutionManager.restoreTemporary(this) }.start()
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
    }
    override fun onTaskRemoved(rootIntent: Intent?) { stopSession(); super.onTaskRemoved(rootIntent) }
    override fun onDestroy() { active.set(false); hideHud(); ResolutionManager.restoreTemporary(this); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
