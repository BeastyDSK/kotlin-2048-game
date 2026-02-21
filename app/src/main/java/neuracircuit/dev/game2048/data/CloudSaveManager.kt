package neuracircuit.dev.game2048.data

import android.app.Activity
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange

class CloudSaveManager {

    fun isAuthenticated(activity: Activity, onResult: (Boolean) -> Unit) {
        PlayGames.getGamesSignInClient(activity)
            .isAuthenticated()
            .addOnSuccessListener { authResult ->
                onResult(authResult.isAuthenticated)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun saveGameToCloud(activity: Activity, data: CloudSaveData) {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        
        snapshotsClient.open("autosave", true, SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS)
            .addOnSuccessListener { dataOrConflict ->
                val snapshot = dataOrConflict.data
                if (snapshot != null) {
                    val bytes = data.toByteArray()
                    snapshot.snapshotContents.writeBytes(bytes)

                    val metadataChange = SnapshotMetadataChange.Builder()
                        .setDescription("Autosave: Score ${data.score}")
                        .setProgressValue(data.score.toLong())
                        .build()

                    snapshotsClient.commitAndClose(snapshot, metadataChange)
                }
            }
            .addOnFailureListener { e ->  
                e.printStackTrace()
            }
    }

    fun loadGameFromCloud(activity: Activity, onLoaded: (CloudSaveData?) -> Unit) {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)

        snapshotsClient.open("autosave", false, SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS)
            .addOnSuccessListener { dataOrConflict ->
                val snapshot = dataOrConflict.data
                if (snapshot != null) {
                    try {
                        val bytes = snapshot.snapshotContents.readFully()
                        val save = CloudSaveData.fromByteArray(bytes)
                        snapshotsClient.discardAndClose(snapshot)
                        onLoaded(save)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snapshotsClient.discardAndClose(snapshot)
                        onLoaded(null)
                    }
                } else {
                    onLoaded(null)
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                onLoaded(null)
            }
    }
}
