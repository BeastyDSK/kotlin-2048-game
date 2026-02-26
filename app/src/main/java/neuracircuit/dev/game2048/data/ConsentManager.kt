package neuracircuit.dev.game2048.data

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /**
     * Helper interface to listen for the result of the consent flow.
     */
    interface OnConsentGatheringCompleteListener {
        fun onConsentGatheringComplete(error: com.google.android.ump.FormError?)
    }

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun gatherConsent(onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener) {
        // consentInformation.reset()

        // For testing purposes, you can force a debug geography.
        // val debugSettings = com.google.android.ump.ConsentDebugSettings.Builder(activity)
        //     .setDebugGeography(com.google.android.ump.ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //     .addTestDeviceHashedId("B3EEABB8EE11C2BE770B684D95219ECB")
        //     .build()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            // .setConsentDebugSettings(debugSettings)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent gathering process is complete.
                    onConsentGatheringCompleteListener.onConsentGatheringComplete(formError)
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                onConsentGatheringCompleteListener.onConsentGatheringComplete(requestConsentError)
            }
        )
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            onDismiss()
        }
    }
}
