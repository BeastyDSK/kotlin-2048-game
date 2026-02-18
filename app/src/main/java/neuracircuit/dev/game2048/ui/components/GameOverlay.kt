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
                text = "Game Over!",
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
                        contentDescription = "Watch Ad",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Ad to Undo",
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
                    containerColor = GameColors.TextDark, // Or a specific "Try Again" color
                    contentColor = Color.White
                ),
                shape = ButtonShape,
                modifier = Modifier.height(50.dp).width(160.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text(
                    "Try Again", 
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
                text = "You Won!",
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
                    "Keep Playing", 
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
                    "New Game", 
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
                    "Restart?",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = GameColors.TextDark
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Are you sure you want to start over? Your current progress will be lost.",
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
                    // Cancel / Keep Playing Button
                    TextButton(
                        onClick = onKeepPlaying,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = ButtonShape
                    ) {
                        Text(
                            "Cancel", 
                            color = Color.Gray, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Confirm / New Game Button
                    Button(
                        onClick = onNewGame,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GameColors.TextDark
                        ),
                        shape = ButtonShape,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            "Restart", 
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
