package neuracircuit.dev.game2048.helpers

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

object AppReviewManager {
    private const val TAG = "AppReviewManager"

    fun onGameFinished(activity: Activity) {
        requestReview(activity)
    }

    private fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener {
                    Log.d(TAG, "Review flow finished")
                }
            } else {
                Log.e(TAG, "Review request failed: ${task.exception?.message}")
            }
        }
    }
}
