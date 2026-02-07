package neuracircuit.dev.game2048.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AnalyticsManager(private val context: Context) {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    // Call this ONLY after the user clicks "Accept" or if consent is already saved
    fun initializeAndEnable() {
        try {
            // 1. Get the instance (this is safe now because auto-collection is off in Manifest)
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            
            // 2. Explicitly ENABLE collection
            firebaseAnalytics?.setAnalyticsCollectionEnabled(true)
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            
            Log.d("AnalyticsManager", "Consent granted. Analytics enabled.")
        } catch (e: Exception) {
            // Log error but don't crash if Firebase fails to init (e.g. no google-services.json)
            Log.e("AnalyticsManager", "Failed to initialize Firebase", e)
        }
    }

    fun logGameStart() {
        firebaseAnalytics?.logEvent("game_start", null)
    }

    fun logGameOver(score: Int, maxTile: Int) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.SCORE, score)
            putInt("max_tile", maxTile)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
    }

    // Call this only if the tile value is significant (e.g., >= 512)
    fun logTileReached(value: Int) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.LEVEL, value) // Using 'Level' as a proxy for Tile Value
            putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "tile_$value")
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle)
    }

    fun logUndoUsed() {
        firebaseAnalytics?.logEvent("undo_used", null)
    }
}
