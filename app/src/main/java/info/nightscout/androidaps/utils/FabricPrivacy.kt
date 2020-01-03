package info.nightscout.androidaps.utils

import android.os.Bundle
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import info.nightscout.androidaps.BuildConfig
import info.nightscout.androidaps.MainApp
import info.nightscout.androidaps.R
import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.configBuilder.ConfigBuilderPlugin
import info.nightscout.androidaps.plugins.configBuilder.ConstraintChecker
import info.nightscout.androidaps.plugins.constraints.signatureVerifier.SignatureVerifierPlugin
import info.nightscout.androidaps.utils.sharedPreferences.SP
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Some users do not wish to be tracked, Fabric Answers and Crashlytics do not provide an easy way
 * to disable them and make calls from a potentially invalid singleton reference. This wrapper
 * emulates the methods but ignores the request if the instance is null or invalid.
 */
@Singleton
class FabricPrivacy @Inject constructor(
    private val aapsLogger: AAPSLogger,
    private val sp: SP,
    private val constraintChecker: ConstraintChecker,
    private val mainApp: MainApp,
    private val signatureVerifierPlugin: SignatureVerifierPlugin,
    private val configBuilderPlugin: ConfigBuilderPlugin
) {

    init {
        instance = this
    }

    companion object {
        private lateinit var instance: FabricPrivacy
        @JvmStatic
        fun getInstance(): FabricPrivacy = instance
    }

    // Analytics logCustom
    fun logCustom(event: Bundle) {
        try {
            if (fabricEnabled()) {
                mainApp.firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, event)
            } else {
                aapsLogger.debug(LTag.CORE, "Ignoring recently opted-out event: $event")
            }
        } catch (e: NullPointerException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted-out non-initialized event: $event")
        } catch (e: IllegalStateException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted-out non-initialized event: $event")
        }
    }

    // Analytics logCustom
    fun logCustom(event: String) {
        try {
            if (fabricEnabled()) {
                mainApp.firebaseAnalytics.logEvent(event, Bundle())
            } else {
                aapsLogger.debug(LTag.CORE, "Ignoring recently opted-out event: $event")
            }
        } catch (e: NullPointerException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted-out non-initialized event: $event")
        } catch (e: IllegalStateException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted-out non-initialized event: $event")
        }
    }

    // Crashlytics logException
    fun logException(throwable: Throwable) {
        try {
            val crashlytics = Crashlytics.getInstance()
            crashlytics.core.logException(throwable)
        } catch (e: NullPointerException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $throwable")
        } catch (e: IllegalStateException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $throwable")
        }
    }

    // Crashlytics log
    fun log(msg: String) {
        try {
            val crashlytics = Crashlytics.getInstance()
            crashlytics.core.log(msg)
        } catch (e: NullPointerException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $msg")
        } catch (e: IllegalStateException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $msg")
        }
    }

    // Crashlytics log
    fun log(priority: Int, tag: String?, msg: String) {
        try {
            val crashlytics = Crashlytics.getInstance()
            crashlytics.core.log(priority, tag, msg)
        } catch (e: NullPointerException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $msg")
        } catch (e: IllegalStateException) {
            aapsLogger.debug(LTag.CORE, "Ignoring opted out non-initialized log: $msg")
        }
    }

    fun fabricEnabled(): Boolean {
        return sp.getBoolean(R.string.key_enable_fabric, true)
    }

    fun setUserStats() {
        if (!fabricEnabled()) return
        val closedLoopEnabled = if (constraintChecker.isClosedLoopAllowed().value()) "CLOSED_LOOP_ENABLED" else "CLOSED_LOOP_DISABLED"
        // Size is limited to 36 chars
        val remote = BuildConfig.REMOTE.toLowerCase(Locale.getDefault())
            .replace("https://", "")
            .replace("http://", "")
            .replace(".git", "")
            .replace(".com/", ":")
            .replace(".org/", ":")
            .replace(".net/", ":")
        mainApp.firebaseAnalytics.setUserProperty("Mode", BuildConfig.APPLICATION_ID + "-" + closedLoopEnabled)
        mainApp.firebaseAnalytics.setUserProperty("Language", sp.getString(R.string.key_language, Locale.getDefault().language))
        mainApp.firebaseAnalytics.setUserProperty("Version", BuildConfig.VERSION)
        mainApp.firebaseAnalytics.setUserProperty("HEAD", BuildConfig.HEAD)
        mainApp.firebaseAnalytics.setUserProperty("Remote", remote)
        val hashes: List<String> = signatureVerifierPlugin.shortHashes()
        if (hashes.isNotEmpty()) mainApp.firebaseAnalytics.setUserProperty("Hash", hashes[0])
        configBuilderPlugin.activePump?.let { mainApp.firebaseAnalytics.setUserProperty("Pump", it::class.java.simpleName) }
        configBuilderPlugin.activeAPS?.let { mainApp.firebaseAnalytics.setUserProperty("Aps", it::class.java.simpleName) }
        configBuilderPlugin.activeBgSource?.let { mainApp.firebaseAnalytics.setUserProperty("BgSource", it::class.java.simpleName) }
        mainApp.firebaseAnalytics.setUserProperty("Profile", configBuilderPlugin.activeProfileInterface.javaClass.simpleName)
        configBuilderPlugin.activeSensitivity?.let { mainApp.firebaseAnalytics.setUserProperty("Sensitivity", it::class.java.simpleName) }
        configBuilderPlugin.activeInsulin?.let { mainApp.firebaseAnalytics.setUserProperty("Insulin", it::class.java.simpleName) }
    }
}