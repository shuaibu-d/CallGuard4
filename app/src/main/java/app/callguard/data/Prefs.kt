package app.callguard.data

import android.content.Context
import app.callguard.domain.RuleAction

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("callguard", Context.MODE_PRIVATE)

    var protectionEnabled: Boolean
        get() = sp.getBoolean("protection", true)
        set(value) = sp.edit().putBoolean("protection", value).apply()

    var neverBlockContacts: Boolean
        get() = sp.getBoolean("never_block_contacts", false)
        set(value) = sp.edit().putBoolean("never_block_contacts", value).apply()

    var defaultAction: RuleAction
        get() = runCatching { RuleAction.valueOf(sp.getString("default_action", "SILENCE")!!) }
            .getOrDefault(RuleAction.SILENCE)
        set(value) = sp.edit().putString("default_action", value.name).apply()
}
