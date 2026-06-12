package com.pgedeon.portalcounters.model

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

    /** Dead if life <= 0, poison >= 10, or any single commander damage >= 21. */
    val isDead: Boolean get() = life <= 0 || poisonCounters >= 10 ||
            commanderDamage.values.any { it >= 21 }

    /** Change life by delta. Life may go below 0 (MTG allows negative life). */
    fun changeLife(delta: Int) { life += delta }

    /**
     * Change poison by delta, clamped to >= 0.
     * @return the effective delta applied (may differ from requested if clamped).
     */
    fun changePoison(delta: Int): Int {
        val old = poisonCounters
        poisonCounters = (poisonCounters + delta).coerceAtLeast(0)
        return poisonCounters - old
    }

    /**
     * Change energy by delta, clamped to >= 0.
     * @return the effective delta applied.
     */
    fun changeEnergy(delta: Int): Int {
        val old = energyCounters
        energyCounters = (energyCounters + delta).coerceAtLeast(0)
        return energyCounters - old
    }

    /**
     * Change commander damage from [fromPlayerIndex] by delta, clamped to >= 0.
     * @return the effective delta applied.
     */
    fun changeCommanderDamage(fromPlayerIndex: Int, delta: Int): Int {
        val current = commanderDamage.getOrDefault(fromPlayerIndex, 0)
        val newVal = (current + delta).coerceAtLeast(0)
        commanderDamage[fromPlayerIndex] = newVal
        return newVal - current
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

    /** Whether the game is over (only one player alive). */
    var gameOver by mutableStateOf(false)
        private set

    fun updateElapsed(seconds: Int) {
        _elapsedSeconds.value = seconds
    }

    /**
     * Apply an action. For clamped counters (poison, energy, commander damage),
     * only records the action if the effective delta is non-zero.
     * The recorded delta is the effective delta, ensuring undo correctness.
     */
    fun applyAction(action: GameAction) {
        val recorded: GameAction? = when (action) {
            is GameAction.LifeChange -> {
                players[action.playerIndex].changeLife(action.delta)
                action
            }
            is GameAction.PoisonChange -> {
                val effective = players[action.playerIndex].changePoison(action.delta)
                if (effective != 0) action.copy(delta = effective) else null
            }
            is GameAction.EnergyChange -> {
                val effective = players[action.playerIndex].changeEnergy(action.delta)
                if (effective != 0) action.copy(delta = effective) else null
            }
            is GameAction.CommanderDamageChange -> {
                val effective = players[action.playerIndex].changeCommanderDamage(action.fromPlayerIndex, action.delta)
                if (effective != 0) action.copy(delta = effective) else null
            }
        }
        if (recorded != null) {
            _actionHistory.add(recorded)
        }
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

    /** Winner: the one player still alive, or null if no winner yet. */
    val winner: PlayerState?
        get() {
            val alive = players.filter { !it.isDead }
            return if (alive.size == 1) alive[0] else null
        }

    /** Life differential: winner's remaining life. */
    val winnerLifeDifferential: Int
        get() = winner?.life ?: 0
}
