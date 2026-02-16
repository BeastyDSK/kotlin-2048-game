package neuracircuit.dev.game2048.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import neuracircuit.dev.game2048.BuildConfig
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.wrapContentHeight
import android.util.Log
import androidx.compose.ui.Alignment
import com.google.android.gms.ads.AdListener

/**
 * AD CONFIGURATION
 * Now securely pulling from build.gradle(app)
 */
object AdConfig {
    const val BANNER_ID = BuildConfig.APP_BOTTOM_BANNER_AD_ID
    const val INTERSTITIAL_RESET_ID = BuildConfig.GAMEOVER_RESET_INTERSTITIAL_AD_ID
    const val REWARDED_UNDO_ID = BuildConfig.UNDO_LAST_MOVE_REWARDED_AD_ID
//    val REWARDED_INTERSTITIAL_UNDO_ID = BuildConfig.UNDO_REWARDED_INTERSTITIAL_AD_ID
}

class AdManager(context: Context) {
    private val appContext = context.applicationContext

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    // --- INTERSTITIAL LOGIC (Game Over / Reset) ---
    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(appContext, AdConfig.INTERSTITIAL_RESET_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onDismissed()
                    loadInterstitialAd() // Auto-reload next ad
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    onDismissed()
                    loadInterstitialAd()
                }
            }
            interstitialAd?.show(activity)
        } else {
            onDismissed()
            loadInterstitialAd()
        }
    }

    // --- REWARDED LOGIC (Undo Move) ---
    fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(appContext, AdConfig.REWARDED_UNDO_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onDismissed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    onDismissed()
                    loadRewardedAd() // Auto-reload next rewarded ad
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onDismissed()
                    loadRewardedAd()
                }
            }
            rewardedAd?.show(activity) { _ ->
                onRewardEarned()
            }
        } else {
            onDismissed()
            loadRewardedAd()
        }
    }
}

/**
 * THE BANNER COMPOSABLE
 */
@Composable
fun AdaptiveBannerAd(modifier: Modifier = Modifier) {
    // 1. BoxWithConstraints gives us the exact available width for this specific container
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Convert the available layout width to an integer for AdMob
        val availableWidthDp = maxWidth.value.toInt()
        
        // Fallback to a standard width if constraints are weirdly 0
        val adWidth = if (availableWidthDp > 0) availableWidthDp else 320

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    // 2. Use the exact available width to calculate the AdSize
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                    
                    // 3. Force the specific ADAPTIVE BANNER TEST ID during development
                    adUnitId = if (BuildConfig.DEBUG) {
                        "ca-app-pub-3940256099942544/9214589741"
                    } else {
                        AdConfig.BANNER_ID
                    }
                    
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d("AdMobBanner", "SUCCESS: Banner Ad Loaded! Width used: $adWidth")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e("AdMobBanner", "FAILED: ${error.message} (Code: ${error.code})")
                        }
                    }

                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
