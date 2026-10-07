package app.callguard.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telecom.TelecomManager
import app.callguard.data.AppDatabase
import app.callguard.data.HistoryEntity
import app.callguard.data.Prefs
import app.callguard.domain.BlockingEngine
import app.callguard.domain.Decision
import app.callguard.domain.PhoneNumberNormalizer
import app.callguard.domain.RuleAction
import app.callguard.license.LicenseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class CallGuardScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            allow(callDetails)
            return
        }
        if (!LicenseManager.isActivated(this)) {
            allow(callDetails)
            return
        }
        val prefs = Prefs(this)
        if (!prefs.protectionEnabled) {
            allow(callDetails)
            return
        }

        val raw = callDetails.handle?.schemeSpecificPart
        val isPrivate = raw.isNullOrBlank() ||
            callDetails.handlePresentation != TelecomManager.PRESENTATION_ALLOWED

        val db = AppDatabase.get(this)
        val decision = try {
            runBlocking(Dispatchers.IO) {
                val rules = db.rules().getAll().map { it.toDomain() }
                val allowed = db.allowed().getAll().map { it.number }.toSet()
                BlockingEngine().evaluate(raw, isPrivate, rules, allowed)
            }
        } catch (e: Exception) {
            Decision.Allow("error")
        }

        if (decision !is Decision.Apply) {
            allow(callDetails)
            return
        }

        if (prefs.neverBlockContacts && !isPrivate && raw != null && ContactLookup.isContact(this, raw)) {
            allow(callDetails)
            return
        }

        val rule = decision.rule
        val response = CallResponse.Builder().apply {
            if (rule.action == RuleAction.BLOCK) {
                setDisallowCall(true)
                setRejectCall(true)
                setSkipNotification(true)
            } else {
                setSilenceCall(true)
            }
        }.build()
        respondToCall(callDetails, response)

        val number = if (isPrivate) "" else PhoneNumberNormalizer.normalize(raw.orEmpty())
        val now = System.currentTimeMillis()
        scope.launch {
            db.history().insert(
                HistoryEntity(number = number, ruleName = rule.name, action = rule.action, timestamp = now),
            )
            db.history().pruneBefore(now - THIRTY_DAYS)
        }
    }

    private fun allow(details: Call.Details) {
        respondToCall(details, CallResponse.Builder().build())
    }

    private companion object {
        const val THIRTY_DAYS = 30L * 24 * 60 * 60 * 1000
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
