package neuracircuit.dev.game2048.data

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

class AnalyticsManager(private val context: Context) {

    // Lazy initialization ensures we don't touch the SDK until we actually need it
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    private fun applyConsentToFirebase() {
        val consentMap = mapOf(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_STORAGE to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_USER_DATA to FirebaseAnalytics.ConsentStatus.GRANTED,
            FirebaseAnalytics.ConsentType.AD_PERSONALIZATION to FirebaseAnalytics.ConsentStatus.GRANTED
        )
        firebaseAnalytics.setConsent(consentMap)
    }

    fun initializeAndEnable() {
        try {
            applyConsentToFirebase()
            firebaseAnalytics.setAnalyticsCollectionEnabled(true)
            crashlytics.isCrashlyticsCollectionEnabled = true
            
            Log.d("AnalyticsManager", "Consent granted. Analytics & Crashlytics enabled.")
        } catch (e: Exception) {
            // This catches issues if Google Services is missing entirely on the device
            Log.e("AnalyticsManager", "Failed to enable Firebase", e)
        }
    }

    fun logGameStart() {
        firebaseAnalytics.logEvent("game_start", null)
    }

    fun logGameOver(score: Int, maxTile: Int) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.SCORE, score)
            putInt("max_tile", maxTile)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
    }

    fun logTileReached(value: Int) {
        val bundle = Bundle().apply {
            putInt(FirebaseAnalytics.Param.LEVEL, value) // Using 'Level' as a proxy for Tile Value
            putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "tile_$value")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle)
    }

    fun logUndoUsed() {
        firebaseAnalytics.logEvent("undo_used", null)
    }

    fun logNonFatalError(tag: String, exception: Throwable) {
        crashlytics.log("$tag: ${exception.message}")
        // should be non fatal, so we don't want to crash the app, just log the exception
        crashlytics.recordException(exception)
    }

    fun logAction(action: String, label: String? = null) {
        val bundle = Bundle().apply {
            putString("label", label)
        }
        firebaseAnalytics.logEvent(action, bundle)
    }

    fun logButtonClick(buttonName: String) {
        val bundle = Bundle().apply {
            putString("button_name", buttonName)
        }
        firebaseAnalytics.logEvent("button_click", bundle)
    }
}
