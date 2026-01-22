package neuracircuit.dev.game2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import neuracircuit.dev.game2048.model.Direction
import neuracircuit.dev.game2048.ui.components.AnimatedTile
import neuracircuit.dev.game2048.ui.components.GridSlot
import neuracircuit.dev.game2048.ui.components.ScoreBoard
import neuracircuit.dev.game2048.viewmodel.GameViewModel
import kotlin.math.abs
import kotlin.math.max

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
