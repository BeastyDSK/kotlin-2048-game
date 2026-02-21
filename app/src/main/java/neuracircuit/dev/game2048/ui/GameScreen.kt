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
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalConfiguration
import neuracircuit.dev.game2048.ui.components.NewGameOverlay
import neuracircuit.dev.game2048.ads.AdaptiveBannerAd
import neuracircuit.dev.game2048.ads.AdManager
import neuracircuit.dev.game2048.ads.RewardType
import neuracircuit.dev.game2048.ads.RewardedAdResult
import neuracircuit.dev.game2048.ui.components.UndoAdOverlay

@Composable
fun GameScreen(viewModel: GameViewModel = viewModel(), canRequestAds: Boolean = false) {
    // Depending on if you renamed state to 'uiState' or kept 'gameState', update this line.
    // I am assuming 'uiState' based on the settings features, but if you kept 'gameState', change it back.
    val state by viewModel.uiState.collectAsState()

    // State to toggle Settings Dialog
    var showSettings by remember { mutableStateOf(false) }
    
    // State to toggle the Rewarded Ad Undo Dialog
    var showUndoAdOverlay by remember { mutableStateOf(false) }

    // Haptic Feedback Hook
    val haptic = LocalHapticFeedback.current

    // double back to exit
    val context = LocalContext.current
    val activity = LocalActivity.current as Activity
    
    // Load resource for toast
    val doubleBackMsg = stringResource(R.string.msg_double_back_exit)
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    
    // Screen Orientation
    val configuration = LocalConfiguration.current

    // --- AD MANAGER INITIALIZATION & LOADING ---
    val adManager = remember { AdManager(context) }

    LaunchedEffect(canRequestAds) {
        if (canRequestAds) {
            adManager.loadInterstitialAd()
            adManager.loadRewardedAd(RewardType.REVIVE)
            adManager.loadRewardedAd(RewardType.UNDO)
        }
    }

    // --- DRY RESET LOGIC ---
    val handleGameReset = {
        viewModel.resetGame()

        if (canRequestAds) {
            adManager.showInterstitialAd(activity, onDismissed = {})
        }
    }

    // --- DRY REVIVE LOGIC (Rewarded) ---
    val handleReviveRequest = {
        if (canRequestAds) {
            adManager.showRewardedAd(
                activity = activity,
                onResult = { result ->
                    when (result) {
                        RewardedAdResult.RewardGranted -> {
                            viewModel.undoLastMove()
                        }
                        RewardedAdResult.ClosedWithoutReward -> {
                            Toast.makeText(context, context.getString(R.string.msg_ad_not_completed), Toast.LENGTH_SHORT).show()
                        }
                        RewardedAdResult.NotAvailable -> {
                            Toast.makeText(context, context.getString(R.string.msg_ad_not_available), Toast.LENGTH_SHORT).show()
                        }
                        RewardedAdResult.FailedToShow -> {
                            Toast.makeText(context, context.getString(R.string.msg_ad_failed_to_show), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                rewardType = RewardType.REVIVE
            )
        }
    }

    // --- FREEMIUM UNDO LOGIC ---
    val onUndoClick = {
        if (state.freeUndosLeft > 0 || !canRequestAds) {
            viewModel.undoLastMove()
        } else {
            showUndoAdOverlay = true
        }
    }

    val onUndoAdDismiss = {
        showUndoAdOverlay = false
    }

    val onUndoAdConfirm = {
        adManager.showRewardedAd(
            activity = activity,
            onResult = { result ->
                when (result) {
                    RewardedAdResult.RewardGranted -> {
                        showUndoAdOverlay = false
                        viewModel.grantMoreUndos(1)
                    }
                    RewardedAdResult.ClosedWithoutReward -> {
                        Toast.makeText(context, context.getString(R.string.msg_ad_not_completed), Toast.LENGTH_SHORT).show()
                    }
                    RewardedAdResult.NotAvailable -> {
                        Toast.makeText(context, context.getString(R.string.msg_ad_not_available), Toast.LENGTH_SHORT).show()
                    }
                    RewardedAdResult.FailedToShow -> {
                        Toast.makeText(context, context.getString(R.string.msg_ad_failed_to_show), Toast.LENGTH_SHORT).show()
                    }
                }
            },
            rewardType = RewardType.UNDO
        )
    }

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
                onUndoClick = onUndoClick,
                onResetClick = { viewModel.toggleUserReset(true) },
                handleGameReset = handleGameReset,
                onReviveRequest = handleReviveRequest,
                canRequestAds = canRequestAds,
                showUndoAdOverlay = showUndoAdOverlay,
                onUndoAdDismiss = onUndoAdDismiss,
                onUndoAdConfirm = onUndoAdConfirm,
            )
        } else {
            PortraitGameLayout(
                state = state,
                viewModel = viewModel,
                modifier = layoutModifier,
                isOverlayVisible = isOverlayVisible,
                onSettingsClick = { showSettings = true },
                onUndoClick = onUndoClick,
                onResetClick = { viewModel.toggleUserReset(true) },
                handleGameReset = handleGameReset,
                onReviveRequest = handleReviveRequest,
                canRequestAds = canRequestAds,
                showUndoAdOverlay = showUndoAdOverlay,
                onUndoAdDismiss = onUndoAdDismiss,
                onUndoAdConfirm = onUndoAdConfirm,
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
    onResetClick: () -> Unit,
    handleGameReset: () -> Unit,
    canRequestAds: Boolean,
    onReviveRequest: () -> Unit,
    showUndoAdOverlay: Boolean,
    onUndoAdDismiss: () -> Unit,
    onUndoAdConfirm: () -> Unit
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
                // Undo Button with OVERFLOW Dynamic Badge
                Box {
                    ControlIcon(
                        icon = { 
                            val iconTint = if (state.freeUndosLeft == 0 && canRequestAds && state.canUndo && !isOverlayVisible) {
                                Color(0xFFFFD700) 
                            } else if (state.canUndo && !isOverlayVisible) {
                                GameColors.TextDark 
                            } else {
                                Color.Gray.copy(alpha = 0.5f)
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.ic_undo),
                                contentDescription = stringResource(R.string.desc_undo),
                                tint = iconTint
                            )
                        },
                        enabled = state.canUndo && !isOverlayVisible,
                        onClick = onUndoClick
                    )
                    
                    // The Number or AD Badge Overlay (Now OUTSIDE the ControlIcon)
                    if (state.canUndo && !isOverlayVisible) {
                        val badgeText = when {
                            state.freeUndosLeft > 0 -> state.freeUndosLeft.toString()
                            canRequestAds -> "AD"
                            else -> null
                        }
                        
                        if (badgeText != null) {
                            Surface(
                                color = if (badgeText == "AD") Color(0xFFE76F51) else GameColors.TextDark, 
                                shape = androidx.compose.foundation.shape.CircleShape, // Perfect Circle
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // Move to top-right corner
                                    .offset(x = 6.dp, y = (-6).dp) // BREAKS OUT of the bounds
                                    .size(20.dp) // Fixed size so it never stretches
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = badgeText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

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
            modifier = Modifier.aspectRatio(1f),
            handleGameReset = handleGameReset,
            onReviveRequest = onReviveRequest,
            canRequestAds = canRequestAds,
            showUndoAdOverlay = showUndoAdOverlay,
            onUndoAdDismiss = onUndoAdDismiss,
            onUndoAdConfirm = onUndoAdConfirm,
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
    onResetClick: () -> Unit,
    handleGameReset: () -> Unit,
    canRequestAds: Boolean,
    onReviveRequest: () -> Unit,
    showUndoAdOverlay: Boolean,
    onUndoAdDismiss: () -> Unit,
    onUndoAdConfirm: () -> Unit
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
                // Undo Button with OVERFLOW Dynamic Badge
                Box {
                    ControlIcon(
                        icon = { 
                            val iconTint = if (state.freeUndosLeft == 0 && canRequestAds && state.canUndo && !isOverlayVisible) {
                                Color(0xFFFFD700) 
                            } else if (state.canUndo && !isOverlayVisible) {
                                GameColors.TextDark 
                            } else {
                                Color.Gray.copy(alpha = 0.5f)
                            }
                            
                            Icon(
                                painter = painterResource(id = R.drawable.ic_undo),
                                contentDescription = stringResource(R.string.desc_undo),
                                tint = iconTint
                            )
                        },
                        enabled = state.canUndo && !isOverlayVisible,
                        onClick = onUndoClick
                    )
                    
                    // The Number or AD Badge Overlay (Now OUTSIDE the ControlIcon)
                    if (state.canUndo && !isOverlayVisible) {
                        val badgeText = when {
                            state.freeUndosLeft > 0 -> state.freeUndosLeft.toString()
                            canRequestAds -> "AD"
                            else -> null
                        }
                        
                        if (badgeText != null) {
                            Surface(
                                color = if (badgeText == "AD") Color(0xFFE76F51) else GameColors.TextDark, 
                                shape = androidx.compose.foundation.shape.CircleShape, // Perfect Circle
                                modifier = Modifier
                                    .align(Alignment.TopEnd) // Move to top-right corner
                                    .offset(x = 6.dp, y = (-6).dp) // BREAKS OUT of the bounds
                                    .size(20.dp) // Fixed size so it never stretches
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = badgeText,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

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
                    .aspectRatio(1f),
                handleGameReset = handleGameReset,
                onReviveRequest = onReviveRequest,
                canRequestAds = canRequestAds,
                showUndoAdOverlay = showUndoAdOverlay,
                onUndoAdDismiss = onUndoAdDismiss,
                onUndoAdConfirm = onUndoAdConfirm,
            )
        }
    }
}

// --- SHARED COMPONENTS ---

@Composable
fun GameBoard(
    state: neuracircuit.dev.game2048.viewmodel.GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier,
    handleGameReset: () -> Unit,
    canRequestAds: Boolean,
    onReviveRequest: () -> Unit,
    showUndoAdOverlay: Boolean,
    onUndoAdDismiss: () -> Unit,
    onUndoAdConfirm: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GameColors.GridBackground)
            .padding(4.dp)
    ) {
        // Calculate tile size based on the smaller dimension to prevent cutoff
        val boardSize = min(maxWidth, maxHeight)
        val tileSize = boardSize / 4

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
        
        // The remodeled logic neatly injects 'handleGameReset' into all 3 Overlays
        if (state.hasWon && !state.keepPlaying) {
            VictoryOverlay(
                onKeepPlaying = { viewModel.keepPlaying() },
                onNewGame = handleGameReset
            )
        } else if (state.isGameOver) {
            GameOverOverlay(
                onRestart = handleGameReset,
                canRevive = state.canUndo && canRequestAds,
                onRevive = onReviveRequest
            )
        } else if (state.isUserReset) {
            NewGameOverlay(
                onKeepPlaying = { viewModel.toggleUserReset(false) },
                onNewGame = handleGameReset
            )
        }

        if (showUndoAdOverlay) {
            UndoAdOverlay(
                onDismiss = onUndoAdDismiss,
                onConfirm = onUndoAdConfirm
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
