package neuracircuit.dev.game2048.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    val density = LocalDensity.current

    // 1. Animation States
    // We use Animatable for precise control over the sequence
    val offsetAnimation = remember { Animatable(300f) } // Start 300dp apart (off-center)
    val scaleAnimation = remember { Animatable(0f) }    // Logo starts at scale 0
    val tilesVisible = remember { mutableStateOf(true) }

    // 2. The Animation Sequence
    LaunchedEffect(Unit) {
        // Step A: Slide tiles in
        offsetAnimation.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )

        // Step B: Impact! Hide tiles, show Logo
        tilesVisible.value = false

        // Step C: Pop the Logo (Bounce effect)
        scaleAnimation.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Step D: Wait a moment for user to see it, then navigate
        delay(500)
        onAnimationFinished()
    }

    // 3. The UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8EF)), // Standard 2048 background color
        contentAlignment = Alignment.Center
    ) {

        // The two sliding tiles (Only visible before impact)
        if (tilesVisible.value) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Left Tile (slides from left)
                GameTile(
                    text = "1024",
                    modifier = Modifier
                        .offset(x = -offsetAnimation.value.dp) // Negative offset
                        .size(100.dp)
                )

                // Right Tile (slides from right)
                GameTile(
                    text = "1024",
                    modifier = Modifier
                        .offset(x = offsetAnimation.value.dp) // Positive offset
                        .size(100.dp)
                )
            }
        }

        // The Main Logo (Only visible after impact)
        if (!tilesVisible.value) {
            GameTile(
                text = "2048",
                modifier = Modifier
                    .scale(scaleAnimation.value) // Applies the bounce
                    .size(100.dp)
            )
        }
    }
}

// Reusable Tile Component
@Composable
fun GameTile(
    text: String,
    modifier: Modifier = Modifier,
    textSize: Int = 32
) {
    Box(
        modifier = modifier
            .padding(4.dp) // Gutter between tiles
            .background(
                color = Color(0xFFEDC22E), // Classic 2048 Gold
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
