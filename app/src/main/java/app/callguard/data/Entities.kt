package app.callguard.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.callguard.domain.BlockingRule
import app.callguard.domain.MatchType
import app.callguard.domain.RuleAction

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val pattern: String,
    val matchType: MatchType,
    val action: RuleAction,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
) {
    fun toDomain() = BlockingRule(id, name, pattern, matchType, action, enabled)
}

/** `number` is the normalized number. It never leaves the device and the UI shows it masked. */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val ruleName: String,
    val action: RuleAction,
    val timestamp: Long,
)

@Entity(tableName = "allowed")
data class AllowedEntity(
    @PrimaryKey val number: String,
    val addedAt: Long,
)
