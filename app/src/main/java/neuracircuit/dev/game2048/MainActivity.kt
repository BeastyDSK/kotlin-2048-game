package neuracircuit.dev.game2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

// -------------------------------------------------------------------------------------
// 1. DATA MODELS
// -------------------------------------------------------------------------------------

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
    val isGameOver: Boolean = false
)

enum class Direction { UP, DOWN, LEFT, RIGHT }

// -------------------------------------------------------------------------------------
// 2. VIEW MODEL & GAME LOGIC
// -------------------------------------------------------------------------------------

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    init {
        resetGame()
    }

    fun resetGame() {
        _gameState.value = GameState()
        spawnTile(2)
    }

    fun handleSwipe(direction: Direction) {
        viewModelScope.launch {
            val currentGrid = _gameState.value.grid
            
            // 1. Calculate the Move
            // We get two grids: 
            // - intermediateGrid: Tiles moved to destination (overlapping, values unchanged)
            // - finalGrid: The clean board after merges occur
            val (intermediateGrid, finalGrid, points, moved) = processMove(currentGrid, direction)

            if (moved) {
                // 2. Stage One: Trigger Animation
                // We update the grid to the intermediate state. 
                // The UI sees two '2's at the same spot. One slid there, one was waiting.
                _gameState.update { it.copy(grid = intermediateGrid) }

                // Wait for the slide to finish (slightly less than animation duration to feel snappy)
                delay(100)

                // 3. Stage Two: Finalize Merge
                // Replace overlaps with the single merged tile (e.g., '4')
                _gameState.update {
                    it.copy(grid = finalGrid, score = it.score + points)
                }

                // 4. Spawn new tile (Standard 2048 delay logic)
                delay(50) 
                spawnTile(1)
                
                checkGameOver()
            }
        }
    }

    private fun spawnTile(count: Int = 1) {
        _gameState.update { current ->
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

    private fun checkGameOver() {
        val grid = _gameState.value.grid
        if (grid.size < 16) return

        val gridMap = grid.associate { (it.x to it.y) to it.value }
        for (x in 0..3) {
            for (y in 0..3) {
                val valCurrent = gridMap[x to y] ?: continue
                val valRight = gridMap[x + 1 to y]
                val valDown = gridMap[x to y + 1]
                if (valCurrent == valRight || valCurrent == valDown) return
            }
        }
        _gameState.update { it.copy(isGameOver = true) }
    }

    // Returns: Quadruple(IntermediateGrid, FinalGrid, ScoreAdded, WasMoveValid)
    private fun processMove(tiles: List<Tile>, direction: Direction): Quadruple<List<Tile>, List<Tile>, Int, Boolean> {
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

            // Logic to track merges and overlaps
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
                    // MERGE HAPPENING
                    moved = true
                    val mergedValue = current.value * 2
                    points += mergedValue

                    // 1. Intermediate State (Visuals)
                    // We want BOTH tiles to exist at the destination for the animation
                    // 'current' moves to dest. 'next' moves to dest.
                    // IMPORTANT: Ensure the one "sliding in" is added last so it draws on top? 
                    // Or actually, just having both at the same X,Y is enough.
                    intermediateTiles.add(current.copy(x = newX, y = newY))
                    intermediateTiles.add(next.copy(x = newX, y = newY))

                    // 2. Final State (Logic)
                    // Only one tile remains with the new value.
                    // We preserve the ID of 'current' (or 'next')?
                    // To get the "Pop" effect, we usually keep the 'stationary' ID or just the first one.
                    // Let's keep 'current' ID and upgrade its value.
                    finalTiles.add(current.copy(x = newX, y = newY, value = mergedValue, isNew = false))
                    
                    skipNext = true
                } else {
                    // NO MERGE
                    if (current.x != newX || current.y != newY) moved = true
                    
                    intermediateTiles.add(current.copy(x = newX, y = newY))
                    finalTiles.add(current.copy(x = newX, y = newY, isNew = false))
                }
                
                placeIndex += step
            }
        }

        return Quadruple(intermediateTiles, finalTiles, points, moved)
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// -------------------------------------------------------------------------------------
// 3. UI IMPLEMENTATION (COMPOSE)
// -------------------------------------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFAF8EF)
                ) {
                    GameScreen()
                }
            }
        }
    }
}

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    val state by viewModel.gameState.collectAsState()
    
    // Gesture Detection
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val minSwipeDist = 50f // Sensitivity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val absX = abs(offsetX)
                        val absY = abs(offsetY)
                        if (max(absX, absY) > minSwipeDist) {
                            if (absX > absY) {
                                if (offsetX > 0) viewModel.handleSwipe(Direction.RIGHT)
                                else viewModel.handleSwipe(Direction.LEFT)
                            } else {
                                if (offsetY > 0) viewModel.handleSwipe(Direction.DOWN)
                                else viewModel.handleSwipe(Direction.UP)
                            }
                        }
                        offsetX = 0f
                        offsetY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "2048",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF776E65)
            )
            ScoreBoard(score = state.score)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Game Board Container
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFBBADA0))
                .padding(4.dp)
        ) {
            val tileSize = (maxWidth - 8.dp) / 4 // 4dp padding total inside / 4 slots
            val density = LocalDensity.current

            // 1. Static Background Grid
            for (x in 0..3) {
                for (y in 0..3) {
                    GridSlot(tileSize, x, y)
                }
            }

            // 2. Active Tiles (Overlays)
            // We use 'key' to ensure Compose tracks individual tiles by ID for animations
            state.grid.forEach { tile ->
                key(tile.id) {
                    AnimatedTile(tile = tile, tileSize = tileSize)
                }
            }
            
            if (state.isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Game Over", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Button(onClick = { viewModel.resetGame() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreBoard(score: Int) {
    Column(
        modifier = Modifier
            .background(Color(0xFFBBADA0), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SCORE", fontSize = 12.sp, color = Color(0xFFEEE4DA), fontWeight = FontWeight.Bold)
        Text(score.toString(), fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GridSlot(size: Dp, x: Int, y: Int) {
    val xOffset = size * x
    val yOffset = size * y
    
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                translationX = xOffset.toPx()
                translationY = yOffset.toPx()
            }
            .padding(4.dp)
            .background(Color(0xFFCDC1B4), RoundedCornerShape(4.dp))
    )
}

// -------------------------------------------------------------------------------------
// 4. ANIMATION LOGIC (Visual Buffer Pattern)
// -------------------------------------------------------------------------------------

@Composable
fun AnimatedTile(tile: Tile, tileSize: Dp) {
    // Current pixel target
    val targetX = tileSize * tile.x
    val targetY = tileSize * tile.y

    // Animation States for Movement
    // We use Animatable to manually control the sequence of "Slide -> Update Text -> Pop"
    val animX = remember { Animatable(targetX.value) }
    val animY = remember { Animatable(targetY.value) }
    val scale = remember { Animatable(if (tile.isNew) 0f else 1f) }

    // Visual Buffer: Maintain the *displayed* value separately from the *data* value
    // This allows us to keep showing "2" while sliding, then flip to "4" on arrival.
    var displayedValue by remember { mutableIntStateOf(tile.value) }

    // Logic: Observe data changes
    LaunchedEffect(tile.x, tile.y, tile.value) {
        val prevX = animX.value
        val prevY = animY.value
        
        // 1. Animate Movement (Standard Slide)
        // We launch parallel animations for X and Y
        launch {
            animX.animateTo(
                targetValue = targetX.value,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
        }
        launch {
            animY.animateTo(
                targetValue = targetY.value,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
            )
        }
        
        // 2. Handle Merges (Visual Buffer Update)
        // If the data value changed (e.g., 2 -> 4), wait for movement to finish, then pop.
        if (tile.value != displayedValue) {
            // Wait approximately for the slide to finish (100ms safe buffer within the 150ms window)
            delay(100) 
            
            // Update the text
            displayedValue = tile.value
            
            // Pop animation
            scale.snapTo(1.2f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }
    
    // Initial Spawn Animation
    LaunchedEffect(Unit) {
        if (tile.isNew) {
            scale.animateTo(1f, tween(200))
        }
    }

    // Visual Buffer Logic
    // If the tile is being "merged into" (value changes 2->4), 
    // we want that change to happen ONLY after the slide is fully done.
    
    LaunchedEffect(tile.value) {
        if (tile.value != displayedValue) {
            // This delay must match the gap between Stage 1 and Stage 2 in ViewModel
            // We used delay(100) in ViewModel + delay(150) in animation
            // Ideally, we just wait for the slide to settle.
            delay(50) 
            displayedValue = tile.value
            scale.snapTo(1.2f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    // Render
    Box(
        modifier = Modifier
            .size(tileSize)
            .graphicsLayer {
                // High performance translation
                translationX = animX.value.dp.toPx()
                translationY = animY.value.dp.toPx()
                scaleX = scale.value
                scaleY = scale.value
            }
            .padding(4.dp)
            .background(getTileColor(displayedValue), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$displayedValue",
            fontSize = getFontSize(displayedValue),
            fontWeight = FontWeight.Bold,
            color = getTextColor(displayedValue)
        )
    }
}

// -------------------------------------------------------------------------------------
// 5. STYLES & UTILS
// -------------------------------------------------------------------------------------

fun getTileColor(value: Int): Color {
    return when (value) {
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }
}

fun getTextColor(value: Int): Color {
    return if (value <= 4) Color(0xFF776E65) else Color(0xFFF9F6F2)
}

fun getFontSize(value: Int): TextUnit {
    return when {
        value < 100 -> 32.sp
        value < 1000 -> 28.sp
        else -> 24.sp
    }
}
