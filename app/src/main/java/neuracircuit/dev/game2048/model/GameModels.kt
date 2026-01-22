package neuracircuit.dev.game2048.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Tile(
    val id: String = UUID.randomUUID().toString(),
    val value: Int,
    val x: Int,
    val y: Int,
    val isNew: Boolean = false
)

data class GameState(
    val grid: List<Tile> = emptyList(),
    val score: Int = 0,
    val highScore: Int = 0, // Added High Score to state
    val isGameOver: Boolean = false
)

enum class Direction { UP, DOWN, LEFT, RIGHT }

// Helper for returning multiple values from the move logic
data class MoveResult(
    val intermediateGrid: List<Tile>,
    val finalGrid: List<Tile>,
    val points: Int,
    val moved: Boolean
)
