package com.pgedeon.portalcounters.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class GameRecord(
    val id: Long,
    val winnerName: String,
    val players: List<PlayerResult>,
    val gameMode: String,
    val startingLife: Int,
    val durationSeconds: Int,
    val timestamp: Long,
)

data class PlayerResult(
    val name: String,
    val life: Int,
    val colorIndex: Int,
)

class GameStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("portal_counters", Context.MODE_PRIVATE)

    // === Sound Preference ===

    var soundMuted: Boolean
        get() = prefs.getBoolean("sound_muted", false)
        set(value) = prefs.edit().putBoolean("sound_muted", value).apply()

    // === Player Name Presets ===

    fun getPlayerNames(): List<String> {
        val json = prefs.getString("player_names", null) ?: return emptyList()
        val arr = JSONArray(json)
        val all = (0 until arr.length()).map { arr.getString(it) }
        // Filter out generic "Player N" defaults and clean storage
        val filtered = all.filter { !it.matches(Regex("Player \\d+")) }
        if (filtered.size < all.size) {
            prefs.edit().putString("player_names", JSONArray(filtered).toString()).apply()
        }
        return filtered
    }

    fun savePlayerName(name: String) {
        val names = getPlayerNames().toMutableList()
        if (name.isNotBlank() && name !in names) {
            names.add(name)
            prefs.edit().putString("player_names", JSONArray(names).toString()).apply()
        }
    }

    fun removePlayerName(name: String) {
        val names = getPlayerNames().toMutableList()
        names.remove(name)
        prefs.edit().putString("player_names", JSONArray(names).toString()).apply()
    }

    // === Game History ===
    // Games are stored newest-first (index 0 = most recent).

    fun saveGame(record: GameRecord) {
        val history = getGameHistory().toMutableList()
        history.add(0, record)
        // Keep last 100 games (newest at front)
        while (history.size > 100) history.removeAt(history.lastIndex)
        val arr = JSONArray()
        history.forEach { arr.put(recordToJson(it)) }
        prefs.edit().putString("game_history", arr.toString()).apply()
    }

    fun getGameHistory(): List<GameRecord> {
        val json = prefs.getString("game_history", null) ?: return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { jsonToRecord(arr.getJSONObject(it)) }
    }

    fun getRecentGames(count: Int): List<GameRecord> {
        return getGameHistory().take(count)
    }

    // === Win Counts ===

    fun getWinCounts(): Map<String, Int> {
        val history = getGameHistory()
        val counts = mutableMapOf<String, Int>()
        history.forEach { rec ->
            counts[rec.winnerName] = (counts[rec.winnerName] ?: 0) + 1
        }
        return counts
    }

    private fun recordToJson(record: GameRecord): JSONObject {
        val playersArr = JSONArray()
        record.players.forEach { p ->
            playersArr.put(JSONObject().apply {
                put("name", p.name)
                put("life", p.life)
                put("colorIndex", p.colorIndex)
            })
        }
        return JSONObject().apply {
            put("id", record.id)
            put("winnerName", record.winnerName)
            put("players", playersArr)
            put("gameMode", record.gameMode)
            put("startingLife", record.startingLife)
            put("durationSeconds", record.durationSeconds)
            put("timestamp", record.timestamp)
        }
    }

    private fun jsonToRecord(obj: JSONObject): GameRecord {
        val playersArr = obj.getJSONArray("players")
        val players = (0 until playersArr.length()).map { i ->
            val p = playersArr.getJSONObject(i)
            PlayerResult(p.getString("name"), p.getInt("life"), p.getInt("colorIndex"))
        }
        return GameRecord(
            id = obj.getLong("id"),
            winnerName = obj.getString("winnerName"),
            players = players,
            gameMode = obj.optString("gameMode", "Standard"),
            startingLife = obj.optInt("startingLife", 20),
            durationSeconds = obj.optInt("durationSeconds", 0),
            timestamp = obj.getLong("timestamp"),
        )
    }
}
