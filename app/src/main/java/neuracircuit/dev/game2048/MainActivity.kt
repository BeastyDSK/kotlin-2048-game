package neuracircuit.dev.game2048

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import neuracircuit.dev.game2048.ui.GameScreen
import neuracircuit.dev.game2048.ui.theme.GameColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import neuracircuit.dev.game2048.ui.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import neuracircuit.dev.game2048.data.AnalyticsManager
import androidx.core.view.WindowCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private lateinit var analyticsManager: AnalyticsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // 1. Enable Edge-to-Edge
        enableEdgeToEdge()

        // 2. Immersive Mode: Hide System Bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // 2. Initialize Analytics Helper
        analyticsManager = AnalyticsManager(applicationContext)
        
        // Note: If you implement the GDPR flow later, you would trigger it here
        // and call analyticsManager.initializeAndEnable() only after consent.
        // For now, we assume the ViewModel handles the safe/lazy logging.

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GameColors.Background
                ) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onAnimationFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        GameScreen()
                    }
                }
            }
        }
    }
}
