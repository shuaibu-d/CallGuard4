package app.callguard.license

import android.content.Context
import android.provider.Settings
import java.security.SecureRandom

object LicenseManager {
    private const val PREFS = "callguard_license"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Stable per phone + app signing key. Survives restarts, updates and reinstalls. Changes on factory reset. */
    fun deviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        if (LicenseCrypto.isValidId(androidId)) return LicenseCrypto.formatId(androidId)

        val sp = prefs(context)
        val saved = sp.getString("fallback_id", null)
        if (saved != null && LicenseCrypto.isValidId(saved)) return LicenseCrypto.formatId(saved)
        val bytes = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val generated = bytes.joinToString("") { "%02X".format(it) }
        sp.edit().putString("fallback_id", generated).apply()
        return LicenseCrypto.formatId(generated)
    }

    fun isActivated(context: Context): Boolean {
        val code = prefs(context).getString("code", null) ?: return false
        return LicenseCrypto.verify(LicenseConfig.PUBLIC_KEY_B64, deviceId(context), code)
    }

    fun activate(context: Context, code: String): Boolean {
        val ok = LicenseCrypto.verify(LicenseConfig.PUBLIC_KEY_B64, deviceId(context), code)
        if (ok) prefs(context).edit().putString("code", code.filter { !it.isWhitespace() }).apply()
        return ok
    }
}
