package com.pgedeon.portalcounters

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.pgedeon.portalcounters.data.GameRecord
import com.pgedeon.portalcounters.data.GameStorage
import com.pgedeon.portalcounters.data.PlayerResult
import com.pgedeon.portalcounters.model.*
import com.pgedeon.portalcounters.ui.GameScreen
import com.pgedeon.portalcounters.ui.GameSetupScreen
import com.pgedeon.portalcounters.ui.theme.MtgColors
import com.pgedeon.portalcounters.ui.theme.PortalCountersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        val storage = GameStorage(this)
        setContent {
            PortalCountersTheme(darkTheme = true) {
                PortalCountersApp(storage)
            }
        }
    }
}

@Composable
fun PortalCountersApp(storage: GameStorage) {
    var screen by remember { mutableStateOf("setup") }
    var gameState by remember { mutableStateOf<GameState?>(null) }
    var currentGameMode by remember { mutableStateOf(GameMode.STANDARD) }
    var currentStartingLife by remember { mutableStateOf(20) }
    var currentPlayerCount by remember { mutableIntStateOf(2) }
    var gameSaved by remember { mutableStateOf(false) }

    // Remember last used player setups so they persist between games
    var lastPlayerSetups by remember { mutableStateOf<List<PlayerSetup>>(emptyList()) }

    /**
     * Save a completed game to history.
     * Only saves once per game, and only when the game is actually over.
     * Called when the user navigates away from the game screen (not on every recomposition).
     */
    fun saveCompletedGameIfNeeded(gs: GameState) {
        if (!gameSaved && gs.gameOver && gs.winner != null) {
            gameSaved = true
            val record = GameRecord(
                id = System.currentTimeMillis(),
                winnerName = gs.winner!!.name,
                players = gs.players.map { p ->
                    PlayerResult(
                        name = p.name,
                        life = p.life,
                        colorIndex = MtgColors.indexOfFirst { it.first == p.color }.coerceAtLeast(0),
                    )
                },
                gameMode = currentGameMode.label,
                startingLife = currentStartingLife,
                durationSeconds = gs.elapsedSeconds,
                timestamp = System.currentTimeMillis(),
            )
            storage.saveGame(record)
        }
    }

    when (screen) {
        "setup" -> {
            GameSetupScreen(
                gameStorage = storage,
                lastSetups = lastPlayerSetups,
                lastPlayerCount = currentPlayerCount,
                lastGameMode = currentGameMode,
                lastStartingLife = currentStartingLife,
                onStartGame = { playerCount, startingLife, gameMode, setups ->
                    currentGameMode = gameMode
                    currentStartingLife = startingLife
                    currentPlayerCount = playerCount
                    lastPlayerSetups = setups
                    val players = setups.take(playerCount).mapIndexed { index, setup ->
                        val (color, colorName) = MtgColors[setup.colorIndex]
                        PlayerState(
                            name = setup.name.ifBlank { "Player ${index + 1}" },
                            color = color,
                            colorName = colorName,
                            startingLife = startingLife,
                        )
                    }
                    setups.take(playerCount).forEach { setup ->
                        if (setup.name.isNotBlank()) storage.savePlayerName(setup.name)
                    }
                    gameState = GameState(players, gameMode, startingLife)
                    gameSaved = false
                    screen = "game"
                },
            )
        }
        "game" -> {
            val gs = gameState
            if (gs != null) {
                GameScreen(
                    gameState = gs,
                    gameMode = currentGameMode,
                    onMenu = {
                        saveCompletedGameIfNeeded(gs)
                        screen = "setup"
                    },
                    onNewGame = {
                        saveCompletedGameIfNeeded(gs)
                        screen = "setup"
                    },
                )
            }
        }
    }
}
