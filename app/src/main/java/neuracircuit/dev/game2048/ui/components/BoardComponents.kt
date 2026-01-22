package neuracircuit.dev.game2048.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neuracircuit.dev.game2048.model.Tile
import neuracircuit.dev.game2048.ui.theme.GameColors
import neuracircuit.dev.game2048.ui.theme.getFontSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            .background(GameColors.tileColor(displayedValue), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$displayedValue",
            fontSize = getFontSize(displayedValue),
            fontWeight = FontWeight.Bold,
            color = GameColors.textColor(displayedValue)
        )
    }
}
