package app.callguard.domain

enum class MatchType { PREFIX, EXACT, CONTAINS, INTERNATIONAL, PRIVATE }

enum class RuleAction { SILENCE, BLOCK }

data class BlockingRule(
    val id: Long,
    val name: String,
    val pattern: String,
    val matchType: MatchType,
    val action: RuleAction,
    val enabled: Boolean,
)

sealed interface Decision {
    data class Allow(val reason: String? = null) : Decision
    data class Apply(val rule: BlockingRule) : Decision
}
