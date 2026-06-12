package com.meta.portal.sampleapp.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.portal.sampleapp.data.GameRecord
import com.meta.portal.sampleapp.data.GameStorage
import com.meta.portal.sampleapp.model.GameMode
import com.meta.portal.sampleapp.model.PlayerSetup
import com.meta.portal.sampleapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// === Stats data ===
data class PlayerStats(
    val name: String,
    val wins: Int,
    val losses: Int,
    val totalGames: Int,
    val winRate: Float,
    val currentStreak: Int,       // positive = win streak, negative = loss streak
    val bestStreak: Int,
    val avgWinnerLife: Float,      // avg remaining life when they won
    val recentForm: String,        // "W W L W L" last 5
)

data class DashboardStats(
    val totalGames: Int,
    val standardGames: Int,
    val commanderGames: Int,
    val avgDuration: Int,           // seconds
    val playerStats: List<PlayerStats>,
    val headToHead: Map<Pair<String, String>, Int>, // (winner, loser) -> count
    val longestGame: Int,           // seconds
    val shortestGame: Int,
)

fun computeStats(games: List<GameRecord>): DashboardStats {
    val total = games.size
    val standard = games.count { it.gameMode == "Standard" }
    val commander = games.count { it.gameMode == "Commander" }

    val durations = games.map { it.durationSeconds }.filter { it > 0 }
    val avgDur = if (durations.isNotEmpty()) durations.average().toInt() else 0
    val longest = durations.maxOrNull() ?: 0
    val shortest = durations.minOrNull() ?: 0

    // Collect all player names
    val allNames = games.flatMap { it.players.map { p -> p.name } }.toSet()

    // Win/loss tracking per player
    val winsPerPlayer = mutableMapOf<String, Int>()
    val lossesPerPlayer = mutableMapOf<String, Int>()
    val winLifeTotals = mutableMapOf<String, MutableList<Int>>()
    val h2h = mutableMapOf<Pair<String, String>, Int>()

    // Track streaks — ordered by time
    val playerGames = mutableMapOf<String, MutableList<Boolean>>() // true=win

    games.forEach { game ->
        val winner = game.winnerName
        winsPerPlayer[winner] = (winsPerPlayer[winner] ?: 0) + 1
        winLifeTotals.getOrPut(winner) { mutableListOf() }.add(game.players.find { it.name == winner }?.life ?: 0)

        game.players.forEach { p ->
            if (p.name != winner) {
                lossesPerPlayer[p.name] = (lossesPerPlayer[p.name] ?: 0) + 1
                h2h[winner to p.name] = (h2h[winner to p.name] ?: 0) + 1
            }
            playerGames.getOrPut(p.name) { mutableListOf() }.add(p.name == winner)
        }
    }

    val playerStats = allNames.map { name ->
        val wins = winsPerPlayer[name] ?: 0
        val losses = lossesPerPlayer[name] ?: 0
        val results = playerGames[name] ?: emptyList()

        // Current streak
        var currentStreak = 0
        if (results.isNotEmpty()) {
            val last = results.last()
            currentStreak = if (last) {
                results.takeLastWhile { it }.size
            } else {
                -results.takeLastWhile { !it }.size
            }
        }

        // Best win streak
        var bestStreak = 0
        var run = 0
        for (r in results) {
            if (r) { run++; bestStreak = maxOf(bestStreak, run) } else { run = 0 }
        }

        val avgLife = winLifeTotals[name]?.average()?.toFloat() ?: 0f

        // Recent form (last 5)
        val recent = results.takeLast(5).map { if (it) "W" else "L" }.reversed()

        PlayerStats(
            name = name,
            wins = wins,
            losses = losses,
            totalGames = wins + losses,
            winRate = if (wins + losses > 0) wins.toFloat() / (wins + losses) else 0f,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            avgWinnerLife = avgLife,
            recentForm = recent.joinToString(" "),
        )
    }.sortedByDescending { it.wins }

    return DashboardStats(
        totalGames = total,
        standardGames = standard,
        commanderGames = commander,
        avgDuration = avgDur,
        playerStats = playerStats,
        headToHead = h2h,
        longestGame = longest,
        shortestGame = shortest,
    )
}

@Composable
fun GameSetupScreen(
    gameStorage: GameStorage,
    lastSetups: List<PlayerSetup>,
    lastPlayerCount: Int,
    lastGameMode: GameMode,
    lastStartingLife: Int,
    onStartGame: (playerCount: Int, startingLife: Int, gameMode: GameMode, setups: List<PlayerSetup>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var playerCount by remember { mutableStateOf(lastPlayerCount) }
    var gameMode by remember { mutableStateOf(lastGameMode) }
    var startingLife by remember { mutableStateOf(lastStartingLife) }
    var showCustomLifeDialog by remember { mutableStateOf(false) }
    var customLifeInput by remember { mutableStateOf("40") }
    var savedNames by remember { mutableStateOf(gameStorage.getPlayerNames()) }

    // Pre-fill from last game
    val playerSetups = remember(lastSetups) {
        mutableStateListOf<PlayerSetup>().also { list ->
            repeat(4) { i -> list.add(lastSetups.getOrNull(i) ?: PlayerSetup(colorIndex = i)) }
        }
    }

    val allGames = remember { gameStorage.getGameHistory() }
    val recentGames = allGames.take(2)
    val stats = remember(allGames) { computeStats(allGames) }

    LaunchedEffect(gameMode) {
        startingLife = when (gameMode) {
            GameMode.STANDARD -> 20
            GameMode.COMMANDER -> 40
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⚔ Portal Counters", style = MaterialTheme.typography.headlineSmall, color = MetaBlue)

        if (allGames.isNotEmpty()) {
            StatsDashboard(stats = stats, recentGames = recentGames)
        }

        Spacer(modifier = Modifier.height(2.dp))

        SetupRow(label = "Players") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(2, 3, 4).forEach { count ->
                    Button(onClick = { playerCount = count }, modifier = Modifier.height(48.dp),
                        colors = btnColors(playerCount == count), shape = RoundedCornerShape(8.dp)
                    ) { Text("$count", fontSize = 18.sp) }
                }
            }
        }

        SetupRow(label = "Mode") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameMode.entries.forEach { mode ->
                    Button(onClick = { gameMode = mode }, modifier = Modifier.height(48.dp),
                        colors = btnColors(gameMode == mode), shape = RoundedCornerShape(8.dp)
                    ) { Text(mode.label, fontSize = 16.sp) }
                }
            }
        }

        SetupRow(label = "Life") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val opts = when (gameMode) { GameMode.STANDARD -> listOf(20, 25, 30); GameMode.COMMANDER -> listOf(30, 40, 50) }
                opts.forEach { life ->
                    Button(onClick = { startingLife = life }, modifier = Modifier.height(48.dp),
                        colors = btnColors(startingLife == life), shape = RoundedCornerShape(8.dp)) { Text("$life", fontSize = 18.sp) }
                }
                Button(onClick = { showCustomLifeDialog = true }, modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceMid, contentColor = ContentOnDark),
                    shape = RoundedCornerShape(8.dp)) { Text("Custom", fontSize = 14.sp) }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        repeat(playerCount) { index ->
            PlayerNameRow(
                index = index, setup = playerSetups[index], savedNames = savedNames,
                onSetupChange = { playerSetups[index] = it },
                onAddName = { name -> gameStorage.savePlayerName(name); savedNames = gameStorage.getPlayerNames() },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                val setups = (0 until playerCount).map { i ->
                    playerSetups[i].copy(name = playerSetups[i].name.ifBlank { "Player ${i + 1}" })
                }
                onStartGame(playerCount, startingLife, gameMode, setups)
            },
            modifier = Modifier.height(56.dp).width(260.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MetaBlue, contentColor = OnMetaBlue),
            shape = RoundedCornerShape(12.dp),
        ) { Text("⚔ Start Game", style = MaterialTheme.typography.titleMedium) }
    }

    if (showCustomLifeDialog) {
        AlertDialog(containerColor = SurfaceDark, onDismissRequest = { showCustomLifeDialog = false },
            title = { Text("Set Starting Life", color = ContentOnDark) },
            text = {
                OutlinedTextField(value = customLifeInput, onValueChange = { customLifeInput = it }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ContentOnDark, unfocusedTextColor = ContentOnDark,
                        focusedContainerColor = SurfaceMid, unfocusedContainerColor = SurfaceMid))
            },
            confirmButton = { TextButton(onClick = { customLifeInput.toIntOrNull()?.let { startingLife = it }; showCustomLifeDialog = false }) { Text("OK", color = MetaBlue) } },
            dismissButton = { TextButton(onClick = { showCustomLifeDialog = false }) { Text("Cancel", color = ContentOnDark) } },
        )
    }
}

@Composable
private fun btnColors(selected: Boolean) = if (selected) {
    ButtonDefaults.buttonColors(containerColor = MetaBlue, contentColor = OnMetaBlue)
} else {
    ButtonDefaults.buttonColors(containerColor = SurfaceMid, contentColor = ContentOnDark)
}

@Composable
private fun StatsDashboard(stats: DashboardStats, recentGames: List<GameRecord>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, SurfaceMid),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            // === COMPACT OVERVIEW ROW ===
            val avgMin = stats.avgDuration / 60
            val avgSec = stats.avgDuration % 60
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatChip("⚔ ${stats.totalGames}", "total", Modifier.weight(1f))
                StatChip("🎯 ${stats.standardGames}S/${stats.commanderGames}C", "modes", Modifier.weight(1f))
                StatChip("⏱ ${avgMin}:${String.format("%02d", avgSec)}", "avg", Modifier.weight(1f))
            }

            // === COMPACT LEADERBOARD — one line per player ===
            if (stats.playerStats.isNotEmpty()) {
                stats.playerStats.forEach { ps ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .background(SurfaceMid, RoundedCornerShape(5.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        // Name
                        Text(ps.name, fontSize = 16.sp, color = ContentOnDark, fontWeight = FontWeight.Medium,
                            maxLines = 1, modifier = Modifier.width(90.dp))
                        // Streak badge (compact)
                        when {
                            ps.currentStreak >= 3 -> Text("🔥${ps.currentStreak} ", fontSize = 14.sp, color = MtgGreen)
                            ps.currentStreak >= 1 -> Text("↑${ps.currentStreak} ", fontSize = 13.sp, color = MtgGreen)
                            ps.currentStreak <= -2 -> Text("↓${kotlin.math.abs(ps.currentStreak)} ", fontSize = 13.sp, color = MtgRed)
                            else -> {}
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        // Stats right-aligned
                        // Colored W/L form
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            ps.recentForm.split(" ").forEach { ch ->
                                if (ch == "W") Text("W", fontSize = 13.sp, color = MtgGreen, fontWeight = FontWeight.Bold)
                                else if (ch == "L") Text("L", fontSize = 13.sp, color = MtgRed, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${ps.wins}W/${ps.losses}L", fontSize = 15.sp, color = MtgGreen, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${(ps.winRate * 100).toInt()}%", fontSize = 13.sp, color = ContentOnDark.copy(alpha = 0.6f))
                    }
                }
            }

            // === COMPACT LAST 2 GAMES (merged with rivalries as a single row) ===
            if (recentGames.isNotEmpty()) {
                recentGames.forEach { game ->
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(game.timestamp))
                    val dur = "${game.durationSeconds / 60}m"
                    val lifeDiff = game.players.find { it.name == game.winnerName }?.life ?: 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                            .background(SurfaceMid.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text("🏆 ${game.winnerName}", fontSize = 16.sp, color = MtgGreen, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("♥${lifeDiff}", fontSize = 14.sp, color = ContentOnDark.copy(alpha = 0.5f))
                        Text("${game.gameMode} • ${dur} • ${time}", fontSize = 14.sp, color = ContentOnDark.copy(alpha = 0.35f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(8.dp), color = SurfaceMid, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 5.dp)) {
            Text(value, fontSize = 16.sp, color = ContentOnDark, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(label, fontSize = 11.sp, color = ContentOnDark.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SetupRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = ContentOnDark, modifier = Modifier.width(70.dp), textAlign = TextAlign.End)
        content()
    }
}

@Composable
private fun PlayerNameRow(
    index: Int,
    setup: PlayerSetup,
    savedNames: List<String>,
    onSetupChange: (PlayerSetup) -> Unit,
    onAddName: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }
    val allOptions = savedNames + listOf("+ Add New…")

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(MtgColors[setup.colorIndex].first, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text("P${index + 1}", fontSize = 16.sp, color = ContentOnDark, modifier = Modifier.width(28.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            Surface(onClick = { expanded = true }, shape = RoundedCornerShape(8.dp), color = SurfaceMid, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    Text(setup.name.ifBlank { "Select player…" }, fontSize = 15.sp, color = if (setup.name.isNotBlank()) ContentOnDark else ContentOnDark.copy(alpha = 0.4f))
                    Text("▼", fontSize = 12.sp, color = ContentOnDark.copy(alpha = 0.5f))
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = SurfaceDark) {
                allOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { if (option == "+ Add New…") Text(option, color = MetaBlue, fontWeight = FontWeight.Medium) else Text(option, color = ContentOnDark) },
                        onClick = {
                            expanded = false
                            if (option == "+ Add New…") showAddDialog = true else onSetupChange(setup.copy(name = option))
                        },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MtgColors.forEachIndexed { colorIdx, (color, _) ->
                Surface(modifier = Modifier.size(28.dp), shape = CircleShape, color = color,
                    border = if (setup.colorIndex == colorIdx) BorderStroke(2.dp, OnMetaBlue) else null,
                    onClick = { onSetupChange(setup.copy(colorIndex = colorIdx)) }) {}
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(containerColor = SurfaceDark, onDismissRequest = { showAddDialog = false; newNameInput = "" },
            title = { Text("Add Player Name", color = ContentOnDark) },
            text = {
                OutlinedTextField(value = newNameInput, onValueChange = { newNameInput = it }, singleLine = true,
                    placeholder = { Text("Enter name", color = ContentOnDark.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ContentOnDark, unfocusedTextColor = ContentOnDark,
                        focusedContainerColor = SurfaceMid, unfocusedContainerColor = SurfaceMid))
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNameInput.isNotBlank()) { onAddName(newNameInput.trim()); onSetupChange(setup.copy(name = newNameInput.trim())) }
                    showAddDialog = false; newNameInput = ""
                }) { Text("Add", color = MetaBlue) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; newNameInput = "" }) { Text("Cancel", color = ContentOnDark) } },
        )
    }
}
