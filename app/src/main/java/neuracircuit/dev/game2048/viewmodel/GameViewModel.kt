package neuracircuit.dev.game2048.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import neuracircuit.dev.game2048.data.GameStorage
import neuracircuit.dev.game2048.data.SoundManager
import neuracircuit.dev.game2048.data.AnalyticsManager // Import added
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
import java.util.ArrayDeque

// --- DATA MODEL FOR UNDO ---
data class Snapshot(val tiles: List<Tile>, val score: Int)

sealed class GameEvent {
    data object Merge : GameEvent()
    data object Victory : GameEvent()
}

data class GameUiState(
    val grid: List<Tile> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0,
    val isGameOver: Boolean = false,
    val hasWon: Boolean = false,
    val keepPlaying: Boolean = false,
    val volume: Float = 0.5f,
    val isHapticEnabled: Boolean = true,
    val canUndo: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    private val storage = GameStorage(application)
    private val soundManager = SoundManager(application)
    private val analytics = AnalyticsManager(application) // Initialize Analytics

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>()
    val gameEvents: SharedFlow<GameEvent> = _gameEvents.asSharedFlow()
    
    private val history = ArrayDeque<Snapshot>()
    private val maxHistorySize = 1

    // Helper to track session-based achievements so we don't log "512 reached" multiple times per game
    private val reachedTilesSession = mutableSetOf<Int>()

    init {
        // Attempt to load previous game
        val savedGame = storage.loadData()
        val currentGrid = savedGame?.grid ?: emptyList()
        val currentScore = savedGame?.score ?: 0
        val currentHigh = savedGame?.highScore ?: storage.getHighScore()
        val settings = storage.getSettings()

        if (currentGrid.isNotEmpty()) {
            if (calculateGameOver(currentGrid)) {
                _uiState.value = GameUiState(
                    highScore = currentHigh,
                    volume = settings.volume,
                    isHapticEnabled = settings.hapticsEnabled
                )
                spawnTile(2)
                storage.clearActiveGame()
            } else {
                val has2048 = currentGrid.any { it.value >= 2048 }
                _uiState.value = GameUiState(
                    grid = currentGrid,
                    score = currentScore,
                    highScore = currentHigh,
                    volume = settings.volume,
                    isHapticEnabled = settings.hapticsEnabled,
                    hasWon = has2048,
                    keepPlaying = has2048
                )
            }
        } else {
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
    
    private fun saveState() {
        val currentGrid = _uiState.value.grid
        val currentScore = _uiState.value.score
        val gridCopy = currentGrid.map { it.copy() }
        if (history.size >= maxHistorySize) history.removeFirst()
        history.addLast(Snapshot(gridCopy, currentScore))
        _uiState.update { it.copy(canUndo = true) }
    }

    fun undoLastMove() {
        if (history.isEmpty()) return

        val snapshot = history.removeLast()
        
        // Log Undo Action
        analytics.logUndoUsed()

        _uiState.update { 
            it.copy(
                grid = snapshot.tiles,
                score = snapshot.score,
                isGameOver = false,
                canUndo = history.isNotEmpty(),
            )
        }
        
        soundManager.playMove(_uiState.value.volume)
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

    fun resetGame() {
        val currentHigh = storage.getHighScore()
        history.clear()
        reachedTilesSession.clear() // Reset session tracker

        // Log Game Start
        analytics.logGameStart()
        
        _uiState.update { 
            GameUiState(
                highScore = currentHigh,
                volume = it.volume,
                isHapticEnabled = it.isHapticEnabled,
                canUndo = false,
                hasWon = false,
                keepPlaying = false
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
                saveState()

                val vol = _uiState.value.volume
                if (result.points > 0) {
                    soundManager.playMerge(vol)
                    if (_uiState.value.isHapticEnabled) {
                        _gameEvents.emit(GameEvent.Merge)
                    }
                } else {
                    soundManager.playMove(vol)
                }

                _uiState.update { it.copy(grid = result.intermediateGrid) }
                delay(100)

                val newScore = _uiState.value.score + result.points
                val highScore = maxOf(newScore, _uiState.value.highScore)
                
                var victoryTriggered = false
                
                // Check if any tile is >= 512 and log it (if not already logged this session)
                result.finalGrid.forEach { tile ->
                    if (tile.value >= 512 && !reachedTilesSession.contains(tile.value)) {
                        analytics.logTileReached(tile.value)
                        reachedTilesSession.add(tile.value)
                    }
                    // Victory check
                    if (!_uiState.value.hasWon && !_uiState.value.keepPlaying && tile.value == 2048) {
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
                     if (_uiState.value.isHapticEnabled) _gameEvents.emit(GameEvent.Victory)
                }

                storage.saveData(result.finalGrid, newScore)
                delay(50)
                spawnTile(1)
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
            
            // Log Game Over
            val maxTile = _uiState.value.grid.maxOfOrNull { it.value } ?: 0
            analytics.logGameOver(_uiState.value.score, maxTile)
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
                if (skipNext) { skipNext = false; continue }
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
