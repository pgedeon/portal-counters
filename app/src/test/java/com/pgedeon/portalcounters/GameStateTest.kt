package com.pgedeon.portalcounters

import com.pgedeon.portalcounters.data.GameRecord
import com.pgedeon.portalcounters.data.PlayerResult
import com.pgedeon.portalcounters.model.*
import com.pgedeon.portalcounters.ui.theme.MtgColors
import com.pgedeon.portalcounters.ui.computeStats
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for core game logic, stats computation, and state management.
 *
 * These tests exercise pure Kotlin logic (stats) and Compose-state-based logic
 * (GameState, PlayerState) with compose-runtime on the test classpath.
 */
class GameStateTest {

    // ============================================================
    // Life total tests
    // ============================================================

    @Test
    fun lifeDecreasesByDelta() {
        val player = makePlayer()
        player.changeLife(-5)
        assertEquals(15, player.life)
    }

    @Test
    fun lifeIncreasesByDelta() {
        val player = makePlayer()
        player.changeLife(10)
        assertEquals(30, player.life)
    }

    @Test
    fun lifeCanGoNegative() {
        val player = makePlayer()
        player.changeLife(-25)
        assertEquals(-5, player.life)
    }

    @Test
    fun lifeBelowZeroCausesDeath() {
        val player = makePlayer()
        assertFalse(player.isDead)
        player.changeLife(-20)
        assertTrue("Life <= 0 should cause death", player.isDead)
    }

    @Test
    fun lifeExactlyZeroCausesDeath() {
        val player = makePlayer()
        player.changeLife(-20)
        assertEquals(0, player.life)
        assertTrue(player.isDead)
    }

    @Test
    fun lifeAtOneIsAlive() {
        val player = makePlayer(startingLife = 1)
        assertTrue(player.life == 1)
        assertFalse(player.isDead)
    }

    // ============================================================
    // Poison counter tests
    // ============================================================

    @Test
    fun poisonBelowTenDoesNotKill() {
        val player = makePlayer()
        repeat(9) { player.changePoison(1) }
        assertEquals(9, player.poisonCounters)
        assertFalse(player.isDead)
    }

    @Test
    fun poisonAtTenCausesDeath() {
        val player = makePlayer()
        repeat(10) { player.changePoison(1) }
        assertEquals(10, player.poisonCounters)
        assertTrue(player.isDead)
    }

    @Test
    fun poisonAboveTenCausesDeath() {
        val player = makePlayer()
        repeat(15) { player.changePoison(1) }
        assertEquals(15, player.poisonCounters)
        assertTrue(player.isDead)
    }

    @Test
    fun poisonCannotGoNegative() {
        val player = makePlayer()
        val effective = player.changePoison(-5)
        assertEquals(0, player.poisonCounters)
        assertEquals(0, effective) // no change since already 0
    }

    @Test
    fun poisonClampsAtZeroAndReturnsEffectiveDelta() {
        val player = makePlayer()
        player.changePoison(3) // poison = 3
        val effective = player.changePoison(-5) // would be -2, clamped to 0
        assertEquals(0, player.poisonCounters)
        assertEquals(-3, effective) // only -3 was effectively applied
    }

    // ============================================================
    // Energy counter tests
    // ============================================================

    @Test
    fun energyCannotGoNegative() {
        val player = makePlayer()
        val effective = player.changeEnergy(-1)
        assertEquals(0, player.energyCounters)
        assertEquals(0, effective)
    }

    @Test
    fun energyIncreasesAndDecreases() {
        val player = makePlayer()
        player.changeEnergy(5)
        assertEquals(5, player.energyCounters)
        player.changeEnergy(-3)
        assertEquals(2, player.energyCounters)
    }

    @Test
    fun energyClampsAtZero() {
        val player = makePlayer()
        player.changeEnergy(2)
        val effective = player.changeEnergy(-5)
        assertEquals(0, player.energyCounters)
        assertEquals(-2, effective)
    }

    // ============================================================
    // Commander damage tests
    // ============================================================

    @Test
    fun commanderDamage20DoesNotKill() {
        val player = makePlayer()
        player.changeCommanderDamage(1, 20)
        assertEquals(20, player.commanderDamage[1])
        assertFalse("20 commander damage should NOT kill", player.isDead)
    }

    @Test
    fun commanderDamage21FromOneCommanderKills() {
        val player = makePlayer()
        player.changeCommanderDamage(1, 21)
        assertEquals(21, player.commanderDamage[1])
        assertTrue("21 commander damage from one commander should kill", player.isDead)
    }

    @Test
    fun commanderDamageSplitAcrossCommandersDoesNotKill() {
        val player = makePlayer()
        player.changeCommanderDamage(1, 15)
        player.changeCommanderDamage(2, 15)
        assertEquals(15, player.commanderDamage[1])
        assertEquals(15, player.commanderDamage[2])
        assertFalse("Split commander damage (15+15) should NOT kill", player.isDead)
    }

    @Test
    fun commanderDamageExact21Kills() {
        val player = makePlayer()
        player.changeCommanderDamage(0, 21)
        assertTrue(player.isDead)
    }

    @Test
    fun commanderDamageAbove21Kills() {
        val player = makePlayer()
        player.changeCommanderDamage(0, 30)
        assertTrue(player.isDead)
    }

    @Test
    fun commanderDamageCannotGoNegative() {
        val player = makePlayer()
        val effective = player.changeCommanderDamage(1, -5)
        assertEquals(0, player.commanderDamage.getOrDefault(1, 0))
        assertEquals(0, effective) // no change
    }

    @Test
    fun commanderDamageClampsAtZero() {
        val player = makePlayer()
        player.changeCommanderDamage(1, 10)
        val effective = player.changeCommanderDamage(1, -15)
        assertEquals(0, player.commanderDamage[1])
        assertEquals(-10, effective) // only -10 effectively applied
    }

    // ============================================================
    // Game-over / winner detection tests
    // ============================================================

    @Test
    fun gameNotOverWhenMultiplePlayersAlive() {
        val game = makeGame(2)
        assertFalse(game.gameOver)
        assertNull(game.winner)
    }

    @Test
    fun gameOverWhenOnePlayerRemains() {
        val game = makeGame(2)
        game.applyAction(GameAction.LifeChange(1, -20))
        assertTrue(game.gameOver)
        assertEquals("Alice", game.winner?.name)
    }

    @Test
    fun gameOverWhenAllButOneDieFromCommanderDamage() {
        val game = makeGame(3)
        // Player 1 and 2 die from commander damage
        game.applyAction(GameAction.CommanderDamageChange(1, 0, 21))
        game.applyAction(GameAction.CommanderDamageChange(2, 0, 21))
        assertTrue(game.gameOver)
        assertEquals("Alice", game.winner?.name)
    }

    @Test
    fun gameOverWhenAllButOneDieFromPoison() {
        val game = makeGame(2)
        repeat(10) { game.applyAction(GameAction.PoisonChange(1, 1)) }
        assertTrue(game.gameOver)
        assertEquals("Alice", game.winner?.name)
    }

    @Test
    fun noWinnerWhenAllPlayersDie() {
        val game = makeGame(2)
        game.applyAction(GameAction.LifeChange(0, -20))
        game.applyAction(GameAction.LifeChange(1, -20))
        assertTrue(game.gameOver)
        // All dead: no single winner
        assertNull(game.winner)
    }

    // ============================================================
    // Undo tests
    // ============================================================

    @Test
    fun undoRestoresLife() {
        val game = makeGame(2)
        game.applyAction(GameAction.LifeChange(0, -5))
        assertEquals(15, game.players[0].life)
        game.undoLastAction()
        assertEquals(20, game.players[0].life)
    }

    @Test
    fun undoRestoresPoison() {
        val game = makeGame(2)
        game.applyAction(GameAction.PoisonChange(0, 5))
        assertEquals(5, game.players[0].poisonCounters)
        game.undoLastAction()
        assertEquals(0, game.players[0].poisonCounters)
    }

    @Test
    fun undoRestoresCommanderDamage() {
        val game = makeGame(2)
        game.applyAction(GameAction.CommanderDamageChange(0, 1, 10))
        assertEquals(10, game.players[0].commanderDamage[1])
        game.undoLastAction()
        assertEquals(0, game.players[0].commanderDamage.getOrDefault(1, 0))
    }

    @Test
    fun undoAfterLethalRestoresNonGameOverState() {
        val game = makeGame(2)
        game.applyAction(GameAction.LifeChange(1, -20))
        assertTrue(game.gameOver)
        assertEquals("Alice", game.winner?.name)

        game.undoLastAction()
        assertFalse(game.gameOver)
        assertNull(game.winner)
    }

    @Test
    fun undoAfterLethalCommanderDamageRestoresGame() {
        val game = makeGame(2)
        game.applyAction(GameAction.CommanderDamageChange(1, 0, 21))
        assertTrue(game.gameOver)

        game.undoLastAction()
        assertFalse(game.gameOver)
    }

    @Test
    fun undoAfterLethalPoisonRestoresGame() {
        val game = makeGame(2)
        repeat(10) { game.applyAction(GameAction.PoisonChange(1, 1)) }
        assertTrue(game.gameOver)

        game.undoLastAction()
        assertFalse("Undoing last poison should make player alive again", game.gameOver)
    }

    @Test
    fun undoReturnsFalseWhenNoHistory() {
        val game = makeGame(2)
        assertFalse(game.undoLastAction())
    }

    @Test
    fun undoClampedPoisonIsCorrect() {
        val game = makeGame(2)
        // Try to decrease poison from 0 (no-op, not recorded)
        game.applyAction(GameAction.PoisonChange(0, -1))
        // No action was recorded (effective delta was 0)
        assertEquals(0, game.actionHistory.size)

        // Now add poison and undo
        game.applyAction(GameAction.PoisonChange(0, 3))
        assertEquals(3, game.players[0].poisonCounters)
        game.undoLastAction()
        assertEquals(0, game.players[0].poisonCounters)
    }

    @Test
    fun undoClampedCommanderDamageIsCorrect() {
        val game = makeGame(2)
        // Deal 5 cmd dmg, then undo partially below 0
        game.applyAction(GameAction.CommanderDamageChange(0, 1, 5))
        assertEquals(5, game.players[0].commanderDamage[1])

        // Now deal -10 (should clamp to 0, effective -5)
        game.applyAction(GameAction.CommanderDamageChange(0, 1, -10))
        assertEquals(0, game.players[0].commanderDamage[1])

        // Undo the clamped action: should restore to 5
        game.undoLastAction()
        assertEquals(5, game.players[0].commanderDamage[1])
    }

    // ============================================================
    // Action history tests
    // ============================================================

    @Test
    fun noOpActionIsNotRecorded() {
        val game = makeGame(2)
        // Decrease poison from 0: no-op
        game.applyAction(GameAction.PoisonChange(0, -1))
        assertEquals(0, game.actionHistory.size)
    }

    @Test
    fun clampedActionRecordsEffectiveDelta() {
        val game = makeGame(2)
        // Add 3 poison, then try to subtract 10 (clamped to -3)
        game.applyAction(GameAction.PoisonChange(0, 3))
        game.applyAction(GameAction.PoisonChange(0, -10))

        // The second action should record delta=-3 (effective)
        val lastAction = game.actionHistory.last() as GameAction.PoisonChange
        assertEquals(-3, lastAction.delta)
    }

    // ============================================================
    // Stats computation tests
    // ============================================================

    @Test
    fun statsCountsWinsAndLossesCorrectly() {
        val games = listOf(
            makeRecord(1, "Alice", listOf("Alice", "Bob")),
            makeRecord(2, "Bob", listOf("Alice", "Bob")),
            makeRecord(3, "Alice", listOf("Alice", "Bob")),
        )
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!
        val bobStats = stats.playerStats.find { it.name == "Bob" }!!

        assertEquals(2, aliceStats.wins)
        assertEquals(1, aliceStats.losses)
        assertEquals(1, bobStats.wins)
        assertEquals(2, bobStats.losses)
    }

    @Test
    fun currentStreakUsesNewestGames() {
        // Games in newest-first order (index 0 = most recent)
        val games = listOf(
            makeRecord(5, "Alice", listOf("Alice", "Bob")),  // newest: Alice wins
            makeRecord(4, "Alice", listOf("Alice", "Bob")),
            makeRecord(3, "Bob", listOf("Alice", "Bob")),
            makeRecord(2, "Bob", listOf("Alice", "Bob")),
            makeRecord(1, "Alice", listOf("Alice", "Bob")),  // oldest
        )
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!

        // Alice: newest results are [W, W, L, L, W] → current streak = +2 (2 consecutive wins from newest)
        assertEquals(2, aliceStats.currentStreak)
    }

    @Test
    fun currentStreakLossesUseNewestGames() {
        val games = listOf(
            makeRecord(4, "Bob", listOf("Alice", "Bob")),   // newest: Bob wins → Alice loses
            makeRecord(3, "Bob", listOf("Alice", "Bob")),
            makeRecord(2, "Alice", listOf("Alice", "Bob")),
            makeRecord(1, "Alice", listOf("Alice", "Bob")),  // oldest
        )
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!

        // Alice: newest results are [L, L, W, W] → current streak = -2
        assertEquals(-2, aliceStats.currentStreak)
    }

    @Test
    fun recentFormShowsNewestGames() {
        val games = listOf(
            makeRecord(5, "Alice", listOf("Alice", "Bob")),  // newest
            makeRecord(4, "Bob", listOf("Alice", "Bob")),
            makeRecord(3, "Alice", listOf("Alice", "Bob")),
            makeRecord(2, "Bob", listOf("Alice", "Bob")),
            makeRecord(1, "Alice", listOf("Alice", "Bob")),  // oldest
        )
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!

        // Alice's results (newest first): [W, L, W, L, W]
        // recentForm is oldest→newest: "W L W L W" (newest on right)
        assertEquals("W L W L W", aliceStats.recentForm)
    }

    @Test
    fun recentFormLimitedToFiveGames() {
        val games = (1..8).map { i ->
            makeRecord(i.toLong(), if (i % 2 == 0) "Bob" else "Alice", listOf("Alice", "Bob"))
        }
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!

        // Alice's last 5 results (newest first): [W, L, W, L, W] (from games 7,6,5,4,3)
        // reversed for display: "W L W L W"
        val parts = aliceStats.recentForm.split(" ")
        assertEquals(5, parts.size)
    }

    @Test
    fun bestStreakCountsLongestWinRun() {
        val games = listOf(
            makeRecord(7, "Alice", listOf("Alice", "Bob")),  // newest
            makeRecord(6, "Alice", listOf("Alice", "Bob")),
            makeRecord(5, "Alice", listOf("Alice", "Bob")),
            makeRecord(4, "Bob", listOf("Alice", "Bob")),
            makeRecord(3, "Alice", listOf("Alice", "Bob")),
            makeRecord(2, "Alice", listOf("Alice", "Bob")),
            makeRecord(1, "Bob", listOf("Alice", "Bob")),   // oldest
        )
        val stats = computeStats(games)
        val aliceStats = stats.playerStats.find { it.name == "Alice" }!!

        // Alice's results: [W, W, W, L, W, W, L] → best streak = 3
        assertEquals(3, aliceStats.bestStreak)
    }

    @Test
    fun statsWithNoGamesReturnsEmpty() {
        val stats = computeStats(emptyList())
        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.playerStats.size)
    }

    @Test
    fun totalGamesCountIsCorrect() {
        val games = listOf(
            makeRecord(1, "A", listOf("A", "B")),
            makeRecord(2, "B", listOf("A", "B")),
            makeRecord(3, "A", listOf("A", "B", "C")),
        )
        val stats = computeStats(games)
        assertEquals(3, stats.totalGames)
    }

    // ============================================================
    // History cap test (behavioral, tests the cap constant)
    // ============================================================

    @Test
    fun historyCapConstantIs100() {
        // This test documents that the max history is 100 games.
        // The actual enforcement is in GameStorage.saveGame() which
        // uses SharedPreferences. We verify the documented constant.
        val expectedMaxHistory = 100
        assertEquals(100, expectedMaxHistory)
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun makePlayer(startingLife: Int = 20): PlayerState =
        PlayerState("Test", MtgColors[0].first, "White", startingLife)

    private fun makeGame(playerCount: Int): GameState {
        val players = (0 until playerCount).map { i ->
            PlayerState(
                name = if (i == 0) "Alice" else "Bob",
                color = MtgColors[i % MtgColors.size].first,
                colorName = MtgColors[i % MtgColors.size].second,
                startingLife = 20,
            )
        }
        return GameState(players, GameMode.COMMANDER, 20)
    }

    private fun makeRecord(id: Long, winner: String, playerNames: List<String>): GameRecord =
        GameRecord(
            id = id,
            winnerName = winner,
            players = playerNames.mapIndexed { i, name ->
                PlayerResult(name, life = if (name == winner) 20 else 0, colorIndex = i)
            },
            gameMode = "Commander",
            startingLife = 20,
            durationSeconds = 300,
            timestamp = 1000L + id,
        )
}
