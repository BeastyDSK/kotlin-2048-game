package neuracircuit.dev.game2048.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import neuracircuit.dev.game2048.R
import com.google.android.gms.games.PlayGames

class PlayGamesManager(private val context: Context) {

    fun submitScore(activity: Activity, score: Int) {
        PlayGames.getLeaderboardsClient(activity).submitScore(
            context.getString(R.string.leaderboard_high_scores),
            score.toLong()
        )
    }

    fun showLeaderboard(activity: Activity, onSuccess: (Intent) -> Unit) {
        PlayGames.getLeaderboardsClient(activity)
            .getLeaderboardIntent(context.getString(R.string.leaderboard_high_scores))
            .addOnSuccessListener { intent -> onSuccess(intent) }
    }

    fun unlockAchievement(activity: Activity, achievementIdRes: Int) {
        PlayGames.getAchievementsClient(activity)
            .unlock(context.getString(achievementIdRes))
    }

    fun showAchievements(activity: Activity, onSuccess: (Intent) -> Unit) {
        PlayGames.getAchievementsClient(activity)
            .getAchievementsIntent()
            .addOnSuccessListener { intent -> onSuccess(intent) }
    }
}
