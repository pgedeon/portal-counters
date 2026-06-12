package com.meta.portal.sampleapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.meta.portal.sampleapp.data.GameRecord
import com.meta.portal.sampleapp.data.GameStorage
import com.meta.portal.sampleapp.model.*
import com.meta.portal.sampleapp.ui.GameScreen
import com.meta.portal.sampleapp.ui.GameSetupScreen
import com.meta.portal.sampleapp.ui.theme.MtgColors
import com.meta.portal.sampleapp.ui.theme.SampleAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        val storage = GameStorage(this)
        setContent {
            SampleAppTheme(darkTheme = true) {
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
                if (gs.gameOver && gs.winner != null && !gameSaved) {
                    gameSaved = true
                    val record = GameRecord(
                        id = System.currentTimeMillis(),
                        winnerName = gs.winner!!.name,
                        players = gs.players.map { p ->
                            com.meta.portal.sampleapp.data.PlayerResult(
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

                GameScreen(
                    gameState = gs,
                    gameMode = currentGameMode,
                    onMenu = { screen = "setup" },
                    onNewGame = { screen = "setup" },
                )
            }
        }
    }
}
