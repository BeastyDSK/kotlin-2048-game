package neuracircuit.dev.game2048.data

import android.content.Context
import android.content.SharedPreferences
import neuracircuit.dev.game2048.model.Tile
import kotlinx.serialization.json.Json
import androidx.core.content.edit

class GameStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    private val analytics = AnalyticsManager(context.applicationContext)
    
    // Configured Json to be lenient
    private val json = Json { ignoreUnknownKeys = true }

    fun saveData(
        grid: List<Tile>,
        score: Int,
        freeUndosLeft: Int,
        isCloudSaveFlag: Boolean = false,
        cloudHighScore: Int = 0
    ) {
        prefs.edit {

            // 1. Save Score
            putInt("score", score)

            // 2. Update High Score if needed
            val currentHigh = prefs.getInt("high_score", 0)
            val candidateHigh = if (isCloudSaveFlag) {
                maxOf(currentHigh, cloudHighScore)
            } else {
                maxOf(currentHigh, score)
            }
            putInt("high_score", candidateHigh)

            // 3. Save Free Undos Left
            putInt("free_undos_left", freeUndosLeft)

            // 3. Serialize Grid
            val gridJson = json.encodeToString(grid)
            putString("game_board", gridJson)

        }
    }

    // NEW: Save Settings
    fun saveSettings(volume: Float, hapticsEnabled: Boolean) {
        prefs.edit {
            putFloat("volume", volume)
                .putBoolean("haptics", hapticsEnabled)
        }
    }

    fun loadData(): SavedGame? {
        val gridJson = prefs.getString("game_board", null) ?: return null
        val score = prefs.getInt("score", 0)
        val highScore = prefs.getInt("high_score", 0)
        val freeUndosLeft = prefs.getInt("free_undos_left", 3)

        return try {
            val grid = json.decodeFromString<List<Tile>>(gridJson)
            SavedGame(grid, score, highScore, freeUndosLeft)
        } catch (e: Exception) {
            // Handle corruption (e.g., manual JSON editing or bad update)
            analytics.logNonFatalError("GameStorage.loadData", e)
            null
        }
    }
    
    fun getHighScore(): Int {
        return prefs.getInt("high_score", 0)
    }

    fun clearActiveGame() {
        // We keep high_score, but reset board and score
        prefs.edit {
            remove("game_board")
                .putInt("score", 0)
                .putInt("free_undos_left", 3)
        }
    }

    // NEW: Load Settings
    fun getSettings(): SavedSettings {
        return SavedSettings(
            volume = prefs.getFloat("volume", 0.5f), // Default 0.5f
            hapticsEnabled = prefs.getBoolean("haptics", true) // Default true
        )
    }

    fun hasSeenTutorial(): Boolean {
        return prefs.getBoolean("has_seen_tutorial", false)
    }

    fun setTutorialSeen() {
        prefs.edit {
            putBoolean("has_seen_tutorial", true)
        }
    }
}

// Helper DTO for loading
data class SavedGame(val grid: List<Tile>, val score: Int, val highScore: Int, val freeUndosLeft: Int = 3)
data class SavedSettings(val volume: Float, val hapticsEnabled: Boolean)
