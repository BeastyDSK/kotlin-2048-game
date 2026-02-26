package neuracircuit.dev.game2048.data

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateCoordinator(
    private val analytics: AnalyticsManager? = null
) {
    enum class PreferredMode {
        FLEXIBLE,
        IMMEDIATE,
        AUTO
    }

    private var isListenerRegistered = false
    private var onFlexibleUpdateDownloaded: (() -> Unit)? = null
    private var appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager? = null

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            onFlexibleUpdateDownloaded?.invoke()
        }
    }

    fun setOnFlexibleUpdateDownloaded(listener: () -> Unit) {
        onFlexibleUpdateDownloaded = listener
    }

    fun register(activity: androidx.activity.ComponentActivity) {
        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity)
        }
        if (!isListenerRegistered) {
            appUpdateManager?.registerListener(installStateListener)
            isListenerRegistered = true
        }
    }

    fun unregister() {
        if (isListenerRegistered) {
            appUpdateManager?.unregisterListener(installStateListener)
            isListenerRegistered = false
        }
    }

    fun checkForUpdates(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        preferredMode: PreferredMode = PreferredMode.AUTO
    ) {
        val manager = appUpdateManager ?: return

        manager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val updateType = resolveUpdateType(appUpdateInfo, preferredMode)
                if (updateType != null) {
                    startUpdateFlow(manager, appUpdateInfo, updateType, launcher)
                } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    onFlexibleUpdateDownloaded?.invoke()
                }
            }
            .addOnFailureListener { e ->
                analytics?.logNonFatalError("InAppUpdateCoordinator.checkForUpdates", e)
            }
    }

    fun onResume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val manager = appUpdateManager ?: return

        manager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    onFlexibleUpdateDownloaded?.invoke()
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    startUpdateFlow(manager, appUpdateInfo, AppUpdateType.IMMEDIATE, launcher)
                }
            }
            .addOnFailureListener { e ->
                analytics?.logNonFatalError("InAppUpdateCoordinator.onResume", e)
            }
    }

    fun completeFlexibleUpdate() {
        appUpdateManager?.completeUpdate()
    }

    private fun resolveUpdateType(appUpdateInfo: AppUpdateInfo, preferredMode: PreferredMode): Int? {
        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return null

        val immediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val flexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

        return when (preferredMode) {
            PreferredMode.IMMEDIATE -> when {
                immediateAllowed -> AppUpdateType.IMMEDIATE
                flexibleAllowed -> AppUpdateType.FLEXIBLE
                else -> null
            }
            PreferredMode.FLEXIBLE -> when {
                flexibleAllowed -> AppUpdateType.FLEXIBLE
                immediateAllowed -> AppUpdateType.IMMEDIATE
                else -> null
            }
            PreferredMode.AUTO -> {
                val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
                val shouldForceImmediate = stalenessDays >= 7
                when {
                    shouldForceImmediate && immediateAllowed -> AppUpdateType.IMMEDIATE
                    flexibleAllowed -> AppUpdateType.FLEXIBLE
                    immediateAllowed -> AppUpdateType.IMMEDIATE
                    else -> null
                }
            }
        }
    }

    private fun startUpdateFlow(
        manager: com.google.android.play.core.appupdate.AppUpdateManager,
        appUpdateInfo: AppUpdateInfo,
        appUpdateType: Int,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        try {
            val options = AppUpdateOptions.newBuilder(appUpdateType).build()
            manager.startUpdateFlowForResult(appUpdateInfo, launcher, options)
        } catch (e: Exception) {
            analytics?.logNonFatalError("InAppUpdateCoordinator.startUpdateFlow", e)
        }
    }
}
