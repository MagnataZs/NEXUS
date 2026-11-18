package com.nexusroot.premium

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import android.app.ActivityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

private val Ink = Color(0xFF030712)
private val Panel = Color(0xFF081426)
private val Panel2 = Color(0xFF0B1D34)
private val Cyan = Color(0xFF16C8FF)
private val Blue = Color(0xFF087BFF)
private val Muted = Color(0xFF91A8C2)
private val Green = Color(0xFF48E58A)
private val Purple = Color(0xFFB66BFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (4 or 1024 or 512 or 4096)
        setContent { NexusPremiumApp() }
    }
}

data class InstalledGame(val label: String, val packageName: String, val icon: Drawable)
data class DeviceMetrics(val cpu: Int? = null, val ramUsedMb: Long? = null, val ramTotalMb: Long? = null, val battery: Int? = null, val tempC: Float? = null)
data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun NexusPremiumApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexus_premium", Context.MODE_PRIVATE) }
    val selectedPackages = remember { mutableStateListOf<String>().apply { addAll(prefs.getStringSet("games", emptySet()) ?: emptySet()) } }
    var lobby by remember { mutableIntStateOf(prefs.getInt("lobby", 1)) }
    var selectedNav by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showGamePicker by remember { mutableStateOf(false) }
    var pixelConfigGame by remember { mutableStateOf<InstalledGame?>(null) }
    val installedGames by produceState(initialValue = emptyList<InstalledGame>(), key1 = context) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    val metrics by produceState(initialValue = DeviceMetrics(), key1 = context) {
        var previousCpu = readCpuCounters()
        while (true) {
            value = withContext(Dispatchers.IO) { readDeviceMetrics(context, previousCpu) }
            previousCpu = readCpuCounters()
            delay(2000)
        }
    }
    val games = installedGames.filter { selectedPackages.contains(it.packageName) }

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val compact = maxWidth < 850.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TopBar(compact, metrics) { showSettings = true }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NavigationRail(selectedNav, { selectedNav = it }, compact)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    WelcomeHeader(lobby)
                    if (selectedNav == 0 || selectedNav == 1) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (games.isEmpty()) {
                                EmptyLibrary(Modifier.weight(1f)) { showGamePicker = true }
                            } else {
                                games.take(4).forEach { game ->
                                    InstalledGameCard(game, Modifier.weight(1f), lobby = lobby, onPlay = { startGameSession(context, game.packageName, game.label) }, onPixelConfig = { pixelConfigGame = game }, onRemove = {
                                        selectedPackages.remove(game.packageName)
                                        prefs.edit().putStringSet("games", selectedPackages.toSet()).apply()
                                    })
                                }
                                if (games.size < 4) AddGameCard(Modifier.weight(.8f)) { showGamePicker = true }
                            }
                        }
                    } else {
                        FeaturePanel(selectedNav, metrics, Modifier.weight(1f))
                    }
                    MetricsStrip(metrics, compact)
                }
            }
            BottomBar(selectedNav, { selectedNav = it }, { showSettings = true }, { selectedNav = 2 })
        }
        if (showSettings) SettingsDialog(lobby, { lobby = it; prefs.edit().putInt("lobby", it).apply() }, { showSettings = false })
        pixelConfigGame?.let { game -> PixelGraphicsConfigDialog(game, onClose = { pixelConfigGame = null }) }
        if (showGamePicker) GamePickerDialog(installedGames, selectedPackages.toSet(), { pkg, add ->
            if (add && !selectedPackages.contains(pkg)) selectedPackages.add(pkg)
            if (!add) selectedPackages.remove(pkg)
            prefs.edit().putStringSet("games", selectedPackages.toSet()).apply()
        }, { showGamePicker = false })
    }
}

private fun loadLaunchableApps(context: Context): List<InstalledGame> {
    val pm = context.packageManager
    @Suppress("DEPRECATION")
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    return apps.asSequence()
        .filter { it.packageName != context.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
        .mapNotNull { info -> try { InstalledGame(pm.getApplicationLabel(info).toString(), info.packageName, pm.getApplicationIcon(info)) } catch (_: Exception) { null } }
        .sortedBy { it.label.lowercase() }
        .toList()
}

private fun startGameSession(context: Context, packageName: String, label: String) {
    if (!Settings.canDrawOverlays(context)) {
        Toast.makeText(context, "Ative Permitir overlay nas configurações e toque em PLAY novamente", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    val usageAllowed = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        @Suppress("DEPRECATION")
        ops.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName) == android.app.AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
    if (!usageAllowed) {
        Toast.makeText(context, "Ative Acesso a dados de uso para detectar quando o jogo fechar", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    GameSessionService.start(context, packageName, label)
    launchGame(context, packageName)
}

private fun launchGame(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    if (intent != null) context.startActivity(intent)
}

private fun readCpuCounters(): LongArray? = try {
    val parts = File("/proc/stat").readLines().first().trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
    longArrayOf(parts.sum(), parts[3] + (parts.getOrNull(4) ?: 0L))
} catch (_: Exception) { null }

private fun readDeviceMetrics(context: Context, previous: LongArray?): DeviceMetrics {
    val now = readCpuCounters()
    val cpu = if (now != null && previous != null) {
        val total = now[0] - previous[0]
        val idle = now[1] - previous[1]
        if (total > 0) ((total - idle).toDouble() / total * 100).roundToInt().coerceIn(0, 100) else null
    } else null
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mem)
    val totalMb = mem.totalMem / (1024 * 1024)
    val usedMb = (mem.totalMem - mem.availMem) / (1024 * 1024)
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    return DeviceMetrics(cpu, usedMb, totalMb, if (level >= 0 && scale > 0) (level * 100f / scale).roundToInt() else null, if (temp >= 0) temp / 10f else null)
}

@Composable
fun TopBar(compact: Boolean, metrics: DeviceMetrics, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (compact) 54.dp else 68.dp)
        .background(Brush.horizontalGradient(listOf(Color(0xFF07182D), Color(0xFF030916), Color(0xFF07182D))), RoundedCornerShape(18.dp))
        .border(1.dp, Brush.horizontalGradient(listOf(Blue, Cyan.copy(.5f), Blue)), RoundedCornerShape(18.dp)).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("N", fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Black, color = Cyan)
        Column(Modifier.padding(start = 8.dp)) {
            Text("NEXUS ROOT", fontSize = if (compact) 17.sp else 23.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
            Text("G A M I N G   H U B", fontSize = 8.sp, color = Muted, letterSpacing = 2.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = Color(0xFF061D36), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Blue)) {
            Text("GAME SPACE  •  PERFORMANCE MAX", Modifier.padding(horizontal = 18.dp, vertical = 9.dp), color = Cyan, fontWeight = FontWeight.Bold, fontSize = if (compact) 9.sp else 12.sp)
        }
        Spacer(Modifier.width(16.dp))
        if (!compact) {
            MiniStatus(Icons.Rounded.Thermostat, metrics.tempC?.let { "%.1f°C".format(it) } ?: "—", Color(0xFFFFB84D))
            Spacer(Modifier.width(12.dp))
            MiniStatus(Icons.Rounded.Memory, metrics.cpu?.let { "$it% CPU" } ?: "CPU —", Cyan)
            Spacer(Modifier.width(12.dp))
            MiniStatus(Icons.Rounded.BatteryFull, metrics.battery?.let { "$it%" } ?: "—", Green)
            Spacer(Modifier.width(12.dp))
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(42.dp).background(Panel2, CircleShape).border(1.dp, Blue, CircleShape)) {
            Icon(Icons.Rounded.Settings, null, tint = Color.White)
        }
    }
}

@Composable
fun MiniStatus(icon: ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WelcomeHeader(lobby: Int) {
    Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).background(Brush.linearGradient(listOf(Cyan, Blue)), CircleShape), contentAlignment = Alignment.Center) {
            Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text(if (lobby == 1) "Olá, Gamer!" else "CENTRO DE COMANDO", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (lobby == 1) "Sua biblioteca. Seu ritmo. Seu jogo." else "Controle sua experiência de jogo em um só lugar.", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.background(Color(0xFF07182C), RoundedCornerShape(14.dp)).border(1.dp, Blue.copy(.7f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFFFC34D), modifier = Modifier.size(20.dp))
            Column(Modifier.padding(horizontal = 8.dp)) {
                Text("MODO ATUAL", color = Muted, fontSize = 8.sp)
                Text("Desempenho", color = Color(0xFFFFC34D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Cyan)
        }
    }
}

@Composable
fun NavigationRail(selected: Int, onSelect: (Int) -> Unit, compact: Boolean) {
    val items = listOf(NavItem("Início", Icons.Rounded.Home), NavItem("Otimização", Icons.Rounded.RocketLaunch), NavItem("Desempenho", Icons.Rounded.QueryStats), NavItem("Ferramentas", Icons.Rounded.Build), NavItem("Personalizar", Icons.Rounded.Palette), NavItem("Perfil Gamer", Icons.Rounded.EmojiEvents))
    Column(Modifier.width(if (compact) 132.dp else 172.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(Color(0xFF0A1A31), Color(0xFF050D1B))), RoundedCornerShape(18.dp)).border(1.dp, Color(0xFF1266C6), RoundedCornerShape(18.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        items.forEachIndexed { i, item ->
            val active = selected == i
            Row(Modifier.fillMaxWidth().height(if (compact) 42.dp else 49.dp).clip(RoundedCornerShape(12.dp))
                .background(if (active) Brush.horizontalGradient(listOf(Color(0xFF07569D), Color(0xFF082442))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                .then(if (active) Modifier.border(1.dp, Cyan.copy(.85f), RoundedCornerShape(12.dp)) else Modifier)
                .clickable { onSelect(i) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, null, tint = if (active) Cyan else Muted, modifier = Modifier.size(20.dp))
                Text(item.label, Modifier.padding(start = 9.dp), color = if (active) Color.White else Color(0xFFD3DFEC), fontSize = if (compact) 10.sp else 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("PLAY SMART.\nPLAY MAX.", color = Cyan.copy(.8f), fontSize = 9.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(7.dp))
    }
}

@Composable
fun InstalledGameCard(game: InstalledGame, modifier: Modifier = Modifier, lobby: Int = 1, onPlay: () -> Unit, onPixelConfig: () -> Unit, onRemove: () -> Unit) {
    val bitmap = remember(game.packageName) { drawableToBitmap(game.icon) }
    val accent = if (lobby == 1) Cyan else Purple
    val cardTop = if (lobby == 1) Color(0xFF102C4C) else Color(0xFF25133F)
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, accent.copy(.75f), RoundedCornerShape(18.dp))) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Brush.verticalGradient(listOf(cardTop, Color(0xFF050B17))))) {
            Row(Modifier.align(Alignment.TopEnd).padding(7.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xCC07365A)).border(1.dp, Cyan.copy(.65f), CircleShape).clickable(onClick = onPixelConfig), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, null, tint = Cyan, modifier = Modifier.size(16.dp))
                }
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xAA07172A)).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, null, tint = Muted, modifier = Modifier.size(16.dp))
                }
            }
            Column(Modifier.align(Alignment.Center).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap.asImageBitmap(), null, Modifier.size(64.dp).clip(RoundedCornerShape(17.dp)), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(12.dp))
                Text(if (lobby == 1) game.label.uppercase() else "SLOT  •  ${game.label.uppercase()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(game.packageName, color = accent.copy(.85f), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(if (lobby == 1) listOf(Color(0xFF064A91), Color(0xFF087DDB)) else listOf(Color(0xFF54259A), Color(0xFF1466B8)))).clickable(onClick = onPlay), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("INICIAR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun PixelGraphicsConfigDialog(game: InstalledGame, onClose: () -> Unit) {
    val context = LocalContext.current
    val current = remember(game.packageName) { PixelGraphicsProfile.read(context, game.packageName) }
    var enabled by remember(game.packageName) { mutableStateOf(current.enabled) }
    var level by remember(game.packageName) { mutableFloatStateOf(current.level.toFloat()) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.62f).background(Panel, RoundedCornerShape(22.dp)).border(1.dp, Cyan.copy(.8f), RoundedCornerShape(22.dp)).padding(22.dp)) {
            Text("PIXEL GRAPHICS • PERFIL POR JOGO", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(game.label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
            Text(game.packageName, color = Muted, fontSize = 9.sp)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Adicionar Pixel Graphics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Perfil exclusivo deste jogo", color = Muted, fontSize = 10.sp)
                }
                Switch(checked = enabled, onCheckedChange = { enabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Cyan))
            }
            Spacer(Modifier.height(12.dp))
            Text("INTENSIDADE  •  NÍVEL ${level.toInt()}", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Slider(value = level, onValueChange = { level = it }, valueRange = 1f..5f, steps = 3, enabled = enabled, colors = SliderDefaults.colors(thumbColor = Cyan, activeTrackColor = Blue))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Suave", color = Muted, fontSize = 9.sp); Text("Extremo", color = Muted, fontSize = 9.sp)
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF17263A)).padding(12.dp)) {
                Text("COMPATIBILIDADE", color = Color(0xFFFFC56B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("O perfil fica salvo e só é associado a este pacote. O Android não oferece uma API universal para pixelizar somente a imagem de qualquer jogo. A aplicação visual real depende de integração compatível com o jogo/driver; este controle não altera a resolução do sistema nem afeta outros apps.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextButton(onClick = onClose) { Text("CANCELAR", color = Muted) }
                Button(onClick = { PixelGraphicsProfile.save(context, game.packageName, PixelGraphicsProfile.Profile(enabled, level.toInt())); onClose() }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("SALVAR PERFIL") }
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(256)
    val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(256)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun EmptyLibrary(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF0B2440), Color(0xFF050C19)))).border(1.dp, Blue, RoundedCornerShape(18.dp)).clickable(onClick = onAdd).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(62.dp).background(Color(0xFF073D69), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = Cyan, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(14.dp))
        Text("SUA BIBLIOTECA ESTÁ VAZIA", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
        Text("Adicione os jogos instalados no aparelho para iniciar por aqui.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp, bottom = 15.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Blue), shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Rounded.LibraryAdd, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("ADICIONAR JOGOS")
        }
    }
}

@Composable
fun AddGameCard(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp)).background(Brush.verticalGradient(listOf(Color(0xFF07172D), Color(0xFF030A16)))).border(1.dp, Blue.copy(.7f), RoundedCornerShape(18.dp)).clickable(onClick = onAdd).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.AddCircle, null, tint = Cyan, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(8.dp))
        Text("Adicionar\njogo", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GamePickerDialog(apps: List<InstalledGame>, selected: Set<String>, onToggle: (String, Boolean) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.78f).fillMaxHeight(.88f).background(Panel, RoundedCornerShape(22.dp)).border(1.dp, Cyan.copy(.75f), RoundedCornerShape(22.dp)).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("BIBLIOTECA DE APLICATIVOS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Adicionar jogos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null, tint = Muted) }
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), placeholder = { Text("Buscar aplicativo ou pacote...", color = Muted) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Cyan) }, singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Cyan, unfocusedBorderColor = Color(0xFF31506D)))
            Text("${selected.size} selecionado(s)  •  ${filtered.size} aplicativo(s) encontrado(s)", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 6.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(filtered, key = { it.packageName }) { app ->
                    val bitmap = remember(app.packageName) { drawableToBitmap(app.icon) }
                    val checked = selected.contains(app.packageName)
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (checked) Color(0xFF0C2B48) else Color(0xFF0A1728)).clickable { onToggle(app.packageName, !checked) }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(bitmap.asImageBitmap(), null, Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(app.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Checkbox(checked = checked, onCheckedChange = { onToggle(app.packageName, it) }, colors = CheckboxDefaults.colors(checkedColor = Cyan, uncheckedColor = Muted))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.End) {
                Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("CONCLUIR") }
            }
        }
    }
}

@Composable
fun MetricsStrip(metrics: DeviceMetrics, compact: Boolean) {
    Row(Modifier.fillMaxWidth().height(if (compact) 58.dp else 70.dp).background(Brush.horizontalGradient(listOf(Color(0xFF071A30), Color(0xFF030A17), Color(0xFF071A30))), RoundedCornerShape(17.dp)).border(1.dp, Color(0xFF1268C7), RoundedCornerShape(17.dp)).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        Metric(Icons.Rounded.QueryStats, "CPU", metrics.cpu?.let { "$it%" } ?: "N/D", Green)
        Metric(Icons.Rounded.Memory, "RAM", if (metrics.ramUsedMb != null && metrics.ramTotalMb != null) "${metrics.ramUsedMb}/${metrics.ramTotalMb} MB" else "N/D", Cyan)
        Box(Modifier.size(if (compact) 38.dp else 48.dp).background(Brush.linearGradient(listOf(Blue, Color(0xFF052C68))), CircleShape).border(1.dp, Cyan, CircleShape), contentAlignment = Alignment.Center) { Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 27.sp) }
        Metric(Icons.Rounded.BatteryFull, "BATERIA", metrics.battery?.let { "$it%" } ?: "N/D", Purple)
        Metric(Icons.Rounded.Thermostat, "TEMP. BATERIA", metrics.tempC?.let { "%.1f°C".format(it) } ?: "N/D", Color(0xFFFFB84D))
    }
}

@Composable
fun Metric(icon: ImageVector, title: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(23.dp))
        Column {
            Text(title, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FeaturePanel(selected: Int, metrics: DeviceMetrics, modifier: Modifier = Modifier) {
    val title = when (selected) { 1 -> "Otimização"; 2 -> "Desempenho"; 3 -> "Ferramentas"; 4 -> "Personalização"; else -> "Perfil Gamer" }
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, Blue.copy(.65f), RoundedCornerShape(18.dp)).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(title.uppercase(), color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Painel de $title", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Leituras disponíveis do dispositivo", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        Text("CPU: ${metrics.cpu?.let { "$it%" } ?: "não disponível"}", color = Color.White)
        Text("RAM: ${metrics.ramUsedMb?.let { "$it MB usados" } ?: "não disponível"}", color = Color.White)
        Text("Bateria: ${metrics.battery?.let { "$it%" } ?: "não disponível"}", color = Color.White)
        Text("FPS/GPU: não expostos por uma API universal do Android", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit, onSettings: () -> Unit, onOverlay: () -> Unit) {
    val items = listOf(Triple("Início", Icons.Rounded.Home, 0), Triple("Jogos", Icons.Rounded.SportsEsports, 1), Triple("Boost", Icons.Rounded.RocketLaunch, 2), Triple("Monitor", Icons.Rounded.MonitorHeart, 3), Triple("Configurações", Icons.Rounded.Settings, 4))
    Row(Modifier.fillMaxWidth().height(56.dp).background(Color(0xFF061327), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF155DAB), RoundedCornerShape(16.dp)), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, icon, index) ->
            val active = selected == index
            Column(Modifier.weight(1f).fillMaxHeight().clickable { if (index == 4) onSettings() else if (index == 3) onOverlay() else onSelect(index) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = if (active) Cyan else Muted, modifier = Modifier.size(20.dp))
                Text(label, color = if (active) Cyan else Color(0xFFC0D0E2), fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
fun SettingsDialog(lobby: Int, onLobby: (Int) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.72f).background(Panel, RoundedCornerShape(24.dp)).border(1.dp, Cyan.copy(.7f), RoundedCornerShape(24.dp)).padding(22.dp)) {
            Text("PERSONALIZAR NEXUS ROOT", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
            Text("Estilo da Lobby", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            LobbyChoice(1, "LOBBY 01", "Gaming Hub • cards de jogos", lobby == 1) { onLobby(1) }
            Spacer(Modifier.height(10.dp))
            LobbyChoice(2, "LOBBY 02", "Command Center • visual alternativo", lobby == 2) { onLobby(2) }
            Spacer(Modifier.height(14.dp))
            Text("RESOLUÇÃO GLOBAL (ROOT)", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Altera a resolução do display inteiro. O valor original pode ser restaurado abaixo.", color = Muted, fontSize = 10.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = { val ok = RootResolutionManager.applyGlobal(context, 1080, 2400); Toast.makeText(context, if (ok) "Comando enviado" else "Falha ROOT", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("1080p") }
                Button(onClick = { val ok = RootResolutionManager.applyGlobal(context, 720, 1600); Toast.makeText(context, if (ok) "Comando enviado" else "Falha ROOT", Toast.LENGTH_SHORT).show() }, colors = ButtonDefaults.buttonColors(containerColor = Blue)) { Text("720p") }
                OutlinedButton(onClick = { val ok = RootResolutionManager.restoreGlobal(context); Toast.makeText(context, if (ok) "Resolução restaurada" else "Falha ao restaurar", Toast.LENGTH_SHORT).show() }) { Text("Restaurar") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Permitir overlay") }
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Acesso de uso") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onClose) { Text("FECHAR", color = Cyan) } }
        }
    }
}

@Composable
fun LobbyChoice(n: Int, title: String, desc: String, active: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(if (active) Color(0xFF0B3155) else Color(0xFF0A1526)).border(1.dp, if (active) Cyan else Color(0xFF203A58), RoundedCornerShape(15.dp)).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Blue, Cyan)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("0$n", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(desc, color = Muted, fontSize = 10.sp) }
        if (active) Icon(Icons.Rounded.CheckCircle, null, tint = Cyan)
    }
}

@Composable
fun OverlayPreview(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xAA020611)).padding(16.dp)) {
            Column(Modifier.align(Alignment.CenterStart).fillMaxHeight(.82f).width(205.dp).background(Color(0xF208192D), RoundedCornerShape(18.dp)).border(1.dp, Cyan, RoundedCornerShape(18.dp)).padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NEXUS ROOT", color = Cyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("GAME BOOST", color = Muted, fontSize = 9.sp)
                OverlayItem(Icons.Rounded.Bolt, "Painel rápido")
                OverlayItem(Icons.Rounded.Tune, "Gráficos")
                OverlayItem(Icons.Rounded.QueryStats, "Desempenho")
                OverlayItem(Icons.Rounded.ViewInAr, "Renderizador")
                OverlayItem(Icons.Rounded.GridView, "Pixel Graphics")
                OverlayItem(Icons.Rounded.SportsEsports, "Perfil do jogo")
                OverlayItem(Icons.Rounded.Settings, "Mais opções")
            }
            Column(Modifier.align(Alignment.TopCenter).background(Color(0xE908172A), RoundedCornerShape(16.dp)).border(1.dp, Blue, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FPS  N/D     CPU  N/D     GPU  N/D     TEMP  N/D", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.align(Alignment.TopEnd).width(240.dp).background(Color(0xF008192D), RoundedCornerShape(17.dp)).border(1.dp, Cyan, RoundedCornerShape(17.dp)).padding(12.dp)) {
                Text("Modo de Jogo", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Gráficos  •  Pixelização: indisponível", color = Color.White, fontSize = 11.sp)
                Text("Renderizador  •  controlado pelo jogo", color = Color.White, fontSize = 11.sp)
                Text("FPS alvo  •  controlado pelo jogo", color = Color.White, fontSize = 11.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.86f).background(Color(0xE908172A), RoundedCornerShape(16.dp)).border(1.dp, Blue, RoundedCornerShape(16.dp)).padding(13.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                OverlayItem(Icons.Rounded.Thermostat, "Temp.")
                OverlayItem(Icons.Rounded.BatteryFull, "Bateria")
                OverlayItem(Icons.Rounded.Memory, "RAM")
                OverlayItem(Icons.Rounded.RocketLaunch, "Boost")
                OverlayItem(Icons.Rounded.ExpandMore, "Minimizar")
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.BottomEnd)) { Text("FECHAR PREVIEW", color = Cyan) }
        }
    }
}

@Composable
fun OverlayItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
