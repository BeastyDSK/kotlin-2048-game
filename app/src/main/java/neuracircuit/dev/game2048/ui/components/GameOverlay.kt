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

@Composable
fun GameOverOverlay(onRestart: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Game Over", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(containerColor = GameColors.TextDark)
            ) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}

@Composable
fun VictoryOverlay(onKeepPlaying: () -> Unit, onNewGame: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCCEDC22E)), // Gold color with transparency
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "You Won!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Button(
                onClick = onKeepPlaying,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Keep Playing", color = GameColors.TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            TextButton(onClick = onNewGame) {
                Text("New Game", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

