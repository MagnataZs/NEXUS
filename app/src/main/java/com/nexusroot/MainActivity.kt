package com.nexusroot

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {
    private val cyan = Color.rgb(0, 205, 255)
    private val blue = Color.rgb(0, 110, 255)
    private val bg = Color.rgb(3, 8, 20)
    private val prefs by lazy { getSharedPreferences("nexus_ui", 0) }
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var lobby = 1
    private val installedGames = mutableListOf<Pair<String,String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lobby = prefs.getInt("lobby", 1)
        window.decorView.systemUiVisibility = 5894 or 1024 or 512
        buildShell()
        refreshStatus()
        loadApps()
    }

    private fun shape(color: Int, stroke: Int = cyan, radius: Float = 16f): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = radius; setStroke(2, stroke) }

    private fun txt(s: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false) =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun button(s: String, action: () -> Unit) = TextView(this).apply {
        text = s; textSize = 12f; gravity = Gravity.CENTER
        setTextColor(Color.WHITE); background = shape(Color.rgb(5, 28, 58), blue, 12f)
        setPadding(12, 12, 12, 12); setOnClickListener { action() }
    }

    private fun buildShell() {
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 8, 12, 8); setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = shape(Color.rgb(4,13,31), blue, 20f); setPadding(14,8,14,8) }
        top.addView(txt("NEXUS ", 23f, Color.WHITE, true))
        top.addView(txt("ROOT", 23f, cyan, true))
        val spacer = Space(this); top.addView(spacer, LinearLayout.LayoutParams(0,1,1f))
        top.addView(txt("GAME SPACE  •  PERFORMANCE MAX", 13f, cyan, true))
        top.addView(Space(this), LinearLayout.LayoutParams(0,1,1f))
        status = txt("CPU --  •  RAM --  •  ROOT verificando", 11f, Color.LTGRAY)
        top.addView(status)
        top.addView(button("⚙") { showSettings() })
        outer.addView(top, LinearLayout.LayoutParams(-1, 58))

        val body = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val nav = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = shape(Color.rgb(4,14,34), blue, 18f); setPadding(8,12,8,12) }
        listOf("🎮  Início", "🚀  Otimização", "▥  Desempenho", "▣  Ferramentas", "◉  Personalizar", "♛  Perfil Gamer").forEachIndexed { i, item ->
            val v = txt(item, 13f, if (i == 0) cyan else Color.LTGRAY, i == 0)
            v.setPadding(12, 15, 8, 15)
            if (i == 0) v.background = shape(Color.rgb(4,45,90), cyan, 12f)
            v.setOnClickListener { if (i == 4) showSettings() else Toast.makeText(this, item.substringAfter("  "), Toast.LENGTH_SHORT).show() }
            nav.addView(v)
        }
        body.addView(nav, LinearLayout.LayoutParams(178, -1))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14,8,4,4) }
        body.addView(content, LinearLayout.LayoutParams(0,-1,1f))
        outer.addView(body, LinearLayout.LayoutParams(-1,0,1f))

        val bottom = LinearLayout(this).apply {
            gravity = Gravity.CENTER; background = shape(Color.rgb(4,13,31), blue, 18f); setPadding(8,7,8,7)
        }
        listOf("⌂  Início", "🎮  Jogos", "🚀  Boost", "∿  Monitor", "⚙  Configurações").forEachIndexed { i, s ->
            val v = txt(s, 12f, if (i == 0) cyan else Color.LTGRAY, true)
            v.gravity = Gravity.CENTER; v.setPadding(12,10,12,10)
            v.setOnClickListener { if (i == 4) showSettings() else Toast.makeText(this,s,Toast.LENGTH_SHORT).show() }
            bottom.addView(v, LinearLayout.LayoutParams(0,-2,1f))
        }
        outer.addView(bottom, LinearLayout.LayoutParams(-1, 54))
        setContentView(outer)
    }

    private fun refreshStatus() {
        Thread {
            val r = RootAccess.run("id")
            runOnUiThread {
                status.text = if (r.ok && r.output.contains("uid=0")) "ROOT MAGISK ✓   •   ${ResolutionManager.displayText()}" else "ROOT não autorizado  •  conceda no Magisk"
            }
        }.start()
    }

    private fun loadApps() {
        content.removeAllViews()
        val heading = txt(if (lobby == 1) "Olá, Gamer!  Prepare-se para uma experiência máxima." else "NEXUS ROOT  •  SUA BIBLIOTECA", 18f, Color.WHITE, true)
        content.addView(heading)
        content.addView(txt(if (lobby == 1) "SEUS JOGOS" else "GAME LIBRARY", 11f, cyan, true))
        val scroll = HorizontalScrollView(this)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        list.forEach { ri ->
            val pkg = ri.activityInfo.packageName
            val label = ri.loadLabel(packageManager).toString()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = shape(Color.rgb(7,20,42), if (label.contains("Free Fire", true)) Color.rgb(255,130,35) else cyan, 18f)
                setPadding(12,12,12,12)
            }
            val icon = ImageView(this).apply { setImageDrawable(ri.loadIcon(packageManager)) }
            card.addView(icon, LinearLayout.LayoutParams(86,86))
            card.addView(txt(label, 14f, Color.WHITE, true))
            card.addView(txt(pkg, 8f, Color.LTGRAY))
            card.addView(button("▶  PLAY") { startGame(pkg, label) }, LinearLayout.LayoutParams(-1,44))
            row.addView(card, LinearLayout.LayoutParams(190, 230).apply { setMargins(6,12,6,12) })
        }
        val add = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = shape(Color.rgb(4,16,36), blue, 18f) }
        add.addView(txt("＋", 46f, cyan, true)); add.addView(txt("Adicionar jogo", 14f, Color.WHITE))
        row.addView(add, LinearLayout.LayoutParams(160,230).apply { setMargins(6,12,6,12) })
        scroll.addView(row); content.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        val metrics = txt("▥  CPU: leitura disponível no sistema     ▣ RAM: leitura do Android     ◉ FPS: requer fonte compatível", 11f, cyan)
        metrics.setPadding(8,12,8,8); content.addView(metrics)
        content.addView(button("ALTERAR ESTILO DA LOBBY  •  ATUAL: $lobby") { showSettings() })
    }

    private fun startGame(pkg: String, label: String) {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Conceda a permissão de sobreposição e toque PLAY novamente.", Toast.LENGTH_LONG).show()
            return
        }
        val appOps = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
        val allowed = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == android.app.AppOpsManager.MODE_ALLOWED
        if (!allowed) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            Toast.makeText(this, "Ative Acesso ao uso e toque PLAY novamente.", Toast.LENGTH_LONG).show()
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(pkg) ?: return
        startForegroundService(Intent(this, GameSessionService::class.java).apply {
            action = GameSessionService.START
            putExtra(GameSessionService.PKG, pkg)
            putExtra(GameSessionService.LABEL, label)
        })
        startActivity(launch)
    }

    private fun showSettings() {
        val choices = arrayOf("Lobby 1 — Gaming Hub (print aprovado)", "Lobby 2 — Estilo alternativo")
        AlertDialog.Builder(this).setTitle("Personalizar NEXUS ROOT")
            .setSingleChoiceItems(choices, lobby - 1) { dialog, which ->
                lobby = which + 1; prefs.edit().putInt("lobby", lobby).apply()
                dialog.dismiss(); loadApps()
            }
            .setNeutralButton("Resolução global") { _, _ -> showResolution() }
            .setPositiveButton("Fechar", null).show()
    }

    private fun showResolution() {
        val levels = arrayOf("Nível 1 • 1080 × 2400", "Nível 2 • 900 × 2000", "Nível 3 • 720 × 1600", "Nível 4 • 600 × 1333", "Nível 5 • 480 × 1067", "Restaurar configuração anterior")
        AlertDialog.Builder(this).setTitle("Resolução / Pixel Graphics")
            .setItems(levels) { _, which ->
                Thread {
                    val ok = if (which == 5) ResolutionManager.restoreGlobal(this)
                    else ResolutionManager.apply(this, which + 1, false)
                    runOnUiThread { Toast.makeText(this, if (ok) "Comando aplicado. Reinicie o jogo se necessário." else "ROOT indisponível ou comando recusado.", Toast.LENGTH_LONG).show(); refreshStatus() }
                }.start()
            }.show()
    }
}
