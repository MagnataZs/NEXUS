package com.nexusroot

import android.app.Activity
import android.app.AppOpsManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var rootStatus: TextView
    private lateinit var resolutionStatus: TextView
    private lateinit var gameList: LinearLayout
    private var selectedPackage: String? = null
    private val prefs by lazy { getSharedPreferences("nexus", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        buildUi()
        refreshStatus()
        loadGames()
    }

    private fun buildUi() {
        val base = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.rgb(7, 10, 18))
        }
        val header = TextView(this).apply {
            text = "N E X U S   R O O T"
            textSize = 25f
            setTextColor(Color.rgb(0, 220, 255))
            gravity = Gravity.CENTER_VERTICAL
        }
        base.addView(header, LinearLayout.LayoutParams(-1, 54))
        val subtitle = TextView(this).apply {
            text = "GAME SPACE  •  ROOT CONTROL"
            textSize = 11f
            setTextColor(Color.LTGRAY)
        }
        base.addView(subtitle)
        rootStatus = infoText()
        base.addView(rootStatus)
        resolutionStatus = infoText()
        base.addView(resolutionStatus)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("VERIFICAR ROOT") { checkRoot() })
        actions.addView(actionButton("RESOLUÇÃO") { showResolutionDialog(false) })
        actions.addView(actionButton("PERMISSÕES") { openPermissions() })
        base.addView(actions)

        val title = TextView(this).apply {
            text = "SUA BIBLIOTECA"
            textSize = 17f
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 8)
        }
        base.addView(title)
        val scroll = ScrollView(this)
        gameList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(gameList)
        base.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val footer = TextView(this).apply {
            text = "A detecção só funciona durante uma sessão iniciada pelo PLAY."
            textSize = 10f
            setTextColor(Color.GRAY)
            setPadding(0, 8, 0, 0)
        }
        base.addView(footer)
        setContentView(base)
    }

    private fun infoText() = TextView(this).apply {
        textSize = 12f
        setTextColor(Color.LTGRAY)
        setPadding(0, 7, 0, 4)
    }

    private fun actionButton(label: String, click: () -> Unit) = Button(this).apply {
        text = label
        textSize = 10f
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(17, 54, 72))
        setOnClickListener { click() }
    }

    private fun loadGames() {
        gameList.removeAllViews()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.ROOT) }

        if (activities.isEmpty()) {
            gameList.addView(infoText().apply { text = "Nenhum aplicativo inicializável encontrado." })
            return
        }

        activities.forEach { resolve ->
            val app = resolve.activityInfo.applicationInfo
            val label = resolve.loadLabel(packageManager).toString()
            val pkg = resolve.activityInfo.packageName
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(8, 5, 8, 5)
                setBackgroundColor(Color.rgb(15, 21, 32))
            }
            val icon = ImageView(this).apply {
                setImageDrawable(resolve.loadIcon(packageManager))
            }
            row.addView(icon, LinearLayout.LayoutParams(42, 42))
            val name = TextView(this).apply {
                text = "$label\n$pkg"
                textSize = 11f
                setTextColor(Color.WHITE)
                setPadding(10, 0, 4, 0)
            }
            row.addView(name, LinearLayout.LayoutParams(0, -2, 1f))
            val play = Button(this).apply {
                text = "PLAY"
                textSize = 11f
                setOnClickListener { startGame(pkg) }
            }
            row.addView(play)
            gameList.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = 5
            })
        }
    }

    private fun startGame(pkg: String) {
        selectedPackage = pkg
        if (!hasUsageAccess()) {
            Toast.makeText(this, "Ative Acesso ao uso para detectar o encerramento do jogo.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permita sobreposição para usar o painel lateral.", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        if (launch == null) {
            Toast.makeText(this, "Não foi possível abrir este aplicativo.", Toast.LENGTH_SHORT).show()
            return
        }
        val service = Intent(this, GameSessionService::class.java).apply {
            action = GameSessionService.ACTION_START
            putExtra(GameSessionService.EXTRA_PACKAGE, pkg)
            putExtra(GameSessionService.EXTRA_LABEL, packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)).toString())
        }
        startForegroundService(service)
        startActivity(launch)
    }

    private fun hasUsageAccess(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun openPermissions() {
        if (!hasUsageAccess()) startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        else if (!Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        else Toast.makeText(this, "Permissões principais já concedidas.", Toast.LENGTH_SHORT).show()
    }

    private fun checkRoot() {
        Thread {
            val result = RootAccess.run("id")
            runOnUiThread {
                rootStatus.text = if (result.success && result.output.contains("uid=0"))
                    "ROOT: autorizado pelo Magisk"
                else "ROOT: não autorizado ou indisponível"
            }
        }.start()
    }

    private fun refreshStatus() {
        Thread {
            val state = ResolutionManager.readDisplayState(this)
            runOnUiThread {
                resolutionStatus.text = "Tela atual: ${state.first}  •  densidade: ${state.second}"
            }
        }.start()
    }

    private fun showResolutionDialog(temporary: Boolean) {
        val presets = arrayOf("720x1600", "900x2000", "1080x2400", "Restaurar padrão")
        AlertDialog.Builder(this)
            .setTitle(if (temporary) "Resolução temporária" else "Resolução global")
            .setItems(presets) { _, which ->
                if (which == 3) {
                    Thread { ResolutionManager.restore(this); runOnUiThread { refreshStatus() } }.start()
                } else {
                    val parts = presets[which].split("x")
                    val w = parts[0].toInt()
                    val h = parts[1].toInt()
                    Thread {
                        val ok = ResolutionManager.apply(this, w, h, temporary)
                        runOnUiThread {
                            Toast.makeText(this, if (ok) "Resolução aplicada." else "Falha ao aplicar. Verifique ROOT.", Toast.LENGTH_LONG).show()
                            refreshStatus()
                        }
                    }.start()
                }
            }.show()
    }
}
