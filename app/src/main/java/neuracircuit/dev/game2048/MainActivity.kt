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
import androidx.compose.runtime.setValue
import neuracircuit.dev.game2048.ui.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import neuracircuit.dev.game2048.data.AnalyticsManager
import androidx.core.view.WindowCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import neuracircuit.dev.game2048.data.ConsentManager
import com.google.android.gms.ads.MobileAds
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk

class MainActivity : ComponentActivity() {
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var consentManager: ConsentManager
    private val canRequestAdState: MutableState<Boolean> = mutableStateOf(false);
    private val playGamesSignedInState: MutableState<Boolean> = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        PlayGamesSdk.initialize(this)

        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated()
            .addOnSuccessListener { result ->
                if (result.isAuthenticated) {
                    playGamesSignedInState.value = true
                } else {
                    gamesSignInClient.signIn()
                        .addOnSuccessListener { signInResult ->
                            playGamesSignedInState.value = signInResult.isAuthenticated
                        }
                        .addOnFailureListener {
                            playGamesSignedInState.value = false
                        }
                }
            }
            .addOnFailureListener {
                gamesSignInClient.signIn()
                    .addOnSuccessListener { signInResult ->
                        playGamesSignedInState.value = signInResult.isAuthenticated
                    }
                    .addOnFailureListener {
                        playGamesSignedInState.value = false
                    }
            }

        // 1. Enable Edge-to-Edge
        enableEdgeToEdge()

        // 2. Immersive Mode: Hide System Bars
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        analyticsManager = AnalyticsManager(applicationContext)

        consentManager = ConsentManager(this)

        consentManager.gatherConsent(object : ConsentManager.OnConsentGatheringCompleteListener {
            override fun onConsentGatheringComplete(error: com.google.android.ump.FormError?) {
                if (consentManager.canRequestAds) {
                    analyticsManager.initializeAndEnable()

                    MobileAds.initialize(this@MainActivity) {
                        runOnUiThread {
                            canRequestAdState.value = true
                        }
                    }
                }
            }
        })

        setContent {
            val canRequestAds by canRequestAdState
            val playGamesSignedIn by playGamesSignedInState

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GameColors.Background
                ) {
                    var showSplash by rememberSaveable { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onAnimationFinished = {
                                showSplash = false
                            }
                        )
                    } else {
                        GameScreen(
                            canRequestAds = canRequestAds,
                            canUseCloudSave = playGamesSignedIn
                        )
                    }
                }
            }
        }
    }
}
