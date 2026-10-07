package app.callguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.callguard.data.AllowedEntity
import app.callguard.data.AppDatabase
import app.callguard.data.HistoryEntity
import app.callguard.data.Prefs
import app.callguard.data.RuleEntity
import app.callguard.domain.BlockingEngine
import app.callguard.domain.Decision
import app.callguard.domain.MatchType
import app.callguard.domain.RuleAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val prefs = Prefs(app)
    private val engine = BlockingEngine()

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)

    val rules: StateFlow<List<RuleEntity>> = db.rules().observeAll().state(emptyList())
    val history: StateFlow<List<HistoryEntity>> = db.history().observeRecent().state(emptyList())
    val allowed: StateFlow<List<AllowedEntity>> = db.allowed().observeAll().state(emptyList())
    val weekCount: StateFlow<Int> =
        db.history().observeCountSince(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000).state(0)

    private val _protection = MutableStateFlow(prefs.protectionEnabled)
    val protection: StateFlow<Boolean> = _protection

    private val _neverBlockContacts = MutableStateFlow(prefs.neverBlockContacts)
    val neverBlockContacts: StateFlow<Boolean> = _neverBlockContacts

    private val _defaultAction = MutableStateFlow(prefs.defaultAction)
    val defaultAction: StateFlow<RuleAction> = _defaultAction

    fun setProtection(value: Boolean) {
        prefs.protectionEnabled = value
        _protection.value = value
    }

    fun setNeverBlockContacts(value: Boolean) {
        prefs.neverBlockContacts = value
        _neverBlockContacts.value = value
    }

    fun setDefaultAction(value: RuleAction) {
        prefs.defaultAction = value
        _defaultAction.value = value
    }

    fun test(number: String, rules: List<RuleEntity>): Decision =
        engine.evaluate(number, false, rules.map { it.toDomain() }, allowed.value.map { it.number }.toSet())

    fun saveRule(existing: RuleEntity?, name: String, pattern: String, type: MatchType, action: RuleAction) {
        viewModelScope.launch {
            val finalName = name.ifBlank { pattern.ifBlank { defaultName(type) } }
            if (existing == null) {
                db.rules().insert(RuleEntity(name = finalName, pattern = pattern, matchType = type, action = action))
            } else {
                db.rules().update(existing.copy(name = finalName, pattern = pattern, matchType = type, action = action))
            }
        }
    }

    fun addPreset(name: String, type: MatchType, pattern: String) {
        viewModelScope.launch {
            if (rules.value.none { it.name == name }) {
                db.rules().insert(
                    RuleEntity(name = name, pattern = pattern, matchType = type, action = _defaultAction.value),
                )
            }
        }
    }

    fun setRuleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { db.rules().setEnabled(id, enabled) }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { db.rules().deleteById(id) }
    }

    fun allowFromHistory(item: HistoryEntity) {
        if (item.number.isEmpty()) return
        viewModelScope.launch {
            db.allowed().insert(AllowedEntity(item.number, System.currentTimeMillis()))
            db.history().deleteById(item.id)
        }
    }

    fun removeAllowed(number: String) {
        viewModelScope.launch { db.allowed().delete(number) }
    }

    fun clearHistory() {
        viewModelScope.launch { db.history().clear() }
    }

    private fun defaultName(type: MatchType) = when (type) {
        MatchType.INTERNATIONAL -> "Outside Nigeria"
        MatchType.PRIVATE -> "Private numbers"
        else -> "New rule"
    }
}
