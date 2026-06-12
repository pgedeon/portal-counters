package com.pgedeon.portalcounters.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pgedeon.portalcounters.R
import com.pgedeon.portalcounters.audio.SoundManager
import com.pgedeon.portalcounters.model.GameAction
import com.pgedeon.portalcounters.model.GameMode
import com.pgedeon.portalcounters.model.GameState
import com.pgedeon.portalcounters.model.PlayerState
import com.pgedeon.portalcounters.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    gameState: GameState,
    gameMode: GameMode,
    onMenu: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    // Release SoundPool when leaving the game screen
    DisposableEffect(soundManager) {
        onDispose { soundManager.release() }
    }

    var showNewGameConfirm by remember { mutableStateOf(false) }
    var showWinnerDialog by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(Unit) {
        var seconds = 0
        while (true) {
            delay(1000)
            seconds++
            gameState.updateElapsed(seconds)
        }
    }

    // Winner detection
    LaunchedEffect(gameState.winner) {
        showWinnerDialog = gameState.winner != null
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        // Top system bar area — fully black to match status bar
        Spacer(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .background(ControlBarBg)
        )

        // Game area
        Box(modifier = Modifier.weight(1f)) {
            when (gameState.players.size) {
                2 -> TwoPlayerLayout(gameState, gameMode, soundManager)
                3 -> ThreePlayerLayout(gameState, gameMode, soundManager)
                else -> FourPlayerLayout(gameState, gameMode, soundManager)
            }
        }

        // Control bar
        ControlBar(
            elapsedSeconds = gameState.elapsedSeconds,
            onUndo = { gameState.undoLastAction() },
            canUndo = gameState.actionHistory.isNotEmpty(),
            onNewGame = { showNewGameConfirm = true },
            onMenu = onMenu,
            soundManager = soundManager,
        )
    }

    // New Game confirmation
    if (showNewGameConfirm) {
        AlertDialog(
            containerColor = SurfaceDark,
            onDismissRequest = { showNewGameConfirm = false },
            text = { Text(stringResource(R.string.dialog_confirm_new_game), color = ContentOnDark) },
            confirmButton = {
                TextButton(onClick = { showNewGameConfirm = false; onNewGame() }) {
                    Text(stringResource(R.string.dialog_yes), color = MtgRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameConfirm = false }) {
                    Text(stringResource(R.string.dialog_no), color = ContentOnDark)
                }
            },
        )
    }

    // Winner announcement
    if (showWinnerDialog && gameState.winner != null) {
        AlertDialog(
            containerColor = SurfaceDark,
            onDismissRequest = { showWinnerDialog = false },
            title = { Text("🏆 ${gameState.winner!!.name} wins!", color = MtgGreen) },
            confirmButton = {
                TextButton(onClick = { showWinnerDialog = false; onNewGame() }) {
                    Text(stringResource(R.string.btn_new_game), color = MetaBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWinnerDialog = false }) {
                    Text(stringResource(R.string.dialog_ok), color = ContentOnDark)
                }
            },
        )
    }
}

@Composable
private fun PZ(
    idx: Int, gameState: GameState, gameMode: GameMode, inverted: Boolean,
    soundManager: SoundManager, modifier: Modifier = Modifier,
) {
    PlayerZone(
        playerIndex = idx, player = gameState.players[idx], allPlayers = gameState.players,
        gameMode = gameMode, isInverted = inverted, soundManager = soundManager,
        onLifeChange = { i, d -> gameState.applyAction(GameAction.LifeChange(i, d)) },
        onCommanderDamageChange = { i, f, d -> gameState.applyAction(GameAction.CommanderDamageChange(i, f, d)) },
        onPoisonChange = { i, d -> gameState.applyAction(GameAction.PoisonChange(i, d)) },
        onEnergyChange = { i, d -> gameState.applyAction(GameAction.EnergyChange(i, d)) },
        modifier = modifier,
    )
}

@Composable
private fun TwoPlayerLayout(gs: GameState, gm: GameMode, sm: SoundManager) {
    Row(modifier = Modifier.fillMaxSize()) {
        PZ(0, gs, gm, false, sm, Modifier.weight(1f))
        PZ(1, gs, gm, true, sm, Modifier.weight(1f))
    }
}

@Composable
private fun ThreePlayerLayout(gs: GameState, gm: GameMode, sm: SoundManager) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            PZ(0, gs, gm, false, sm, Modifier.weight(1f))
            PZ(1, gs, gm, true, sm, Modifier.weight(1f))
        }
        PZ(2, gs, gm, true, sm, Modifier.weight(1f))
    }
}

@Composable
private fun FourPlayerLayout(gs: GameState, gm: GameMode, sm: SoundManager) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            PZ(0, gs, gm, false, sm, Modifier.weight(1f))
            PZ(2, gs, gm, false, sm, Modifier.weight(1f))
        }
        Row(modifier = Modifier.weight(1f)) {
            PZ(1, gs, gm, true, sm, Modifier.weight(1f))
            PZ(3, gs, gm, true, sm, Modifier.weight(1f))
        }
    }
}
