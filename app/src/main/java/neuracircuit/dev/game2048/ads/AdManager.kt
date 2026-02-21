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
    const val REWARDED_REVIVE_ID = BuildConfig.UNDO_LAST_MOVE_REWARDED_AD_ID
    const val REWARDED_UNDO_ID = BuildConfig.UNDO_REWARDED_AD_ID
}

enum class RewardType {
    UNDO,
    REVIVE
}

sealed interface RewardedAdResult {
    data object RewardGranted : RewardedAdResult
    data object ClosedWithoutReward : RewardedAdResult
    data object NotAvailable : RewardedAdResult
    data object FailedToShow : RewardedAdResult
}

class AdManager(context: Context) {
    private val appContext = context.applicationContext

    private var interstitialAd: InterstitialAd? = null
    private var rewardedReviveAd: RewardedAd? = null
    private var rewardedUndoAd: RewardedAd? = null

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
    fun loadRewardedAd(rewardType: RewardType = RewardType.UNDO) {
        val adUnitId = when (rewardType) {
            RewardType.REVIVE -> AdConfig.REWARDED_REVIVE_ID
            RewardType.UNDO -> AdConfig.REWARDED_UNDO_ID
        }

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(appContext, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                when (rewardType) {
                    RewardType.REVIVE -> rewardedReviveAd = ad
                    RewardType.UNDO -> rewardedUndoAd = ad
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                when (rewardType) {
                    RewardType.REVIVE -> rewardedReviveAd = null
                    RewardType.UNDO -> rewardedUndoAd = null
                }
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        rewardType: RewardType = RewardType.UNDO,
        onResult: (RewardedAdResult) -> Unit
    ) {
        val adToShow = when (rewardType) {
            RewardType.REVIVE -> rewardedReviveAd
            RewardType.UNDO -> rewardedUndoAd
        }

        if (adToShow != null) {
            var rewardEarned = false
            adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    when (rewardType) {
                        RewardType.REVIVE -> rewardedReviveAd = null
                        RewardType.UNDO -> rewardedUndoAd = null
                    }
                    if (rewardEarned) {
                        onResult(RewardedAdResult.RewardGranted)
                    } else {
                        onResult(RewardedAdResult.ClosedWithoutReward)
                    }
                    loadRewardedAd(rewardType)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    when (rewardType) {
                        RewardType.REVIVE -> rewardedReviveAd = null
                        RewardType.UNDO -> rewardedUndoAd = null
                    }
                    onResult(RewardedAdResult.FailedToShow)
                    loadRewardedAd(rewardType)
                }
            }

            adToShow.show(activity) {
                rewardEarned = true
            }
        } else {
            onResult(RewardedAdResult.NotAvailable)
            loadRewardedAd(rewardType)
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
