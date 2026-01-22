package neuracircuit.dev.game2048.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ScoreBoard(score: Int, highScore: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScoreBox(label = "SCORE", value = score)
        ScoreBox(label = "BEST", value = highScore)
    }
}

@Composable
fun GridSlot(size: Dp, x: Int, y: Int) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                translationX = (size * x).toPx()
                translationY = (size * y).toPx()
            }
            .padding(4.dp)
            .background(GameColors.EmptySlot, RoundedCornerShape(4.dp))
    )
}

@Composable
fun AnimatedTile(tile: Tile, tileSize: Dp) {
    val targetX = tileSize * tile.x
    val targetY = tileSize * tile.y

    val animX = remember { Animatable(targetX.value) }
    val animY = remember { Animatable(targetY.value) }
    val scale = remember { Animatable(if (tile.isNew) 0f else 1f) }

    var displayedValue by remember { mutableIntStateOf(tile.value) }

    // Movement Animation
    LaunchedEffect(tile.x, tile.y) {
        launch {
            animX.animateTo(targetX.value, tween(150, easing = FastOutSlowInEasing))
        }
        launch {
            animY.animateTo(targetY.value, tween(150, easing = FastOutSlowInEasing))
        }
    }

    // Merge/Pop Animation (Visual Buffer)
    LaunchedEffect(tile.value) {
        if (tile.value != displayedValue) {
            delay(50) 
            displayedValue = tile.value
            scale.snapTo(1.2f)
            scale.animateTo(1f, spring(0.5f))
        }
    }
    
    // Spawn Animation
    LaunchedEffect(Unit) {
        if (tile.isNew) scale.animateTo(1f, tween(200))
    }

    Box(
        modifier = Modifier
            .size(tileSize)
            .graphicsLayer {
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

@Composable
fun ScoreBox(label: String, value: Int) {
    Column(
        modifier = Modifier
            .background(GameColors.GridBackground, RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = GameColors.tileColor(2), fontWeight = FontWeight.Bold)
        Text(value.toString(), fontSize = 20.sp, color = GameColors.tileColor(2), fontWeight = FontWeight.Bold)
    }
}
