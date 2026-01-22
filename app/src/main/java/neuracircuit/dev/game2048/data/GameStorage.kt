package neuracircuit.dev.game2048.data

import android.content.Context
import android.content.SharedPreferences
import neuracircuit.dev.game2048.model.Tile
import kotlinx.serialization.json.Json
import androidx.core.content.edit

class GameStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    
    // Configured Json to be lenient
    private val json = Json { ignoreUnknownKeys = true }

    fun saveData(grid: List<Tile>, score: Int) {
        prefs.edit {

            // 1. Save Score
            putInt("score", score)

            // 2. Update High Score if needed
            val currentHigh = prefs.getInt("high_score", 0)
            if (score > currentHigh) {
                putInt("high_score", score)
            }

            // 3. Serialize Grid
            val gridJson = json.encodeToString(grid)
            putString("game_board", gridJson)

        }
    }

    fun loadData(): SavedGame? {
        val gridJson = prefs.getString("game_board", null) ?: return null
        val score = prefs.getInt("score", 0)
        val highScore = prefs.getInt("high_score", 0)

        return try {
            val grid = json.decodeFromString<List<Tile>>(gridJson)
            SavedGame(grid, score, highScore)
        } catch (e: Exception) {
            // Handle corruption (e.g., manual JSON editing or bad update)
            e.printStackTrace()
            null
        }
    }
    
    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    fun clearGame() {
        // We keep high_score, but reset board and score
        prefs.edit {
            remove("game_board")
                .putInt("score", 0)
        }
    }
}

// Helper DTO for loading
data class SavedGame(val grid: List<Tile>, val score: Int, val highScore: Int)
