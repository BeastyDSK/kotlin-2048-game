package neuracircuit.dev.game2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import neuracircuit.dev.game2048.model.Direction
import neuracircuit.dev.game2048.ui.components.AnimatedTile
import neuracircuit.dev.game2048.ui.components.GridSlot
import neuracircuit.dev.game2048.ui.components.ScoreBoard
import neuracircuit.dev.game2048.viewmodel.GameViewModel
import neuracircuit.dev.game2048.ui.theme.GameColors
import neuracircuit.dev.game2048.viewmodel.GameEvent
import neuracircuit.dev.game2048.ui.components.GameOverOverlay
import neuracircuit.dev.game2048.ui.components.dialogs.SettingsDialog
import kotlin.math.abs
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.widget.Toast

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel()) {
    // Depending on if you renamed state to 'uiState' or kept 'gameState', update this line.
    // I am assuming 'uiState' based on the settings features, but if you kept 'gameState', change it back.
    val state by viewModel.uiState.collectAsState()

    // State to toggle Settings Dialog
    var showSettings by remember { mutableStateOf(false) }

    // Haptic Feedback Hook
    val haptic = LocalHapticFeedback.current

    // double back to exit
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Listen for Merge Events from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.gameEvents.collect { event ->
            if (event is GameEvent.Merge) {
                // Trigger heavy click (LongPress) or TextHandleMove for crisp feeling
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Swipe Logic
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val minSwipeDist = 50f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val absX = abs(offsetX)
                        val absY = abs(offsetY)
                        if (java.lang.Float.max(absX, absY) > minSwipeDist) {
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
        // --- HEADER ROW (Title + Settings Icon) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("2048", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = GameColors.TextDark)
            
            // Settings Button
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .background(GameColors.GridBackground, RoundedCornerShape(8.dp))
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings, 
                    contentDescription = "Settings", 
                    tint = Color.White
                )
            }
        }
        
        // --- SCORE BOARD ---
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ScoreBoard(score = state.score, highScore = state.highScore)
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- GAME BOARD ---
        BoxWithConstraints(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(GameColors.GridBackground)
                .padding(4.dp)
        ) {
            val tileSize = (maxWidth - 8.dp) / 4

            // Static Background
            for (x in 0..3) {
                for (y in 0..3) {
                    GridSlot(tileSize, x, y)
                }
            }

            // Dynamic Tiles
            state.grid.forEach { tile ->
                key(tile.id) {
                    AnimatedTile(tile = tile, tileSize = tileSize)
                }
            }

            if (state.isGameOver) {
                GameOverOverlay(onRestart = { viewModel.resetGame() })
            }
        }
    }

    // --- SETTINGS DIALOG ---
    if (showSettings) {
        SettingsDialog(
            volume = state.volume,
            isHapticsEnabled = state.isHapticEnabled,
            onVolumeChange = { viewModel.setVolume(it) },
            onVolumeChangeFinished = { viewModel.playTestSound() },
            onHapticsChange = { viewModel.toggleHaptics(it) },
            onDismiss = { showSettings = false }
        )
    }

    // --- DOUBLE BACK TO EXIT LOGIC ---
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            // Exit the app
            (context as? Activity)?.finish()
        } else {
            // Show toast and update time
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
