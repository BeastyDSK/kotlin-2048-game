package neuracircuit.dev.game2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neuracircuit.dev.game2048.ui.theme.GameColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import neuracircuit.dev.game2048.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.BoxWithConstraints

// Common constants for consistent styling
private val OverlayCornerRadius = 16.dp
private val ButtonShape = RoundedCornerShape(8.dp)

@Composable
fun GameOverOverlay(
    onRestart: () -> Unit,
    canRevive: Boolean = false,
    onRevive: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEE4DA).copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.msg_game_over),
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GameColors.TextDark, // Dark Brown/Black
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (canRevive) {
                Button(
                    onClick = onRevive,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.tileColor(2048)), // Uses the gold 2048 color
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        // .fillMaxWidth(0.7f)
                        .height(56.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.cnt_desc_watch_ad),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_watch_ad_to_undo),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GameColors.TextDark,
                    contentColor = Color.White
                ),
                shape = ButtonShape,
                modifier = Modifier.height(50.dp).width(160.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.btn_try_again),
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VictoryOverlay(onKeepPlaying: () -> Unit, onNewGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Gold color with slightly higher opacity for impact
            .background(Color(0xFFEDC22E).copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.msg_you_won),
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                // Add a subtle shadow for pop against the gold
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color.Black.copy(alpha = 0.2f),
                        blurRadius = 4f
                    )
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Primary Action: Keep Playing
            Button(
                onClick = onKeepPlaying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.9f),
                    contentColor = GameColors.TextDark
                ),
                shape = ButtonShape,
                modifier = Modifier.height(50.dp).fillMaxWidth(0.7f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    stringResource(R.string.btn_keep_playing), 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Secondary Action: New Game (More subtle)
            OutlinedButton(
                onClick = onNewGame,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shape = ButtonShape,
                modifier = Modifier.height(50.dp).fillMaxWidth(0.7f)
            ) {
                Text(
                    stringResource(R.string.btn_new_game), 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun NewGameOverlay(onKeepPlaying: () -> Unit, onNewGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Dim background
            .padding(24.dp), // Safe area
        contentAlignment = Alignment.Center
    ) {
        // Card-like container for the dialog
        Card(
            shape = RoundedCornerShape(OverlayCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.restart),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = GameColors.TextDark
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.msg_confirm_restart),
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center, // Crucial for multi-line text
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onKeepPlaying,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = ButtonShape
                    ) {
                        Text(
                            text = stringResource(R.string.btn_keep_playing),
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onNewGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.TextDark
                        ),
                        shape = ButtonShape,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_new_game),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateReadyOverlay(
    onLater: () -> Unit,
    onRestartNow: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(OverlayCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_update_ready),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = GameColors.TextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.msg_update_ready),
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onLater,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ButtonShape
                    ) {
                        Text(
                            text = stringResource(R.string.btn_later),
                            color = Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onRestartNow,
                        colors = ButtonDefaults.buttonColors(containerColor = GameColors.TextDark),
                        shape = ButtonShape,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_restart_now),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UndoAdOverlay(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Dim background
            .padding(24.dp), // Safe area
        contentAlignment = Alignment.Center
    ) {
        // Card-like container for the dialog
        Card(
            shape = RoundedCornerShape(OverlayCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.msg_out_of_undos),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = GameColors.TextDark,
                    lineHeight = 32.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.msg_watch_ad_undo),
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center, // Crucial for multi-line text
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ButtonShape
                    ) {
                        Text(
                            stringResource(R.string.btn_cancel), 
                            color = Color.Gray, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Confirm / New Game Button
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.TextDark
                        ),
                        shape = ButtonShape,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            stringResource(R.string.btn_watch), 
                            color = Color.White, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSyncOverlay(
    cloudScore: Int,
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)) // Dim background
            .padding(24.dp), // Safe area
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(OverlayCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.msg_cloud_save_found),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameColors.TextDark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = stringResource(R.string.msg_cloud_save_details, cloudScore),
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Reject Button
                    TextButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ButtonShape
                    ) {
                        Text(
                            text = stringResource(R.string.btn_keep_local), 
                            color = Color.Gray, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Accept Button
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.TextDark
                        ),
                        shape = ButtonShape,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_load_cloud), 
                            color = Color.White, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(OverlayCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.title_how_to_play),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameColors.TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tutorial),
                        contentDescription = null,
                        tint = GameColors.TextDark
                    )
                    Text(
                        text = stringResource(R.string.tutorial_rule_1),
                        fontSize = 16.sp,
                        color = Color.Gray,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tutorial),
                        contentDescription = null,
                        tint = GameColors.TextDark
                    )
                    Text(
                        text = stringResource(R.string.tutorial_rule_2),
                        fontSize = 16.sp,
                        color = Color.Gray,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tutorial),
                        contentDescription = null,
                        tint = GameColors.TextDark
                    )
                    Text(
                        text = stringResource(R.string.tutorial_rule_3),
                        fontSize = 16.sp,
                        color = Color.Gray,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GameColors.TextDark),
                    shape = ButtonShape,
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_got_it),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
