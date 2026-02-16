package neuracircuit.dev.game2048.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import neuracircuit.dev.game2048.ui.components.VictoryOverlay
import neuracircuit.dev.game2048.ui.components.dialogs.SettingsDialog
import kotlin.math.abs
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import neuracircuit.dev.game2048.R
import androidx.compose.ui.unit.min
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import neuracircuit.dev.game2048.ui.components.NewGameOverlay
import neuracircuit.dev.game2048.ads.AdaptiveBannerAd // Added Banner Ad Import

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel(), canRequestAds: Boolean = false) {
    // Depending on if you renamed state to 'uiState' or kept 'gameState', update this line.
    // I am assuming 'uiState' based on the settings features, but if you kept 'gameState', change it back.
    val state by viewModel.uiState.collectAsState()

    // State to toggle Settings Dialog
    var showSettings by remember { mutableStateOf(false) }

    // Haptic Feedback Hook
    val haptic = LocalHapticFeedback.current

    // double back to exit
    val context = LocalContext.current
    
    // Load resource for toast
    val doubleBackMsg = stringResource(R.string.msg_double_back_exit)
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    
    // Screen Orientation
    val configuration = LocalConfiguration.current

    // Listen for Merge Events from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.gameEvents.collect { event ->
            if (event is GameEvent.Merge) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } else if (event is GameEvent.Victory) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Swipe Logic
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val minSwipeDist = 50f

    // Helper to determine if input should be blocked
    val isOverlayVisible = (state.hasWon && !state.keepPlaying) || state.isGameOver || state.isUserReset

    // Shared Swipe Modifier to prevent code duplication
    val swipeModifier = Modifier
        .fillMaxSize()
        .pointerInput(isOverlayVisible) { // Key checks if overlay state changes
            if (!isOverlayVisible) {
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
        }

    // --- SCAFFOLD WRAPPER ---
    Scaffold(
        containerColor = GameColors.Background,
        bottomBar = {
            if (canRequestAds) {
                AdaptiveBannerAd()
            }
        }
    ) { paddingValues ->
        
        // Combine swipe logic with safe Scaffold padding and standard layout padding
        val layoutModifier = swipeModifier
            .padding(paddingValues)
            .padding(16.dp)
            
        // --- LAYOUT SELECTION ---
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LandscapeGameLayout(
                state = state,
                viewModel = viewModel,
                modifier = layoutModifier,
                isOverlayVisible = isOverlayVisible,
                onSettingsClick = { showSettings = true },
                onUndoClick = { viewModel.undoLastMove() },
                onResetClick = { viewModel.toggleUserReset(true) }
            )
        } else {
            PortraitGameLayout(
                state = state,
                viewModel = viewModel,
                modifier = layoutModifier,
                isOverlayVisible = isOverlayVisible,
                onSettingsClick = { showSettings = true },
                onUndoClick = { viewModel.undoLastMove() },
                onResetClick = { viewModel.toggleUserReset(true) }
            )
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
            Toast.makeText(context, doubleBackMsg, Toast.LENGTH_SHORT).show()
        }
    }
}

// --- SUB-LAYOUTS ---

@Composable
private fun PortraitGameLayout(
    state: neuracircuit.dev.game2048.viewmodel.GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier,
    isOverlayVisible: Boolean,
    onSettingsClick: () -> Unit,
    onUndoClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 1. HEADER ROW (Title + Score)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = GameColors.TextDark
            )
            ScoreBoard(score = state.score, highScore = state.highScore)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. CONTROLS ROW (Above the Board - "Classic Style")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, // Aligned Right, under the scores
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo Button
                ControlIcon(
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.desc_undo),
                            tint = if (state.canUndo && !isOverlayVisible) GameColors.TextDark else Color.Gray.copy(alpha = 0.5f)
                        )
                    },
                    enabled = state.canUndo && !isOverlayVisible,
                    onClick = onUndoClick
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Reset Button
                ControlIcon(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_reset),
                            contentDescription = "New Game",
                            tint = if (!state.isUserReset && !isOverlayVisible) GameColors.TextDark else Color.Gray.copy(alpha = 0.5f)
                        ) 
                    },
                    enabled = !isOverlayVisible,
                    onClick = onResetClick
                )
            }

            // Settings Button
            ControlIcon(
                icon = { 
                   Icon(
                       painter = painterResource(R.drawable.ic_settings),
                       contentDescription = stringResource(R.string.desc_settings),
                       tint = GameColors.TextDark
                   )
                },
                enabled = true,
                onClick = onSettingsClick
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- GAME BOARD ---
        GameBoard(
            state = state,
            viewModel = viewModel,
            modifier = Modifier
                .aspectRatio(1f)
        )
    }
}

@Composable
private fun LandscapeGameLayout(
    state: neuracircuit.dev.game2048.viewmodel.GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier,
    isOverlayVisible: Boolean,
    onSettingsClick: () -> Unit,
    onUndoClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Header, Score, Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 40.sp, // Reduced font for landscape
                fontWeight = FontWeight.Bold,
                color = GameColors.TextDark
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ScoreBoard(score = state.score, highScore = state.highScore)
            
            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(horizontalArrangement = Arrangement.Center) {
                ControlIcon(
                    icon = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_undo),
                            contentDescription = stringResource(R.string.desc_undo),
                            tint = if (state.canUndo && !isOverlayVisible) GameColors.TextDark else Color.Gray.copy(alpha = 0.5f)
                        )
                    },
                    enabled = state.canUndo && !isOverlayVisible,
                    onClick = onUndoClick
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Reset Button
                ControlIcon(
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_reset),
                            contentDescription = "New Game",
                            tint = GameColors.TextDark
                        ) 
                    },
                    enabled = !isOverlayVisible,
                    onClick = onResetClick
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Settings Button
                ControlIcon(
                    icon = { 
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.desc_settings),
                            tint = GameColors.TextDark
                        )
                    },
                    enabled = !isOverlayVisible,
                    onClick = onSettingsClick
                )
            }
        }

        // Right Box: Game Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                state = state,
                viewModel = viewModel,
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxHeight() // Ensure it uses full height
            )
        }
    }
}

// --- SHARED COMPONENTS ---

@Composable
fun GameBoard(
    state: neuracircuit.dev.game2048.viewmodel.GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GameColors.GridBackground)
            .padding(4.dp)
    ) {
        // Calculate tile size based on the smaller dimension to prevent cutoff
        val boardSize = min(maxWidth, maxHeight)
        val tileSize = (boardSize - 8.dp) / 4

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

        // --- OVERLAYS ---
        
        // 1. Victory Overlay (Priority over Game Over, but user can dismiss it)
        if (state.hasWon && !state.keepPlaying) {
            VictoryOverlay(
                onKeepPlaying = { viewModel.keepPlaying() },
                onNewGame = { viewModel.resetGame() }
            )
        } else if (state.isGameOver) {
            GameOverOverlay(onRestart = { viewModel.resetGame() })
        } else if (state.isUserReset) {
            NewGameOverlay(
            onKeepPlaying = { viewModel.keepPlaying() },
            onNewGame = { 
                viewModel.resetGame()
            }
        )
        }
    }
}

// Helper Composable
@Composable
fun ControlIcon(
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .background(
                // Use a lighter color for buttons to stand out against the background
                color = if (enabled) GameColors.GridBackground.copy(alpha = 0.3f) else Color.Transparent, 
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        icon()
    }
}
