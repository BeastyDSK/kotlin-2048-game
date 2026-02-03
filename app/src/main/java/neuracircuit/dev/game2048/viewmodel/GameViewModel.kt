package neuracircuit.dev.game2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import neuracircuit.dev.game2048.data.GameStorage
import neuracircuit.dev.game2048.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import neuracircuit.dev.game2048.data.SoundManager
import java.util.ArrayDeque

// --- DATA MODEL FOR UNDO ---
data class Snapshot(val tiles: List<Tile>, val score: Int)

// Event to trigger UI side-effects (Haptics)
sealed class GameEvent {
    data object Merge : GameEvent()
    data object Victory : GameEvent() // NEW: Event for winning sound/haptics
}

// Add settings to GameState or keep separate. 
// For simplicity, I'll add them to GameState to make the UI simpler to observe.
data class GameUiState(
    val grid: List<Tile> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val isGameOver: Boolean = false,
    val hasWon: Boolean = false,      // NEW: Victory Flag
    val keepPlaying: Boolean = false, // NEW: User decided to continue
    val volume: Float = 0.5f,
    val isHapticEnabled: Boolean = true,
    val canUndo: Boolean = false // UI State for Undo Button
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val storage = GameStorage(application)
    private val soundManager = SoundManager(application) // Init SoundManager

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Flow for one-shot UI events (Haptics)
    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents: SharedFlow<GameEvent> = _gameEvents.asSharedFlow()

    // --- UNDO HISTORY ---
    private val history = ArrayDeque<Snapshot>()
    private val maxHistorySize = 1 // Logic is DRY; change this to 5 later easily

    init {
        // Attempt to load previous game
        val savedGame = storage.loadData()
        val currentGrid = savedGame?.grid ?: emptyList()
        val currentScore = savedGame?.score ?: 0
        val currentHigh = savedGame?.highScore ?: storage.getHighScore()
        
        // Load Settings
        val settings = storage.getSettings()

        if (currentGrid.isNotEmpty()) {
            // FIX: Check if the saved game is already in a Game Over state
            if (calculateGameOver(currentGrid)) {
                // Edge Case: Saved game is dead. Reset but keep high score and settings.
                _uiState.value = GameUiState(
                    highScore = currentHigh,
                    volume = settings.volume,
                    isHapticEnabled = settings.hapticsEnabled
                )
                spawnTile(2)
                storage.clearActiveGame()
            } else {
                // Determine if the user has already won in the saved game
                // If they have a 2048 tile, we assume they chose "Keep Playing" previously
                val has2048 = currentGrid.any { it.value >= 2048 }
                
                _uiState.value = GameUiState(
                    grid = currentGrid,
                    score = currentScore,
                    highScore = currentHigh,
                    volume = settings.volume,
                    isHapticEnabled = settings.hapticsEnabled,
                    hasWon = has2048, // Mark as won so dialog doesn't pop up again immediately
                    keepPlaying = has2048 // Implicitly keep playing on reload
                )
            }
        } else {
            // Apply settings but reset game
            _uiState.value = GameUiState(
                highScore = currentHigh,
                volume = settings.volume,
                isHapticEnabled = settings.hapticsEnabled
            )
            spawnTile(2)
        }
    }

    // --- ACTIONS ---

    fun keepPlaying() {
        _uiState.update { it.copy(keepPlaying = true) }
    }

    // --- UNDO LOGIC ---

    private fun saveState() {
        val currentGrid = _uiState.value.grid
        val currentScore = _uiState.value.score
        
        // CRITICAL: Deep Copy of the list elements
        val gridCopy = currentGrid.map { it.copy() }
        
        if (history.size >= maxHistorySize) {
            history.removeFirst() // Remove oldest
        }
        history.addLast(Snapshot(gridCopy, currentScore))
        
        _uiState.update { it.copy(canUndo = true) }
    }

    fun undoLastMove() {
        if (history.isEmpty()) return

        val snapshot = history.removeLast()
        
        _uiState.update { 
            it.copy(
                grid = snapshot.tiles,
                score = snapshot.score,
                isGameOver = false,
                canUndo = history.isNotEmpty(),
                // Note: We generally don't reset 'hasWon' on undo to avoid re-triggering the dialog annoying 
                // but we could if strict logic is required. Keeping simple for now.
            )
        }
        
        soundManager.playMove(_uiState.value.volume)
        
        // Save the reverted state to storage immediately so app close doesn't lose it
        storage.saveData(snapshot.tiles, snapshot.score)
    }

    // --- Settings Actions ---
    
    fun setVolume(newVolume: Float) {
        _uiState.update { it.copy(volume = newVolume) }
        storage.saveSettings(newVolume, _uiState.value.isHapticEnabled)
    }
    
    fun toggleHaptics(enabled: Boolean) {
        _uiState.update { it.copy(isHapticEnabled = enabled) }
        storage.saveSettings(_uiState.value.volume, enabled)
    }
    
    fun playTestSound() {
        soundManager.playTest(_uiState.value.volume)
    }

    // --- Game Logic ---

    fun resetGame() {
        val currentHigh = storage.getHighScore()
        history.clear() // Clear history on reset
        
        _uiState.update { 
            GameUiState(
                highScore = currentHigh,
                volume = it.volume,
                isHapticEnabled = it.isHapticEnabled,
                canUndo = false,
                hasWon = false,      // Reset Victory
                keepPlaying = false  // Reset Keep Playing
            )
        }
        spawnTile(2)
        storage.clearActiveGame()
    }

    fun handleSwipe(direction: Direction) {
        if (_uiState.value.isGameOver) return

        viewModelScope.launch {
            val currentGrid = _uiState.value.grid
            val result = processMove(currentGrid, direction)

            if (result.moved) {
                // SAVE STATE BEFORE COMMITTING CHANGE
                saveState()

                // Audio & Haptics
                val vol = _uiState.value.volume
                
                if (result.points > 0) {
                    soundManager.playMerge(vol)
                    if (_uiState.value.isHapticEnabled) {
                        _gameEvents.emit(GameEvent.Merge)
                    }
                } else {
                    soundManager.playMove(vol)
                }

                // 1. Intermediate State (The Slide Animation)
                _uiState.update { it.copy(grid = result.intermediateGrid) }

                delay(100) // Wait for slide to finish

                // 2. Logic Merge
                val newScore = _uiState.value.score + result.points
                val highScore = maxOf(newScore, _uiState.value.highScore)
                
                // CHECK VICTORY
                // We check if we haven't won yet, haven't decided to keep playing, 
                // and if any NEW tile in the final grid is 2048.
                var victoryTriggered = false
                if (!_uiState.value.hasWon && !_uiState.value.keepPlaying) {
                    if (result.finalGrid.any { it.value == 2048 }) {
                        victoryTriggered = true
                    }
                }

                _uiState.update {
                    it.copy(
                        grid = result.finalGrid,
                        score = newScore,
                        highScore = highScore,
                        hasWon = if (victoryTriggered) true else it.hasWon
                    )
                }
                
                if (victoryTriggered) {
                     // Optional: Trigger a special victory sound/haptic here
                     if (_uiState.value.isHapticEnabled) _gameEvents.emit(GameEvent.Victory)
                }

                storage.saveData(result.finalGrid, newScore)

                delay(50)
                
                // Only spawn if we haven't just won (wait for user to click Keep Playing)
                // OR spawn anyway? Standard behavior: Spawn anyway, show dialog overlay.
                spawnTile(1)
                
                // Save again after spawn (so the new tile is remembered)
                storage.saveData(_uiState.value.grid, _uiState.value.score)
                
                checkGameOver()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    private fun spawnTile(count: Int = 1) {
        _uiState.update { current ->
            val occupied = current.grid.map { it.x to it.y }.toSet()
            val emptySlots = mutableListOf<Pair<Int, Int>>()
            for (x in 0..3) {
                for (y in 0..3) {
                    if ((x to y) !in occupied) emptySlots.add(x to y)
                }
            }
            if (emptySlots.isEmpty()) return@update current

            val newTiles = current.grid.toMutableList()
            repeat(count) {
                if (emptySlots.isNotEmpty()) {
                    val (x, y) = emptySlots.random()
                    emptySlots.remove(x to y)
                    newTiles.add(Tile(value = if (Math.random() < 0.9) 2 else 4, x = x, y = y, isNew = true))
                }
            }
            current.copy(grid = newTiles)
        }
    }

    // Helper logic to detect Game Over state
    private fun calculateGameOver(grid: List<Tile>): Boolean {
        if (grid.size < 16) return false
        val gridMap = grid.associate { (it.x to it.y) to it.value }
        for (x in 0..3) {
            for (y in 0..3) {
                val valCurrent = gridMap[x to y] ?: continue
                val valRight = gridMap[x + 1 to y]
                val valDown = gridMap[x to y + 1]
                if (valCurrent == valRight || valCurrent == valDown) return false
            }
        }
        return true
    }

    private fun checkGameOver() {
        if (calculateGameOver(_uiState.value.grid)) {
            _uiState.update { it.copy(isGameOver = true) }
        }
    }

    private fun processMove(tiles: List<Tile>, direction: Direction): MoveResult {
        val grid = Array(4) { Array<Tile?>(4) { null } }
        tiles.forEach { grid[it.x][it.y] = it }

        var points = 0
        var moved = false
        val finalTiles = mutableListOf<Tile>()
        val intermediateTiles = mutableListOf<Tile>()

        val isHorizontal = direction == Direction.LEFT || direction == Direction.RIGHT
        val range = if (direction == Direction.RIGHT || direction == Direction.DOWN) 3 downTo 0 else 0..3
        val step = if (direction == Direction.LEFT || direction == Direction.UP) 1 else -1

        for (i in 0..3) {
            val line = mutableListOf<Tile>()
            for (j in range) {
                val tile = if (isHorizontal) grid[j][i] else grid[i][j]
                if (tile != null) line.add(tile)
            }

            var placeIndex = if (direction == Direction.LEFT || direction == Direction.UP) 0 else 3
            var skipNext = false

            for (k in line.indices) {
                if (skipNext) {
                    skipNext = false
                    continue
                }
                val current = line[k]
                val next = line.getOrNull(k + 1)
                
                val newX = if (isHorizontal) placeIndex else i
                val newY = if (isHorizontal) i else placeIndex

                if (next != null && current.value == next.value) {
                    moved = true
                    val mergedValue = current.value * 2
                    points += mergedValue
                    
                    // Add both tiles to intermediate (for animation overlap)
                    intermediateTiles.add(current.copy(x = newX, y = newY))
                    intermediateTiles.add(next.copy(x = newX, y = newY))
                    
                    // Add single merged tile to final
                    finalTiles.add(current.copy(x = newX, y = newY, value = mergedValue, isNew = false))
                    skipNext = true
                } else {
                    if (current.x != newX || current.y != newY) moved = true
                    intermediateTiles.add(current.copy(x = newX, y = newY))
                    finalTiles.add(current.copy(x = newX, y = newY, isNew = false))
                }
                placeIndex += step
            }
        }
        return MoveResult(intermediateTiles, finalTiles, points, moved)
    }
}
