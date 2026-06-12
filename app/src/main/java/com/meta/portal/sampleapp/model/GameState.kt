package com.meta.portal.sampleapp.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

enum class GameMode(val label: String) {
    STANDARD("Standard"),
    COMMANDER("Commander")
}

data class PlayerSetup(
    val name: String = "",
    val colorIndex: Int = 0,
)

class PlayerState(
    val name: String,
    val color: Color,
    val colorName: String,
    startingLife: Int,
) {
    var life by mutableStateOf(startingLife)
        private set
    val startingLife: Int = startingLife
    val commanderDamage = mutableStateMapOf<Int, Int>()
    var poisonCounters by mutableStateOf(0)
        private set
    var energyCounters by mutableStateOf(0)
        private set

    // Dead = life <= 0 OR poison >= 10 (for game end detection)
    val isDead: Boolean get() = life <= 0 || poisonCounters >= 10

    fun changeLife(delta: Int) { life += delta }
    fun changePoison(delta: Int) { poisonCounters += delta }
    fun changeEnergy(delta: Int) { energyCounters += delta }
    fun changeCommanderDamage(fromPlayerIndex: Int, delta: Int) {
        val current = commanderDamage.getOrDefault(fromPlayerIndex, 0)
        commanderDamage[fromPlayerIndex] = current + delta
    }
}

sealed class GameAction {
    abstract val playerIndex: Int
    data class LifeChange(override val playerIndex: Int, val delta: Int) : GameAction()
    data class CommanderDamageChange(override val playerIndex: Int, val fromPlayerIndex: Int, val delta: Int) : GameAction()
    data class PoisonChange(override val playerIndex: Int, val delta: Int) : GameAction()
    data class EnergyChange(override val playerIndex: Int, val delta: Int) : GameAction()
}

class GameState(
    val players: List<PlayerState>,
    val gameMode: GameMode = GameMode.STANDARD,
    val startingLife: Int = 20,
) {
    private val _actionHistory = mutableStateListOf<GameAction>()
    val actionHistory: List<GameAction> get() = _actionHistory

    private var _elapsedSeconds = mutableStateOf(0)
    val elapsedSeconds: Int get() = _elapsedSeconds.value

    // Track if game is over (winner declared)
    var gameOver by mutableStateOf(false)
        private set

    fun updateElapsed(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    fun applyAction(action: GameAction) {
        when (action) {
            is GameAction.LifeChange -> players[action.playerIndex].changeLife(action.delta)
            is GameAction.CommanderDamageChange -> players[action.playerIndex].changeCommanderDamage(action.fromPlayerIndex, action.delta)
            is GameAction.PoisonChange -> players[action.playerIndex].changePoison(action.delta)
            is GameAction.EnergyChange -> players[action.playerIndex].changeEnergy(action.delta)
        }
        _actionHistory.add(action)
        checkGameOver()
    }

    fun undoLastAction(): Boolean {
        val last = _actionHistory.removeLastOrNull() ?: return false
        when (last) {
            is GameAction.LifeChange -> players[last.playerIndex].changeLife(-last.delta)
            is GameAction.CommanderDamageChange -> players[last.playerIndex].changeCommanderDamage(last.fromPlayerIndex, -last.delta)
            is GameAction.PoisonChange -> players[last.playerIndex].changePoison(-last.delta)
            is GameAction.EnergyChange -> players[last.playerIndex].changeEnergy(-last.delta)
        }
        checkGameOver()
        return true
    }

    private fun checkGameOver() {
        gameOver = players.count { !it.isDead } <= 1
    }

    // Winner: the one still alive (or null if no winner yet)
    val winner: PlayerState?
        get() {
            val alive = players.filter { !it.isDead }
            return if (alive.size == 1) alive[0] else null
        }

    // Life differential: winner's remaining life (or negative = how far they were ahead)
    val winnerLifeDifferential: Int
        get() {
            val w = winner ?: return 0
            return w.life
        }
}
