package com.nexusroot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex

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
        window.decorView.systemUiVisibility = (4 or 1024 or 512 or 4096)
        setContent { NexusPremiumApp() }
    }
}

data class Game(val name: String, val subtitle: String, val art: Int, val accent: Color, val packageName: String)
data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun NexusPremiumApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexus_premium", 0) }
    var lobby by remember { mutableIntStateOf(prefs.getInt("lobby", 1)) }
    var selected by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var overlayPreview by remember { mutableStateOf(false) }
    val games = listOf(
        Game("CALL OF DUTY", "MOBILE  •  TACTICAL", R.drawable.cover_ops, Cyan, "com.activision.callofduty.shooter"),
        Game("MOBILE LEGENDS", "BANG BANG  •  MOBA", R.drawable.cover_moba, Purple, "com.mobile.legends"),
        Game("FREE FIRE", "BATTLE ROYALE", R.drawable.cover_battle, Color(0xFFFF963E), "com.dts.freefireth")
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val compact = maxWidth < 850.dp
        if (lobby == 1) {
            LobbyOne(
                compact, selected, { selected = it }, games,
                onSettings = { settings = true }, onOverlay = { overlayPreview = true }
            )
        } else {
            LobbyTwo(
                compact, selected, { selected = it }, games,
                onSettings = { settings = true }, onOverlay = { overlayPreview = true }
            )
        }
        if (settings) {
            SettingsDialog(lobby, { lobby = it; prefs.edit().putInt("lobby", it).apply() }, { settings = false })
        }
        if (overlayPreview) OverlayPreview { overlayPreview = false }
    }
}

@Composable
fun TopBar(compact: Boolean, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(if (compact) 54.dp else 68.dp)
            .background(Brush.horizontalGradient(listOf(Color(0xFF07182D), Color(0xFF030916), Color(0xFF07182D))), RoundedCornerShape(18.dp))
            .border(1.dp, Brush.horizontalGradient(listOf(Blue, Cyan.copy(.5f), Blue)), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("N", fontSize = if (compact) 28.sp else 36.sp, fontWeight = FontWeight.Black, color = Cyan)
        Column(Modifier.padding(start = 8.dp)) {
            Text("NEXUS ROOT", fontSize = if (compact) 17.sp else 23.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 1.5.sp)
            Text("G A M I N G   H U B", fontSize = 8.sp, color = Muted, letterSpacing = 2.sp)
        }
        Spacer(Modifier.weight(1f))
        Surface(color = Color(0xFF061D36), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Blue)) {
            Text("GAME SPACE  •  PERFORMANCE MAX", Modifier.padding(horizontal = 18.dp, vertical = 9.dp), color = Cyan, fontWeight = FontWeight.Bold, fontSize = if (compact) 9.sp else 12.sp)
        }
        Spacer(Modifier.width(18.dp))
        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                MiniStatus(Icons.Rounded.Thermostat, "37°C", Color(0xFFFFB84D))
                MiniStatus(Icons.Rounded.Memory, "2.8 GHz", Cyan)
                MiniStatus(Icons.Rounded.BatteryFull, "70%", Green)
            }
            Spacer(Modifier.width(18.dp))
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
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LobbyOne(compact: Boolean, selected: Int, onSelect: (Int) -> Unit, games: List<Game>, onSettings: () -> Unit, onOverlay: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        TopBar(compact, onSettings)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavigationRail(selected, onSelect, compact, onSettings)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(if (compact) 42.dp else 56.dp).background(Brush.linearGradient(listOf(Cyan, Blue)), CircleShape).padding(2.dp), contentAlignment = Alignment.Center) {
                        Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("Olá, Gamer!", color = Color.White, fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Bold)
                        Text("Prepare-se para uma experiência máxima.", color = Muted, fontSize = 11.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    ModeChip()
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    games.forEachIndexed { i, game ->
                        GameCard(game, selected == i, { onSelect(i) }, Modifier.weight(1f))
                    }
                    AddGameCard(Modifier.weight(.78f))
                }
                MetricsStrip(compact, onOverlay)
            }
        }
        BottomBar(selected, onSelect, onSettings, onOverlay)
    }
}

@Composable
fun NavigationRail(selected: Int, onSelect: (Int) -> Unit, compact: Boolean, onSettings: () -> Unit) {
    val items = listOf(
        NavItem("Início", Icons.Rounded.SportsEsports), NavItem("Otimização", Icons.Rounded.RocketLaunch),
        NavItem("Desempenho", Icons.Rounded.BarChart), NavItem("Ferramentas", Icons.Rounded.Build),
        NavItem("Personalizar", Icons.Rounded.Palette), NavItem("Perfil Gamer", Icons.Rounded.EmojiEvents)
    )
    Column(
        Modifier.width(if (compact) 132.dp else 172.dp).fillMaxHeight()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A1A31), Color(0xFF050D1B))), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF1266C6), RoundedCornerShape(18.dp)).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items.forEachIndexed { i, item ->
            val active = selected == i
            Row(
                Modifier.fillMaxWidth().height(if (compact) 42.dp else 49.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) Brush.horizontalGradient(listOf(Color(0xFF07569D), Color(0xFF082442))) else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)))
                    .then(if (active) Modifier.border(1.dp, Cyan.copy(.85f), RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { onSelect(i) }.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, null, tint = if (active) Cyan else Muted, modifier = Modifier.size(20.dp))
                Text(item.label, Modifier.padding(start = 9.dp), color = if (active) Color.White else Color(0xFFD3DFEC), fontSize = if (compact) 10.sp else 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("PLAY SMART.\nPLAY MAX.", color = Cyan.copy(.8f), fontSize = 9.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(7.dp))
    }
}

@Composable
fun ModeChip() {
    Row(Modifier.background(Color(0xFF07182C), RoundedCornerShape(14.dp)).border(1.dp, Blue.copy(.7f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFFFC34D), modifier = Modifier.size(20.dp))
        Column(Modifier.padding(horizontal = 8.dp)) {
            Text("MODO ATUAL", color = Muted, fontSize = 8.sp)
            Text("Desempenho", color = Color(0xFFFFC34D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Cyan)
    }
}

@Composable
fun GameCard(game: Game, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp))
            .background(Panel).border(if (active) 2.dp else 1.dp, if (active) Cyan else Color(0xFF174D82), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Image(painterResource(game.art), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xEE020713)))))
            if (active) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(24.dp).background(Cyan, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(17.dp))
                }
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 13.dp, vertical = 12.dp)) {
                Text(game.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = .3.sp)
                Text(game.subtitle, color = game.accent, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 10.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF064A91), Color(0xFF087DDB))))
            .clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text("PLAY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
fun AddGameCard(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().clip(RoundedCornerShape(18.dp))
        .background(Brush.verticalGradient(listOf(Color(0xFF07172D), Color(0xFF030A16))))
        .border(1.dp, Blue.copy(.7f), RoundedCornerShape(18.dp))
        .clickable { }.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.AddCircle, null, tint = Cyan, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(8.dp))
        Text("Adicionar\njogo", color = Color.White, textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricsStrip(compact: Boolean, onOverlay: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(if (compact) 56.dp else 68.dp)
        .background(Brush.horizontalGradient(listOf(Color(0xFF071A30), Color(0xFF030A17), Color(0xFF071A30))), RoundedCornerShape(17.dp))
        .border(1.dp, Color(0xFF1268C7), RoundedCornerShape(17.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
        Metric(Icons.Rounded.ShowChart, "FPS ESTÁVEL", "—", Green)
        Metric(Icons.Rounded.Memory, "USO DA CPU", "—", Cyan)
        Box(Modifier.size(if (compact) 38.dp else 48.dp).background(Brush.linearGradient(listOf(Blue, Color(0xFF052C68))), CircleShape).border(1.dp, Cyan, CircleShape), contentAlignment = Alignment.Center) {
            Text("N", color = Color.White, fontWeight = FontWeight.Black, fontSize = 27.sp)
        }
        Metric(Icons.Rounded.Memory, "USO DA RAM", "—", Purple)
        Metric(Icons.Rounded.Thermostat, "TEMPERATURA", "—", Color(0xFFFFB84D))
    }
}

@Composable
fun Metric(icon: ImageVector, title: String, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Column {
            Text(title, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomBar(selected: Int, onSelect: (Int) -> Unit, onSettings: () -> Unit, onOverlay: () -> Unit) {
    val items = listOf(Triple("Início", Icons.Rounded.Home, 0), Triple("Jogos", Icons.Rounded.SportsEsports, 1),
        Triple("Boost", Icons.Rounded.RocketLaunch, 2), Triple("Monitor", Icons.Rounded.MonitorHeart, 3), Triple("Configurações", Icons.Rounded.Settings, 4))
    Row(Modifier.fillMaxWidth().height(57.dp).background(Color(0xFF061327), RoundedCornerShape(16.dp))
        .border(1.dp, Color(0xFF155DAB), RoundedCornerShape(16.dp)), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (label, icon, index) ->
            val active = selected == index
            Column(Modifier.weight(1f).fillMaxHeight().clickable {
                if (index == 4) onSettings() else if (index == 3) onOverlay() else onSelect(index)
            }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = if (active) Cyan else Muted, modifier = Modifier.size(20.dp))
                Text(label, color = if (active) Cyan else Color(0xFFC0D0E2), fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LobbyTwo(compact: Boolean, selected: Int, onSelect: (Int) -> Unit, games: List<Game>, onSettings: () -> Unit, onOverlay: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TopBar(compact, onSettings)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            NavigationRail(selected, onSelect, compact, onSettings)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("SEU CENTRO DE COMANDO", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("Domine cada partida.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                        Text("Biblioteca, desempenho e ferramentas em um só lugar.", color = Muted, fontSize = 11.sp)
                    }
                    ModeChip()
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    games.forEachIndexed { i, game -> GameCard(game, selected == i, { onSelect(i) }, Modifier.weight(1f)) }
                }
                MetricsStrip(compact, onOverlay)
            }
        }
        BottomBar(selected, onSelect, onSettings, onOverlay)
    }
}

@Composable
fun SettingsDialog(lobby: Int, onLobby: (Int) -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth(.72f).background(Panel, RoundedCornerShape(24.dp)).border(1.dp, Cyan.copy(.7f), RoundedCornerShape(24.dp)).padding(22.dp)) {
            Text("PERSONALIZAR NEXUS ROOT", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.6.sp)
            Text("Estilo da Lobby", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            LobbyChoice(1, "LOBBY 01", "Gaming Hub • cards cinematográficos", lobby == 1) { onLobby(1) }
            Spacer(Modifier.height(10.dp))
            LobbyChoice(2, "LOBBY 02", "Command Center • visual alternativo", lobby == 2) { onLobby(2) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text("FECHAR", color = Cyan) }
            }
        }
    }
}

@Composable
fun LobbyChoice(n: Int, title: String, desc: String, active: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
        .background(if (active) Color(0xFF0B3155) else Color(0xFF0A1526))
        .border(1.dp, if (active) Cyan else Color(0xFF203A58), RoundedCornerShape(15.dp))
        .clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Blue, Cyan)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("0$n", color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = Muted, fontSize = 10.sp)
        }
        if (active) Icon(Icons.Rounded.CheckCircle, null, tint = Cyan)
    }
}

@Composable
fun OverlayPreview(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xAA020611)).padding(16.dp)) {
            // Simulated in-game HUD preview with all four anchored zones.
            Row(Modifier.align(Alignment.CenterStart).fillMaxHeight(.82f).width(190.dp)
                .background(Brush.verticalGradient(listOf(Color(0xF20A1D35), Color(0xF2040B18))), RoundedCornerShape(18.dp))
                .border(1.dp, Cyan, RoundedCornerShape(18.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("NEXUS ROOT", color = Cyan, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("GAME BOOST", color = Muted, fontSize = 9.sp)
                    listOf("⚡  Painel Rápido", "▣  Gráficos", "▥  Desempenho", "⬡  Renderizador", "▦  Pixel Graphics", "🎮  Perfil do Jogo", "⚙  Mais Opções").forEach {
                        Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF102B49), RoundedCornerShape(9.dp)).padding(10.dp))
                    }
                }
            }
            Column(Modifier.align(Alignment.TopCenter).background(Color(0xE908172A), RoundedCornerShape(16.dp)).border(1.dp, Blue, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FPS  119     CPU  42%     GPU  68%     TEMP  37°C", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.align(Alignment.TopEnd).width(240.dp).background(Color(0xF008192D), RoundedCornerShape(17.dp)).border(1.dp, Cyan, RoundedCornerShape(17.dp)).padding(12.dp)) {
                Text("◎  Modo de Jogo", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Desempenho Máximo", color = Color.White, fontSize = 11.sp)
                Divider(Modifier.padding(vertical = 6.dp), color = Blue)
                Text("▣  Gráficos", color = Color.White)
                Text("Pixelização: Nível 5", color = Cyan, fontSize = 11.sp)
                Text("⬡  Renderizador     Automático", color = Color.White, fontSize = 11.sp)
                Text("⬢  FPS Alvo             120 FPS", color = Color.White, fontSize = 11.sp)
            }
            Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.86f).background(Color(0xE908172A), RoundedCornerShape(16.dp)).border(1.dp, Blue, RoundedCornerShape(16.dp)).padding(13.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("🌡 37°C", "▣ 70%", "▦ 2.1 GB", "🚀 Boost", "🎮 Modo Gamer", "⌄ Minimizar").forEach { Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
            }
            TextButton(onClick = onClose, modifier = Modifier.align(Alignment.BottomEnd)) { Text("FECHAR PREVIEW", color = Cyan) }
        }
    }
}
